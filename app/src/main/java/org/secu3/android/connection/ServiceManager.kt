package org.secu3.android.connection

import android.content.Context
import android.content.Intent
import android.app.Service
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Sends Android service commands; connection policy stays in Secu3ConnectionManager. */
@Singleton
class ServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun startForegroundService(serviceClass: Class<out Service>) {
        val intent = Intent(context, serviceClass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(context, intent)
        else context.startService(intent)
    }

    fun stopService(serviceClass: Class<out Service>) {
        context.stopService(Intent(context, serviceClass))
    }
}
