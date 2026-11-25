package com.example.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceChangerEngine
import com.example.data.games.GameLauncherRepository
import com.example.data.model.AppSettings
import com.example.data.model.AudioQuality
import com.example.data.model.DeviceTelemetry
import com.example.data.model.InstalledGame
import com.example.data.model.LaunchableAppEntry
import com.example.data.model.SidebarEdge
import com.example.data.model.VoiceEngineState
import com.example.data.model.VoicePreset
import com.example.data.preferences.SettingsRepository
import com.example.data.telemetry.DeviceTelemetryRepository
import com.example.service.FloatingSidebarService
import com.example.service.VoiceChangerService
import com.example.ui.theme.ArifThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SystemPermissionSnapshot(
    val hasRecordAudio: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val hasDndPolicyAccess: Boolean = false,
    val isDndActive: Boolean = false,
    val canWriteSystemSettings: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settingsRepository = SettingsRepository.getInstance(appContext)
    private val telemetryRepository = DeviceTelemetryRepository(appContext)
    private val gameRepository = GameLauncherRepository(appContext)
    private val voiceEngine = VoiceChangerEngine.getInstance(appContext)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val voiceState: StateFlow<VoiceEngineState> = voiceEngine.state
    val isSidebarRunning: StateFlow<Boolean> = FloatingSidebarService.isRunning

    private val _telemetry = MutableStateFlow(telemetryRepository.readTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _installedGames = MutableStateFlow<List<InstalledGame>>(emptyList())
    val installedGames: StateFlow<List<InstalledGame>> = _installedGames.asStateFlow()

    private val _allLaunchableApps = MutableStateFlow<List<LaunchableAppEntry>>(emptyList())
    val allLaunchableApps: StateFlow<List<LaunchableAppEntry>> = _allLaunchableApps.asStateFlow()

    private val _isLoadingGames = MutableStateFlow(true)
    val isLoadingGames: StateFlow<Boolean> = _isLoadingGames.asStateFlow()

    private val _permissions = MutableStateFlow(SystemPermissionSnapshot())
    val permissions: StateFlow<SystemPermissionSnapshot> = _permissions.asStateFlow()

    private val _appBrightness = MutableStateFlow(readCurrentBrightness())
    val appBrightness: StateFlow<Float> = _appBrightness.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var appliedInitialSettings = false

    init {
        refreshPermissions()
        observeSettingsAndSyncEngine()
        startTelemetryPolling()
    }

    private fun observeSettingsAndSyncEngine() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { s ->
                voiceEngine.setMicSensitivity(s.micSensitivity)
                voiceEngine.setClippingProtection(s.clippingProtection)
                voiceEngine.setNoiseSuppressionEnabled(s.hardwareNoiseSuppression)
                voiceEngine.updateSampleRate(s.audioQuality.sampleRateHz)
                if (!appliedInitialSettings) {
                    appliedInitialSettings = true
                    voiceEngine.setLiveMonitoring(s.liveMonitoringDefault)
                    voiceEngine.selectPreset(VoicePreset.fromId(s.lastPresetId))
                }
                refreshGames(s.customGamePackages)
            }
        }
    }

    private fun startTelemetryPolling() {
        viewModelScope.launch {
            while (isActive) {
                val snapshot = withContext(Dispatchers.IO) { telemetryRepository.readTelemetry() }
                _telemetry.value = snapshot
                delay(3000L)
            }
        }
    }

    fun refreshTelemetryNow() {
        viewModelScope.launch {
            _telemetry.value = withContext(Dispatchers.IO) { telemetryRepository.readTelemetry() }
            refreshPermissions()
        }
    }

    fun refreshGames(customPinned: Set<String> = settings.value.customGamePackages) {
        viewModelScope.launch {
            _isLoadingGames.value = true
            val (games, apps) = withContext(Dispatchers.IO) {
                gameRepository.loadGamesAndApps(customPinned)
            }
            _installedGames.value = games
            _allLaunchableApps.value = apps
            _isLoadingGames.value = false
        }
    }

    fun refreshPermissions() {
        val hasAudio = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlay = Settings.canDrawOverlays(appContext)

        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dndGranted = notificationManager?.isNotificationPolicyAccessGranted == true
        val dndActive = if (dndGranted) {
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }

        val canWrite = Settings.System.canWrite(appContext)
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBattery = powerManager?.isIgnoringBatteryOptimizations(appContext.packageName) == true

        _permissions.value = SystemPermissionSnapshot(
            hasRecordAudio = hasAudio,
            hasOverlayPermission = hasOverlay,
            hasNotificationPermission = hasNotif,
            hasDndPolicyAccess = dndGranted,
            isDndActive = dndActive,
            canWriteSystemSettings = canWrite,
            isIgnoringBatteryOptimizations = ignoringBattery
        )
    }

    fun startVoiceChanger() {
        refreshPermissions()
        if (!voiceEngine.hasRecordPermission()) {
            emitMessage("Microphone permission is required to start local voice processing.")
            return
        }
        VoiceChangerService.start(appContext)
    }

    fun stopVoiceChanger() {
        VoiceChangerService.stop(appContext)
    }

    fun selectPreset(preset: VoicePreset) {
        voiceEngine.selectPreset(preset)
        viewModelScope.launch { settingsRepository.setLastPreset(preset) }
    }

    fun setPitchRatio(value: Float) = voiceEngine.setPitchRatio(value)
    fun setBassDb(value: Float) = voiceEngine.setBassDb(value)
    fun setIntensity(value: Float) = voiceEngine.setIntensity(value)
    fun setLiveMonitoring(enabled: Boolean) = voiceEngine.setLiveMonitoring(enabled)
    fun setClippingProtection(enabled: Boolean) {
        voiceEngine.setClippingProtection(enabled)
        viewModelScope.launch { settingsRepository.setClippingProtection(enabled) }
    }
    fun clearVoiceError() = voiceEngine.clearError()

    fun startFloatingSidebar() {
        refreshPermissions()
        if (!Settings.canDrawOverlays(appContext)) {
            emitMessage("Enable 'Display over other apps' permission first.")
            return
        }
        FloatingSidebarService.start(appContext)
    }

    fun stopFloatingSidebar() {
        FloatingSidebarService.stop(appContext)
    }

    fun launchGame(packageName: String, appName: String) {
        val launched = gameRepository.launchPackage(packageName)
        if (!launched) {
            emitMessage("Unable to launch $appName. Package may not have a launcher activity.")
        }
    }

    fun toggleGamingMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setGamingModeKeepScreenOn(enabled) }
    }

    fun toggleDndMode(): Boolean {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        if (!nm.isNotificationPolicyAccessGranted) {
            return false
        }
        val nextFilter = if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        runCatching {
            nm.setInterruptionFilter(nextFilter)
        }.onFailure {
            emitMessage("Could not update Do Not Disturb mode.")
        }
        refreshPermissions()
        return true
    }

    fun updateBrightness(fraction: Float) {
        val clamped = fraction.coerceIn(0.05f, 1.0f)
        _appBrightness.value = clamped
        if (Settings.System.canWrite(appContext)) {
            runCatching {
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    (clamped * 255f).toInt().coerceIn(10, 255)
                )
            }
        }
    }

    private fun readCurrentBrightness(): Float {
        return try {
            val raw = Settings.System.getInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                180
            )
            (raw / 255f).coerceIn(0.05f, 1.0f)
        } catch (_: Exception) {
            0.7f
        }
    }

    fun setThemePreset(preset: ArifThemePreset) {
        viewModelScope.launch { settingsRepository.setThemePreset(preset) }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch { settingsRepository.setAudioQuality(quality) }
    }

    fun setMicSensitivity(value: Float) {
        voiceEngine.setMicSensitivity(value)
        viewModelScope.launch { settingsRepository.setMicSensitivity(value) }
    }

    fun setHardwareNoiseSuppression(enabled: Boolean) {
        voiceEngine.setNoiseSuppressionEnabled(enabled)
        viewModelScope.launch { settingsRepository.setHardwareNoiseSuppression(enabled) }
    }

    fun setLiveMonitoringDefault(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLiveMonitoringDefault(enabled) }
    }

    fun setSidebarEdge(edge: SidebarEdge) {
        viewModelScope.launch { settingsRepository.setSidebarEdge(edge) }
    }

    fun setSidebarOpacity(opacity: Float) {
        viewModelScope.launch { settingsRepository.setSidebarOpacity(opacity) }
    }

    fun toggleCustomGame(packageName: String) {
        viewModelScope.launch { settingsRepository.toggleCustomGamePackage(packageName) }
    }

    fun setPinnedQuickLaunchGame(packageName: String?) {
        viewModelScope.launch { settingsRepository.setPinnedGamePackage(packageName) }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch { _toastMessage.emit(message) }
    }
}
