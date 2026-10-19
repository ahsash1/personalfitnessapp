package com.example.personalfitnessapp.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personalfitnessapp.data.repository.RestTimerRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Runs only while a rest timer is counting down (started when a set completes,
 * stopped when the timer ends or is skipped) - not a persistent workout
 * notification, which is out of scope for this app. Shows a live
 * countdown and fires vibration + a completion notification at zero.
 *
 * Uses the Android 14+ "shortService" foreground type (a ~3 minute execution
 * budget meant for exactly this kind of brief task). If a rest is extended
 * past 3 minutes total, the OS may time the service out - the in-app countdown
 * stays correct regardless since it's timestamp-based (see [RestTimerState]),
 * but the background notification/vibration could be missed in that edge case.
 */
@AndroidEntryPoint
class RestTimerService : Service() {
    @Inject lateinit var restTimerRepository: RestTimerRepository
    @Inject lateinit var clock: Clock

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        RestTimerNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            RestTimerNotifications.NOTIFICATION_ID,
            RestTimerNotifications.countdownNotification(this, "", 0),
        )
        runLoop()
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    /** Polls the persisted timer state once a second and reflects it in the notification. */
    private fun runLoop() {
        serviceScope.launch {
            while (true) {
                val current = restTimerRepository.getCurrent()
                if (current == null) {
                    stopSelf()
                    return@launch
                }
                if (current.state.isFinished(clock.millis())) {
                    onRestComplete(current.exerciseName)
                    return@launch
                }
                val remainingSeconds = (current.state.remainingMillis(clock.millis()) / 1000).toInt()
                updateNotification(current.exerciseName, remainingSeconds)
                delay(1000)
            }
        }
    }

    private fun updateNotification(exerciseName: String, remainingSeconds: Int) {
        val notification = RestTimerNotifications.countdownNotification(this, exerciseName, remainingSeconds)
        NotificationManagerCompat.from(this).notify(RestTimerNotifications.NOTIFICATION_ID, notification)
    }

    private fun onRestComplete(exerciseName: String) {
        vibrate()
        val notification = RestTimerNotifications.restCompleteNotification(this, exerciseName)
        NotificationManagerCompat.from(this).notify(RestTimerNotifications.NOTIFICATION_ID, notification)
        stopSelf()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, RestTimerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
