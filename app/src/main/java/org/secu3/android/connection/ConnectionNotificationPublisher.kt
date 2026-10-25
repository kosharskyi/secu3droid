package org.secu3.android.connection

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.secu3.android.R
import org.secu3.android.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the service notification and refreshes it in place after notification access changes. */
@Singleton
class ConnectionNotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun foregroundNotification(): Notification {
        ensureChannel()
        val intent = PendingIntent.getActivity(
            context,
            0,
            android.content.Intent(context, MainActivity::class.java),
            FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.service_working_in_background))
            .setPriority(PRIORITY_LOW)
            .setContentIntent(intent)
            .build()
    }

    fun refreshPostedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, foregroundNotification())
            }
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.connection_notification_channel), NotificationManager.IMPORTANCE_LOW).apply {
            lockscreenVisibility = VISIBILITY_PRIVATE
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "Background connection chanel"
        const val NOTIFICATION_ID = 6543
    }
}
