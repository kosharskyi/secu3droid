package org.secu3.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.secu3.android.ui.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class SecuConnectionService : LifecycleService() {

    @Inject
    internal lateinit var secu3Repository: Secu3Repository

    private var lostConnectionJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        secu3Repository.connectionStatusLiveData.observe(this) {
            if (it) {
                lostConnectionJob?.cancel()
                lostConnectionJob = null
            } else {
                lostConnectionJob?.let {
                    return@observe
                }
                lostConnectionJob = lifecycleScope.launch(Dispatchers.IO) {
                    delay(20000)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var foregroundServiceType = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        }

        ServiceCompat.startForeground(this, 6543, getNotification() , foregroundServiceType)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun getNotification(): Notification {

        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_working_in_background))
            .setPriority(PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Background service"

            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                 lockscreenVisibility = VISIBILITY_PRIVATE
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        secu3Repository.disable()
        ServiceCompat.stopForeground(this@SecuConnectionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "Background connection chanel"
    }
}