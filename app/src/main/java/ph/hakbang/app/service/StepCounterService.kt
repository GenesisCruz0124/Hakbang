package ph.hakbang.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ph.hakbang.app.HakbangApp
import ph.hakbang.app.MainActivity
import ph.hakbang.app.R
import ph.hakbang.app.data.preferences.AppLanguage
import ph.hakbang.app.sensor.StepSensorManager
import ph.hakbang.app.util.AppStrings
import ph.hakbang.app.util.StringKey
import java.time.LocalDate

class StepCounterService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var stepSensorManager: StepSensorManager
    private lateinit var app: HakbangApp

    private var currentLanguage: AppLanguage = AppLanguage.ENGLISH

    override fun onCreate() {
        super.onCreate()
        app = application as HakbangApp
        stepSensorManager = StepSensorManager(this, app.userPreferences, serviceScope)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(0))

        serviceScope.launch {
            app.userPreferences.userProfile.collect { profile ->
                currentLanguage = profile.language
            }
        }

        serviceScope.launch {
            stepSensorManager.loadPersistedState()
            stepSensorManager.start()
        }

        serviceScope.launch {
            stepSensorManager.allTimeTotal.collect { total ->
                if (total != null) {
                    app.stepRepository.applySensorAllTimeTotal(total, LocalDate.now())
                    val steps = app.stepRepository.observeToday(LocalDate.now()).first()?.steps ?: 0
                    updateNotification(steps)
                }
            }
        }

        return START_STICKY
    }

    private fun updateNotification(steps: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(steps))
    }

    private fun buildNotification(steps: Int): Notification {
        val strings = AppStrings(currentLanguage)
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(strings[StringKey.NOTIFICATION_TITLE])
            .setContentText(strings.format(StringKey.NOTIFICATION_TEXT, steps))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stepSensorManager.stop()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "step_counting_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
