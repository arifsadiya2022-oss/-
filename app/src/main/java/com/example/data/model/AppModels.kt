package com.example.data.model

import androidx.compose.ui.graphics.ImageBitmap
import com.example.ui.theme.ArifThemePreset

enum class VoicePreset(
    val id: String,
    val displayName: String,
    val shortName: String,
    val pitchRatio: Float,
    val bassDb: Float,
    val intensity: Float,
    val formantCutoffHz: Float,
    val description: String
) {
    DEEP_MALE(
        id = "deep_male",
        displayName = "DEEP MALE",
        shortName = "Deep",
        pitchRatio = 0.78f,
        bassDb = 6.5f,
        intensity = 0.90f,
        formantCutoffHz = 3600f,
        description = "Lowered vocal pitch with warm chest resonance"
    ),
    HEAVY_BASS(
        id = "heavy_bass",
        displayName = "HEAVY BASS",
        shortName = "Bass",
        pitchRatio = 0.87f,
        bassDb = 11.5f,
        intensity = 0.92f,
        formantCutoffHz = 3100f,
        description = "Sub-heavy low shelf boost for booming voice depth"
    ),
    MONSTER(
        id = "monster",
        displayName = "MONSTER",
        shortName = "Monster",
        pitchRatio = 0.65f,
        bassDb = 10.0f,
        intensity = 0.95f,
        formantCutoffHz = 2200f,
        description = "Sub-octave growl layering and dark cavern timbre"
    ),
    ROBOT(
        id = "robot",
        displayName = "ROBOT",
        shortName = "Robot",
        pitchRatio = 0.96f,
        bassDb = 2.0f,
        intensity = 0.88f,
        formantCutoffHz = 3400f,
        description = "Synthetic ring-modulated cybernetic vocoder tone"
    ),
    FEMALE_STYLE(
        id = "female_style",
        displayName = "FEMALE STYLE",
        shortName = "Bright",
        pitchRatio = 1.32f,
        bassDb = -3.0f,
        intensity = 0.90f,
        formantCutoffHz = 5200f,
        description = "Elevated vocal pitch with bright upper formant clarity"
    ),
    CHIPMUNK(
        id = "chipmunk",
        displayName = "CHIPMUNK",
        shortName = "Chipmunk",
        pitchRatio = 1.65f,
        bassDb = -5.0f,
        intensity = 0.95f,
        formantCutoffHz = 6000f,
        description = "High-speed pitch shift with crisp high-mid sparkle"
    ),
    RADIO_VOICE(
        id = "radio_voice",
        displayName = "RADIO VOICE",
        shortName = "Radio",
        pitchRatio = 0.98f,
        bassDb = -4.0f,
        intensity = 0.92f,
        formantCutoffHz = 3000f,
        description = "Tactical squad comms bandpass filter with subtle drive"
    ),
    CUSTOM_VOICE(
        id = "custom_voice",
        displayName = "CUSTOM VOICE",
        shortName = "Custom",
        pitchRatio = 1.00f,
        bassDb = 0.0f,
        intensity = 0.85f,
        formantCutoffHz = 4400f,
        description = "Manual pitch, bass, and wet/dry intensity calibration"
    );

    companion object {
        fun fromId(id: String?): VoicePreset =
            entries.firstOrNull { it.id == id } ?: DEEP_MALE
    }
}

enum class AudioQuality(val sampleRateHz: Int, val label: String, val detail: String) {
    LOW_LATENCY_24K(24000, "24 kHz Low Latency", "Fastest DSP turnaround"),
    BALANCED_44K(44100, "44.1 kHz Balanced", "CD-grade clarity & response"),
    STUDIO_48K(48000, "48 kHz Studio HD", "Maximum frequency resolution");

    companion object {
        fun fromSampleRate(rate: Int): AudioQuality =
            entries.firstOrNull { it.sampleRateHz == rate } ?: BALANCED_44K
    }
}

enum class SidebarEdge(val label: String) {
    LEFT("Left Edge"),
    RIGHT("Right Edge")
}

data class VoiceEngineState(
    val isRunning: Boolean = false,
    val activePreset: VoicePreset = VoicePreset.DEEP_MALE,
    val pitchRatio: Float = VoicePreset.DEEP_MALE.pitchRatio,
    val bassDb: Float = VoicePreset.DEEP_MALE.bassDb,
    val intensity: Float = VoicePreset.DEEP_MALE.intensity,
    val micSensitivity: Float = 1.15f,
    val liveMonitoring: Boolean = true,
    val clippingProtection: Boolean = true,
    val noiseSuppressionEnabled: Boolean = true,
    val hardwareNoiseSuppressorActive: Boolean = false,
    val sampleRateHz: Int = AudioQuality.BALANCED_44K.sampleRateHz,
    val micLevelDb: Float = -60f,
    val normalizedLevel: Float = 0f,
    val waveformBins: List<Float> = List(16) { 0f },
    val isClippingDetected: Boolean = false,
    val latencyMs: Int = 0,
    val errorMessage: String? = null
)

data class DeviceTelemetry(
    val deviceModel: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val sdkInt: Int = 0,
    val totalRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val usedStorageGb: Float = 0f,
    val totalStorageGb: Float = 0f,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 0f,
    val cpuCores: Int = 0,
    val cpuAbi: String = "",
    val cpuMaxFreqMhz: Int? = null,
    val cpuCurFreqMhz: Int? = null
) {
    val usedRamMb: Long get() = (totalRamMb - availableRamMb).coerceAtLeast(0L)
    val ramUsageFraction: Float
        get() = if (totalRamMb > 0L) (usedRamMb.toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f) else 0f
    val storageUsageFraction: Float
        get() = if (totalStorageGb > 0f) (usedStorageGb / totalStorageGb).coerceIn(0f, 1f) else 0f
}

data class InstalledGame(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isGameCategory: Boolean,
    val isFreeFire: Boolean,
    val isCustomPinned: Boolean,
    val iconBitmap: ImageBitmap?
)

data class LaunchableAppEntry(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemGame: Boolean,
    val isPinned: Boolean,
    val iconBitmap: ImageBitmap?
)

data class AppSettings(
    val themePreset: ArifThemePreset = ArifThemePreset.ELECTRIC_CYAN,
    val audioQuality: AudioQuality = AudioQuality.BALANCED_44K,
    val micSensitivity: Float = 1.15f,
    val clippingProtection: Boolean = true,
    val hardwareNoiseSuppression: Boolean = true,
    val liveMonitoringDefault: Boolean = true,
    val sidebarEdge: SidebarEdge = SidebarEdge.RIGHT,
    val sidebarOpacity: Float = 0.92f,
    val gamingModeKeepScreenOn: Boolean = false,
    val pinnedGamePackage: String? = null,
    val customGamePackages: Set<String> = emptySet(),
    val lastPresetId: String = VoicePreset.DEEP_MALE.id
)
