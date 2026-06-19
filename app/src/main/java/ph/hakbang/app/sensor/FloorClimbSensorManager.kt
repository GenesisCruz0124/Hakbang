package ph.hakbang.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Detects flights of stairs climbed using the barometer (Sensor.TYPE_PRESSURE). Pressure is
 * converted to a relative altitude, smoothed with an exponential moving average to filter out
 * sensor noise, and a sustained rise past [FLOOR_HEIGHT_METERS] is counted as one floor climbed.
 * Not precise absolute altitude (weather changes pressure too), but altitude *changes* over the
 * short window between climbing a stairwell are a reliable signal.
 */
class FloorClimbSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private var smoothedAltitude: Float? = null
    private var baselineAltitude: Float = 0f

    private val _floorClimbed = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val floorClimbed: SharedFlow<Unit> = _floorClimbed

    val isSensorAvailable: Boolean get() = pressureSensor != null

    fun start() {
        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) return

        val pressureHpa = event.values[0]
        val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureHpa)

        val previousSmoothed = smoothedAltitude
        val smoothed = if (previousSmoothed == null) {
            baselineAltitude = altitude
            altitude
        } else {
            previousSmoothed + SMOOTHING_ALPHA * (altitude - previousSmoothed)
        }
        smoothedAltitude = smoothed

        if (smoothed - baselineAltitude >= FLOOR_HEIGHT_METERS) {
            baselineAltitude = smoothed
            _floorClimbed.tryEmit(Unit)
        } else if (smoothed < baselineAltitude - NOISE_MARGIN_METERS) {
            // Only treat this as a real descent (and reset the baseline) once the drop clears
            // the noise margin; barometer jitter of a few cm otherwise kept re-chasing the
            // baseline downward and prevented a real climb from ever accumulating 3m of net ascent.
            baselineAltitude = smoothed
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        /** Average height of one flight of stairs, used as the ascent threshold for counting a floor climbed. */
        private const val FLOOR_HEIGHT_METERS = 3.0
        private const val SMOOTHING_ALPHA = 0.2f
        /** Minimum drop below the current baseline before it's treated as a real descent rather than sensor noise. */
        private const val NOISE_MARGIN_METERS = 0.5
    }
}
