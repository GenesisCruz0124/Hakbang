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

/**
 * Wraps Sensor.TYPE_STEP_COUNTER and converts its reboot-resetting cumulative value into an
 * ever-increasing all-time step total.
 *
 * TYPE_STEP_COUNTER reports steps since the last device reboot. To get a value that survives
 * reboots we track [bootOffset]: the sum of all step counts from previous boot sessions. When a
 * new raw reading is smaller than the last one we saw, a reboot occurred, so we fold the previous
 * session's final value into the offset before continuing.
 *
 * Each accepted increment is also passed through a cadence check (see [isPlausibleCadence]) so
 * that sustained vibration from riding a motorcycle, car, or other vehicle - which can fool the
 * hardware step sensor - isn't counted as walking. [filteredTotal] is the resulting total that
 * only ever grows from increments that look like plausible human footsteps.
 */
class StepSensorManager(
    context: Context,
    private val preferences: UserPreferences,
    private val scope: CoroutineScope
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var lastRawValue: Long = -1L
    private var bootOffset: Long = 0L
    private var filteredTotal: Long = 0L

    /** Trailing (timestamp, rawValue) samples used to spot a sustained implausible cadence. */
    private val recentSamples = ArrayDeque<Pair<Long, Long>>()

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
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
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
                if (isPlausibleCadence(nowNanos, rawValue)) {
                    filteredTotal += rawDelta
                }
                // Otherwise this delta looks like sustained vehicle vibration (motorcycle, car,
                // jeepney) rather than actual walking, so it's dropped instead of counted as steps.
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

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        /** Cap on plausible walking/running cadence; a sustained rate above this is treated as vehicle vibration, not steps. */
        private const val MAX_STEPS_PER_SECOND = 3.5
        private const val MIN_WINDOW_SECONDS = 3.0
        private const val CADENCE_WINDOW_NANOS = 10_000_000_000L
    }
}
