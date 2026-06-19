package ph.hakbang.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ph.hakbang.app.data.preferences.UserPreferences
import kotlin.math.sqrt

/**
 * Wraps Sensor.TYPE_STEP_COUNTER and converts its reboot-resetting cumulative value into an
 * ever-increasing all-time step total.
 *
 * TYPE_STEP_COUNTER reports steps since the last device reboot. To get a value that survives
 * reboots we track [bootOffset]: the sum of all step counts from previous boot sessions. When a
 * new raw reading is smaller than the last one we saw, a reboot occurred, so we fold the previous
 * session's final value into the offset before continuing.
 *
 * Each accepted increment must also pass two checks meant to reject motion the hardware step
 * sensor can be fooled by but that isn't actual walking:
 *  - [isPlausibleCadence]: rejects a *sustained* unusually fast rate (e.g. vehicle vibration).
 *  - [isGaitValidated]: requires the accelerometer to show an actual periodic gait rhythm
 *    recently, which is what catches idly shaking the phone while standing still - shaking
 *    can match a plausible step *rate* but rarely has the steady, evenly-spaced rhythm of a
 *    real footstep cadence.
 * [filteredTotal] is the resulting total that only ever grows from increments that pass both.
 */
class StepSensorManager(
    context: Context,
    private val preferences: UserPreferences,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val linearAccelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var lastRawValue: Long = -1L
    private var bootOffset: Long = 0L
    private var filteredTotal: Long = 0L

    /** Trailing (timestamp, rawValue) samples used to spot a sustained implausible cadence. */
    private val recentSamples = ArrayDeque<Pair<Long, Long>>()

    /** Trailing motion-peak timestamps used to confirm an actual periodic gait rhythm. */
    private val recentPeakTimestamps = ArrayDeque<Long>()
    private var aboveThreshold = false
    private var lastPeakTimestampNanos: Long = 0L
    private var lastGaitValidNanos: Long = Long.MIN_VALUE

    private val _allTimeTotal = MutableStateFlow<Long?>(null)
    val allTimeTotal: StateFlow<Long?> = _allTimeTotal

    val isSensorAvailable: Boolean get() = stepCounterSensor != null

    suspend fun loadPersistedState() {
        lastRawValue = preferences.lastRawSensorValue.first()
        bootOffset = preferences.bootOffset.first()
        filteredTotal = preferences.filteredStepTotal.first()
    }

    fun start() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        linearAccelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> onStepCounterChanged(event)
            Sensor.TYPE_LINEAR_ACCELERATION -> onLinearAccelerationChanged(event)
        }
    }

    private fun onStepCounterChanged(event: SensorEvent) {
        val rawValue = event.values[0].toLong()
        val nowNanos = event.timestamp

        when {
            lastRawValue < 0L -> {
                // First reading ever for this install: sync the baseline without filtering.
                lastRawValue = rawValue
                filteredTotal = bootOffset + rawValue
            }
            rawValue < lastRawValue -> {
                // Device rebooted since the last reading: fold the previous session into the offset.
                bootOffset += lastRawValue
                filteredTotal += rawValue
                lastRawValue = rawValue
            }
            else -> {
                val rawDelta = rawValue - lastRawValue
                if (isPlausibleCadence(nowNanos, rawValue) && isGaitValidated(nowNanos)) {
                    filteredTotal += rawDelta
                }
                // Otherwise this delta looks like vehicle vibration or in-hand shaking rather
                // than actual walking, so it's dropped instead of counted as steps.
                lastRawValue = rawValue
            }
        }

        recordSample(nowNanos, rawValue)
        _allTimeTotal.value = filteredTotal

        scope.launch {
            preferences.saveSensorState(lastRawValue, bootOffset, filteredTotal)
        }
    }

    private fun recordSample(nowNanos: Long, rawValue: Long) {
        recentSamples.addLast(nowNanos to rawValue)
        while (recentSamples.isNotEmpty() && nowNanos - recentSamples.first().first > CADENCE_WINDOW_NANOS) {
            recentSamples.removeFirst()
        }
    }

    /**
     * True unless the trailing window's average step rate exceeds a generous human walking/
     * running cadence. Short bursts are always accepted (not enough history to judge yet); only
     * a *sustained* high rate over [MIN_WINDOW_SECONDS] is treated as vehicle vibration.
     */
    private fun isPlausibleCadence(nowNanos: Long, rawValue: Long): Boolean {
        val oldest = recentSamples.firstOrNull() ?: return true
        val windowSeconds = (nowNanos - oldest.first) / 1_000_000_000.0
        if (windowSeconds < MIN_WINDOW_SECONDS) return true
        val windowStepsDelta = rawValue - oldest.second
        return (windowStepsDelta / windowSeconds) <= MAX_STEPS_PER_SECOND
    }

    /**
     * True if the accelerometer has recently shown a regular, gait-like rhythm. Devices without a
     * linear-acceleration sensor always pass - we can't validate, so we don't block.
     */
    private fun isGaitValidated(nowNanos: Long): Boolean {
        if (linearAccelSensor == null) return true
        return nowNanos - lastGaitValidNanos <= GAIT_VALID_DURATION_NANOS
    }

    private fun onLinearAccelerationChanged(event: SensorEvent) {
        val magnitude = sqrt(
            event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]
        )
        val nowNanos = event.timestamp

        if (magnitude >= PEAK_THRESHOLD_MPS2) {
            if (!aboveThreshold && nowNanos - lastPeakTimestampNanos >= PEAK_REFRACTORY_NANOS) {
                aboveThreshold = true
                lastPeakTimestampNanos = nowNanos
                recentPeakTimestamps.addLast(nowNanos)
                while (recentPeakTimestamps.isNotEmpty() &&
                    nowNanos - recentPeakTimestamps.first() > GAIT_HISTORY_WINDOW_NANOS
                ) {
                    recentPeakTimestamps.removeFirst()
                }
                if (hasRegularGaitRhythm()) {
                    lastGaitValidNanos = nowNanos
                }
            }
        } else {
            aboveThreshold = false
        }
    }

    /** A handful of consecutive motion peaks, evenly spaced at a walking-like interval, is a gait; an irregular burst (a shake) is not. */
    private fun hasRegularGaitRhythm(): Boolean {
        if (recentPeakTimestamps.size < MIN_PEAKS_FOR_VALIDATION) return false
        val intervals = recentPeakTimestamps.zipWithNext { a, b -> (b - a) / 1_000_000_000.0 }
        if (intervals.any { it < MIN_STEP_INTERVAL_SECONDS || it > MAX_STEP_INTERVAL_SECONDS }) return false
        val mean = intervals.average()
        val variance = intervals.sumOf { (it - mean) * (it - mean) } / intervals.size
        val stddev = sqrt(variance)
        return mean > 0 && (stddev / mean) <= MAX_INTERVAL_COEFFICIENT_OF_VARIATION
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        /** Cap on plausible walking/running cadence; a sustained rate above this is treated as vehicle vibration, not steps. */
        private const val MAX_STEPS_PER_SECOND = 3.5
        private const val MIN_WINDOW_SECONDS = 3.0
        private const val CADENCE_WINDOW_NANOS = 10_000_000_000L

        /** Linear-acceleration magnitude (m/s^2) a footstep impact must clear to count as a motion peak. */
        private const val PEAK_THRESHOLD_MPS2 = 1.8
        private const val PEAK_REFRACTORY_NANOS = 250_000_000L
        private const val GAIT_HISTORY_WINDOW_NANOS = 5_000_000_000L
        private const val MIN_PEAKS_FOR_VALIDATION = 3
        private const val MIN_STEP_INTERVAL_SECONDS = 0.25
        private const val MAX_STEP_INTERVAL_SECONDS = 1.0
        private const val MAX_INTERVAL_COEFFICIENT_OF_VARIATION = 0.35
        /** How long a validated gait rhythm keeps step-counter increments unblocked after the last qualifying peak. */
        private const val GAIT_VALID_DURATION_NANOS = 2_000_000_000L
    }
}
