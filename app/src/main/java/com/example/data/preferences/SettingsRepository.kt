package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import com.example.data.model.AudioQuality
import com.example.data.model.SidebarEdge
import com.example.data.model.VoicePreset
import com.example.ui.theme.ArifThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.arifDataStore: DataStore<Preferences> by preferencesDataStore(name = "arif_booster_prefs")

class SettingsRepository private constructor(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val AUDIO_QUALITY = intPreferencesKey("audio_quality_hz")
        val MIC_SENSITIVITY = floatPreferencesKey("mic_sensitivity")
        val CLIPPING_PROTECTION = booleanPreferencesKey("clipping_protection")
        val HARDWARE_NOISE_SUPPRESSION = booleanPreferencesKey("hardware_noise_suppression")
        val LIVE_MONITORING_DEFAULT = booleanPreferencesKey("live_monitoring_default")
        val SIDEBAR_EDGE = stringPreferencesKey("sidebar_edge")
        val SIDEBAR_OPACITY = floatPreferencesKey("sidebar_opacity")
        val GAMING_MODE_KEEP_SCREEN_ON = booleanPreferencesKey("gaming_mode_keep_screen_on")
        val PINNED_GAME_PACKAGE = stringPreferencesKey("pinned_game_package")
        val CUSTOM_GAME_PACKAGES = stringSetPreferencesKey("custom_game_packages")
        val LAST_PRESET_ID = stringPreferencesKey("last_preset_id")
    }

    val settingsFlow: Flow<AppSettings> = context.arifDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_PRESET]
        val themePreset = ArifThemePreset.entries.firstOrNull { it.name == themeName }
            ?: ArifThemePreset.ELECTRIC_CYAN
        val sampleRate = prefs[Keys.AUDIO_QUALITY] ?: AudioQuality.BALANCED_44K.sampleRateHz
        val edgeName = prefs[Keys.SIDEBAR_EDGE]
        val sidebarEdge = SidebarEdge.entries.firstOrNull { it.name == edgeName } ?: SidebarEdge.RIGHT

        AppSettings(
            themePreset = themePreset,
            audioQuality = AudioQuality.fromSampleRate(sampleRate),
            micSensitivity = prefs[Keys.MIC_SENSITIVITY] ?: 1.15f,
            clippingProtection = prefs[Keys.CLIPPING_PROTECTION] ?: true,
            hardwareNoiseSuppression = prefs[Keys.HARDWARE_NOISE_SUPPRESSION] ?: true,
            liveMonitoringDefault = prefs[Keys.LIVE_MONITORING_DEFAULT] ?: true,
            sidebarEdge = sidebarEdge,
            sidebarOpacity = prefs[Keys.SIDEBAR_OPACITY] ?: 0.92f,
            gamingModeKeepScreenOn = prefs[Keys.GAMING_MODE_KEEP_SCREEN_ON] ?: false,
            pinnedGamePackage = prefs[Keys.PINNED_GAME_PACKAGE],
            customGamePackages = prefs[Keys.CUSTOM_GAME_PACKAGES] ?: emptySet(),
            lastPresetId = prefs[Keys.LAST_PRESET_ID] ?: VoicePreset.DEEP_MALE.id
        )
    }

    suspend fun setThemePreset(preset: ArifThemePreset) {
        context.arifDataStore.edit { it[Keys.THEME_PRESET] = preset.name }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.arifDataStore.edit { it[Keys.AUDIO_QUALITY] = quality.sampleRateHz }
    }

    suspend fun setMicSensitivity(sensitivity: Float) {
        context.arifDataStore.edit { it[Keys.MIC_SENSITIVITY] = sensitivity.coerceIn(0.4f, 2.5f) }
    }

    suspend fun setClippingProtection(enabled: Boolean) {
        context.arifDataStore.edit { it[Keys.CLIPPING_PROTECTION] = enabled }
    }

    suspend fun setHardwareNoiseSuppression(enabled: Boolean) {
        context.arifDataStore.edit { it[Keys.HARDWARE_NOISE_SUPPRESSION] = enabled }
    }

    suspend fun setLiveMonitoringDefault(enabled: Boolean) {
        context.arifDataStore.edit { it[Keys.LIVE_MONITORING_DEFAULT] = enabled }
    }

    suspend fun setSidebarEdge(edge: SidebarEdge) {
        context.arifDataStore.edit { it[Keys.SIDEBAR_EDGE] = edge.name }
    }

    suspend fun setSidebarOpacity(opacity: Float) {
        context.arifDataStore.edit { it[Keys.SIDEBAR_OPACITY] = opacity.coerceIn(0.5f, 1.0f) }
    }

    suspend fun setGamingModeKeepScreenOn(enabled: Boolean) {
        context.arifDataStore.edit { it[Keys.GAMING_MODE_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setPinnedGamePackage(packageName: String?) {
        context.arifDataStore.edit {
            if (packageName.isNullOrBlank()) {
                it.remove(Keys.PINNED_GAME_PACKAGE)
            } else {
                it[Keys.PINNED_GAME_PACKAGE] = packageName
            }
        }
    }

    suspend fun toggleCustomGamePackage(packageName: String) {
        context.arifDataStore.edit { prefs ->
            val current = prefs[Keys.CUSTOM_GAME_PACKAGES] ?: emptySet()
            prefs[Keys.CUSTOM_GAME_PACKAGES] = if (packageName in current) {
                current - packageName
            } else {
                current + packageName
            }
        }
    }

    suspend fun setLastPreset(preset: VoicePreset) {
        context.arifDataStore.edit { it[Keys.LAST_PRESET_ID] = preset.id }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
