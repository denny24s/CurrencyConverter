// DarkModePrefs.kt
package com.example.currencyconverter

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/* 1. extension DataStore on Context */
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/* 2. object with helper functions */
object DarkModePrefs {

    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")

    /** observe changes */
    fun flow(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_DARK_MODE] ?: false }

    /** save new value */
    suspend fun save(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_DARK_MODE] = enabled }
    }
}
