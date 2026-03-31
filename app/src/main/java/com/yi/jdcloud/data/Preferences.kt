package com.yi.jdcloud.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yi.jdcloud.domain.LoginState
import com.yi.jdcloud.domain.QuotaInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jdcloud_prefs")

@Singleton
class Preferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.dataStore

    companion object {
        private val KEY_PIN = stringPreferencesKey("cookie_pin")
        private val KEY_THOR = stringPreferencesKey("cookie_thor")
        private val KEY_QID_UID = stringPreferencesKey("cookie_qid_uid")
        private val KEY_QID_SID = stringPreferencesKey("cookie_qid_sid")
        private val KEY_JDV = stringPreferencesKey("cookie_jdv")
        private val KEY_QUOTA_JSON = stringPreferencesKey("quota_json")
        private val KEY_REFRESH_INTERVAL = intPreferencesKey("refresh_interval_hours")
        private val KEY_LAST_UPDATED = longPreferencesKey("last_updated")
    }

    val loginState: Flow<LoginState> = ds.data.map { prefs ->
        val pin = prefs[KEY_PIN] ?: ""
        LoginState(
            isLoggedIn = pin.isNotBlank() && prefs[KEY_THOR]?.isNotBlank() == true,
            pin = pin,
            thor = prefs[KEY_THOR] ?: "",
            qidUid = prefs[KEY_QID_UID] ?: "",
            qidSid = prefs[KEY_QID_SID] ?: "",
            jdv = prefs[KEY_JDV] ?: ""
        )
    }

    val quotaInfo: Flow<QuotaInfo?> = ds.data.map { prefs ->
        prefs[KEY_QUOTA_JSON]?.let { json ->
            try {
                GsonUtils.fromJson<QuotaInfo>(json)
            } catch (e: Exception) {
                null
            }
        }
    }

    val refreshIntervalHours: Flow<Int> = ds.data.map { prefs ->
        prefs[KEY_REFRESH_INTERVAL] ?: 1
    }

    suspend fun saveLoginState(state: LoginState) {
        ds.edit { prefs ->
            prefs[KEY_PIN] = state.pin
            prefs[KEY_THOR] = state.thor
            prefs[KEY_QID_UID] = state.qidUid
            prefs[KEY_QID_SID] = state.qidSid
            prefs[KEY_JDV] = state.jdv
        }
    }

    suspend fun saveQuotaInfo(quota: QuotaInfo) {
        ds.edit { prefs ->
            prefs[KEY_QUOTA_JSON] = GsonUtils.toJson(quota)
            prefs[KEY_LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun setRefreshInterval(hours: Int) {
        ds.edit { prefs ->
            prefs[KEY_REFRESH_INTERVAL] = hours
        }
    }

    suspend fun clearAll() {
        ds.edit { it.clear() }
    }
}

object GsonUtils {
    private val gson = com.google.gson.Gson()

    fun <T> fromJson(json: String): T {
        return gson.fromJson(json, object : com.google.gson.reflect.TypeToken<T>() {}.type)
    }

    fun toJson(obj: Any): String = gson.toJson(obj)
}
