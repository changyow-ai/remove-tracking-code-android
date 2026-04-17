package app.urlcleaner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ShareMode { ClipboardOnly, ReShare }

data class Settings(
    val shareMode: ShareMode,
    val historyEnabled: Boolean,
    val removeReferralMarketing: Boolean,
    val rulesVersion: String?,
    val rulesUpdatedAtEpochMs: Long,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            shareMode = prefs[KEY_SHARE_MODE]?.let { runCatching { ShareMode.valueOf(it) }.getOrNull() }
                ?: ShareMode.ClipboardOnly,
            historyEnabled = prefs[KEY_HISTORY_ENABLED] ?: false,
            removeReferralMarketing = prefs[KEY_REMOVE_REFERRAL] ?: true,
            rulesVersion = prefs[KEY_RULES_VERSION],
            rulesUpdatedAtEpochMs = prefs[KEY_RULES_UPDATED_AT] ?: 0L,
        )
    }

    suspend fun setShareMode(mode: ShareMode) = context.dataStore.edit { it[KEY_SHARE_MODE] = mode.name }
    suspend fun setHistoryEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_HISTORY_ENABLED] = enabled }
    suspend fun setRemoveReferralMarketing(enabled: Boolean) = context.dataStore.edit { it[KEY_REMOVE_REFERRAL] = enabled }
    suspend fun setRulesMeta(version: String?, updatedAtEpochMs: Long) = context.dataStore.edit { p ->
        if (version != null) p[KEY_RULES_VERSION] = version else p.remove(KEY_RULES_VERSION)
        p[KEY_RULES_UPDATED_AT] = updatedAtEpochMs
    }

    private companion object {
        val KEY_SHARE_MODE = stringPreferencesKey("share_mode")
        val KEY_HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val KEY_REMOVE_REFERRAL = booleanPreferencesKey("remove_referral_marketing")
        val KEY_RULES_VERSION = stringPreferencesKey("rules_version")
        val KEY_RULES_UPDATED_AT = longPreferencesKey("rules_updated_at")
    }
}
