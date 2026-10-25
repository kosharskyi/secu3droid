/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2025 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    SECU-3  - An open source, free engine control unit
 *    Copyright (C) 2007-2025 Alexey A. Shabelnikov. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    contacts:
 *                    http://secu-3.org
 *                    email: vetalkosharskiy@gmail.com
 */

package org.secu3.android.connection

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SecuConnectionService : LifecycleService() {

    @Inject
    internal lateinit var secu3ConnectionManager: Secu3ConnectionManager

    @Inject
    internal lateinit var notificationPublisher: ConnectionNotificationPublisher

    private var ownerToken = INVALID_OWNER_TOKEN

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ownerToken = intent?.getLongExtra(EXTRA_OWNER_TOKEN, INVALID_OWNER_TOKEN) ?: INVALID_OWNER_TOKEN

        var foregroundServiceType = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        }

        ServiceCompat.startForeground(this, ConnectionNotificationPublisher.NOTIFICATION_ID, notificationPublisher.foregroundNotification(), foregroundServiceType)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        if (ownerToken != INVALID_OWNER_TOKEN) secu3ConnectionManager.onConnectionServiceDestroyed(ownerToken)
        ServiceCompat.stopForeground(this@SecuConnectionService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        internal const val EXTRA_OWNER_TOKEN = "connection_service_owner_token"
        private const val INVALID_OWNER_TOKEN = -1L
    }
}
