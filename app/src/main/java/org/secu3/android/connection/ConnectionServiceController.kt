package org.secu3.android.connection

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Owns Android start/stop commands for the foreground connection service. */
@Singleton
class ConnectionServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private data class Owner(val token: Long, val connectionGeneration: Long)

    private var nextToken = 0L
    private var owner: Owner? = null

    @Synchronized
    fun ensureStarted(connectionGeneration: Long): Long {
        val existing = owner?.takeIf { it.connectionGeneration == connectionGeneration }
        val current = existing ?: Owner(++nextToken, connectionGeneration).also { owner = it }
        if (existing != null) return current.token
        val intent = serviceIntent().putExtra(SecuConnectionService.EXTRA_OWNER_TOKEN, current.token)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(context, intent)
            else context.startService(intent)
        } catch (error: Exception) {
            if (owner?.token == current.token) owner = null
            throw error
        }
        return current.token
    }

    @Synchronized
    fun stop() {
        if (owner == null) return
        owner = null
        context.stopService(serviceIntent())
    }

    /** Returns the session generation only when this is still the service instance we own. */
    @Synchronized
    fun completeDestruction(token: Long): Long? {
        val current = owner?.takeIf { it.token == token } ?: return null
        owner = null
        return current.connectionGeneration
    }

    @Synchronized
    internal fun ownerToken(): Long? = owner?.token

    @Synchronized
    internal fun ownerConnectionGeneration(): Long? = owner?.connectionGeneration

    private fun serviceIntent() = Intent(context, SecuConnectionService::class.java)
}
