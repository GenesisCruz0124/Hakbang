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
 * session's final value into the offset before continuing. allTimeTotal = bootOffset + rawValue
 * is therefore monotonically increasing across the life of the install.
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
    private var initialized = false

    private val _allTimeTotal = MutableStateFlow<Long?>(null)
    val allTimeTotal: StateFlow<Long?> = _allTimeTotal

    val isSensorAvailable: Boolean get() = stepCounterSensor != null

    suspend fun loadPersistedState() {
        lastRawValue = preferences.lastRawSensorValue.first()
        bootOffset = preferences.bootOffset.first()
        initialized = true
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

        if (!initialized) {
            // First reading before persisted state finished loading; treat as a fresh boot session.
            lastRawValue = rawValue
            initialized = true
        } else if (rawValue < lastRawValue) {
            // Device rebooted since the last reading: fold the previous session into the offset.
            bootOffset += lastRawValue
            lastRawValue = rawValue
        } else {
            lastRawValue = rawValue
        }

        val total = bootOffset + lastRawValue
        _allTimeTotal.value = total

        scope.launch {
            preferences.saveSensorState(lastRawValue, bootOffset)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
