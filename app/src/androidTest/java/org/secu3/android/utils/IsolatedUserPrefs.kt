/*
 *    SecuDroid  - An open source, free manager for SECU-3 engine control unit
 *    Copyright (C) 2026 Vitalii O. Kosharskyi. Ukraine, Kyiv
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 */
package org.secu3.android.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin

/** One isolated store per fixture; close it before opening the same file again. */
internal class IsolatedUserPrefs(context: Context, name: String) {
    private val storeJob = SupervisorJob()
    private val storeScope = CoroutineScope(storeJob + Dispatchers.IO)
    private val storeFile = File(context.cacheDir, "test-$name.preferences_pb").apply {
        parentFile?.mkdirs()
    }
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = storeScope,
        produceFile = { storeFile },
    )
    val prefs = UserPrefs(context, dataStore)

    suspend fun close() {
        storeJob.cancelAndJoin()
    }
}
