package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.audio.VoiceChangerEngine
import com.example.data.model.AppSettings
import com.example.data.model.SidebarEdge
import com.example.data.model.VoiceEngineState
import com.example.data.model.VoicePreset
import com.example.data.preferences.SettingsRepository
import com.example.ui.theme.ArifDeepBlack
import com.example.ui.theme.ArifGameBoosterTheme
import com.example.ui.theme.ArifSurfaceNavy
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingSidebarService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var voiceEngine: VoiceChangerEngine

    private var handleView: ComposeView? = null
    private var handleParams: WindowManager.LayoutParams? = null
    private var handleLifecycle: OverlayLifecycleOwner? = null

    private var panelView: ComposeView? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var panelLifecycle: OverlayLifecycleOwner? = null

    private var settingsJob: Job? = null
    private var currentEdge: SidebarEdge = SidebarEdge.RIGHT
    private var currentOpacity: Float = 0.92f
    private var isPanelExpanded = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository.getInstance(this)
        voiceEngine = VoiceChangerEngine.getInstance(this)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            teardownOverlays()
            _isRunning.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (handleView == null) {
            showHandleOverlay()
            observeSettings()
        }
        _isRunning.value = true
        return START_STICKY
    }

    private fun observeSettings() {
        settingsJob?.cancel()
        settingsJob = serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                val edgeChanged = currentEdge != settings.sidebarEdge
                currentEdge = settings.sidebarEdge
                currentOpacity = settings.sidebarOpacity
                if (edgeChanged) {
                    updateEdgeGravity()
                }
            }
        }
    }

    private fun updateEdgeGravity() {
        val gravity = if (currentEdge == SidebarEdge.LEFT) {
            Gravity.TOP or Gravity.START
        } else {
            Gravity.TOP or Gravity.END
        }
        handleParams?.let { params ->
            params.gravity = gravity
            handleView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
        }
        panelParams?.let { params ->
            params.gravity = gravity or Gravity.CENTER_VERTICAL
            panelView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showHandleOverlay() {
        val owner = OverlayLifecycleOwner().also { it.performCreate() }
        handleLifecycle = owner

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val gravity = if (currentEdge == SidebarEdge.LEFT) {
            Gravity.TOP or Gravity.START
        } else {
            Gravity.TOP or Gravity.END
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = 0
            y = dpToPx(180)
        }
        handleParams = params

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialY = 0
        var initialTouchY = 0f
        var initialTouchX = 0f
        var isDragging = false

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
                val engineState by voiceEngine.state.collectAsState()
                ArifGameBoosterTheme(themePreset = settings.themePreset) {
                    FloatingEdgeHandle(
                        edge = settings.sidebarEdge,
                        opacity = settings.sidebarOpacity,
                        isVoiceActive = engineState.isRunning
                    )
                }
            }
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = params.y
                        initialTouchY = event.rawY
                        initialTouchX = event.rawX
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = (event.rawY - initialTouchY).toInt()
                        val dx = (event.rawX - initialTouchX).toInt()
                        if (abs(dy) > touchSlop || abs(dx) > touchSlop) {
                            isDragging = true
                        }
                        if (isDragging) {
                            val maxY = (resources.displayMetrics.heightPixels - dpToPx(140)).coerceAtLeast(dpToPx(80))
                            params.y = (initialY + dy).coerceIn(dpToPx(56), maxY)
                            runCatching { windowManager.updateViewLayout(this, params) }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = event.rawX - initialTouchX
                        val swipedIn = (currentEdge == SidebarEdge.RIGHT && dx < -dpToPx(24)) ||
                            (currentEdge == SidebarEdge.LEFT && dx > dpToPx(24))
                        if (!isDragging || swipedIn) {
                            toggleExpandedPanel()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        handleView = view
        runCatching {
            windowManager.addView(view, params)
            owner.performResume()
        }
    }

    private fun toggleExpandedPanel() {
        if (isPanelExpanded) {
            hideExpandedPanel()
        } else {
            showExpandedPanel()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showExpandedPanel() {
        if (panelView != null) return
        isPanelExpanded = true

        val owner = OverlayLifecycleOwner().also { it.performCreate() }
        panelLifecycle = owner

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val gravity = if (currentEdge == SidebarEdge.LEFT) {
            Gravity.START or Gravity.CENTER_VERTICAL
        } else {
            Gravity.END or Gravity.CENTER_VERTICAL
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = dpToPx(10)
            y = 0
        }
        panelParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    hideExpandedPanel()
                    true
                } else {
                    false
                }
            }
            setContent {
                val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())
                val engineState by voiceEngine.state.collectAsState()
                ArifGameBoosterTheme(themePreset = settings.themePreset) {
                    FloatingSidebarPanel(
                        settings = settings,
                        engineState = engineState,
                        onToggleVoice = {
                            if (engineState.isRunning) {
                                VoiceChangerService.stop(this@FloatingSidebarService)
                            } else if (voiceEngine.hasRecordPermission()) {
                                VoiceChangerService.start(this@FloatingSidebarService)
                            } else {
                                openMainRoute(MainActivity.ROUTE_VOICE_STUDIO)
                                hideExpandedPanel()
                            }
                        },
                        onSelectPreset = { preset ->
                            if (voiceEngine.hasRecordPermission()) {
                                VoiceChangerService.selectPresetAndStart(this@FloatingSidebarService, preset)
                            } else {
                                voiceEngine.selectPreset(preset)
                                openMainRoute(MainActivity.ROUTE_VOICE_STUDIO)
                                hideExpandedPanel()
                            }
                        },
                        onOpenDashboard = {
                            openMainRoute(MainActivity.ROUTE_DASHBOARD)
                            hideExpandedPanel()
                        },
                        onOpenSettings = {
                            openMainRoute(MainActivity.ROUTE_SETTINGS)
                            hideExpandedPanel()
                        },
                        onFlipEdge = {
                            val next = if (settings.sidebarEdge == SidebarEdge.RIGHT) {
                                SidebarEdge.LEFT
                            } else {
                                SidebarEdge.RIGHT
                            }
                            serviceScope.launch { settingsRepository.setSidebarEdge(next) }
                        },
                        onCollapse = { hideExpandedPanel() },
                        onHideService = { stop(this@FloatingSidebarService) }
                    )
                }
            }
        }

        panelView = view
        runCatching {
            windowManager.addView(view, params)
            owner.performResume()
        }
    }

    private fun hideExpandedPanel() {
        isPanelExpanded = false
        panelLifecycle?.performPause()
        panelView?.let { runCatching { windowManager.removeView(it) } }
        panelLifecycle?.performDestroy()
        panelView = null
        panelLifecycle = null
        panelParams = null
    }

    private fun teardownOverlays() {
        hideExpandedPanel()
        handleLifecycle?.performPause()
        handleView?.let { runCatching { windowManager.removeView(it) } }
        handleLifecycle?.performDestroy()
        handleView = null
        handleLifecycle = null
        handleParams = null
    }

    private fun openMainRoute(route: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        startActivity(intent)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.sidebar_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.sidebar_service_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val hideIntent = Intent(this, FloatingSidebarService::class.java).apply {
            action = ACTION_STOP
        }
        val hidePending = PendingIntent.getService(
            this,
            202,
            hideIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ARIF Floating Gaming Sidebar")
            .setContentText("Edge handle active. Drag vertically or tap to expand shortcuts.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, getString(R.string.notification_action_hide_sidebar), hidePending)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        settingsJob?.cancel()
        teardownOverlays()
        _isRunning.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "arif_floating_sidebar_channel"
        private const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.example.action.START_FLOATING_SIDEBAR"
        const val ACTION_STOP = "com.example.action.STOP_FLOATING_SIDEBAR"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, FloatingSidebarService::class.java).apply {
                action = ACTION_START
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingSidebarService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }
    }
}

@Composable
private fun FloatingEdgeHandle(
    edge: SidebarEdge,
    opacity: Float,
    isVoiceActive: Boolean
) {
    val arif = LocalArifColors.current
    val shape = if (edge == SidebarEdge.RIGHT) {
        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = Modifier
            .width(34.dp)
            .height(68.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ArifSurfaceNavy.copy(alpha = opacity),
                        ArifDeepBlack.copy(alpha = opacity)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (isVoiceActive) NeonEmerald else arif.neonPrimary,
                        arif.neonSecondary
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "ARIF sidebar handle",
                tint = arif.neonPrimary,
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isVoiceActive) NeonEmerald else arif.neonSecondary)
            )
        }
    }
}

@Composable
private fun FloatingSidebarPanel(
    settings: AppSettings,
    engineState: VoiceEngineState,
    onToggleVoice: () -> Unit,
    onSelectPreset: (VoicePreset) -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onFlipEdge: () -> Unit,
    onCollapse: () -> Unit,
    onHideService: () -> Unit
) {
    val arif = LocalArifColors.current
    Surface(
        modifier = Modifier.width(248.dp),
        shape = RoundedCornerShape(22.dp),
        color = ArifDeepBlack.copy(alpha = settings.sidebarOpacity.coerceAtLeast(0.88f)),
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(listOf(arif.neonPrimary, arif.neonSecondary)),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = arif.neonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ARIF OVERLAY",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
                Row {
                    IconButton(
                        onClick = onFlipEdge,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Switch edge side",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Collapse sidebar",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Voice Changer primary toggle
            SidebarActionRow(
                icon = if (engineState.isRunning) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                label = if (engineState.isRunning) "Voice DSP Active" else "Voice Changer",
                badge = if (engineState.isRunning) "STOP" else "START",
                accent = if (engineState.isRunning) NeonEmerald else arif.neonPrimary,
                isSelected = engineState.isRunning,
                onClick = onToggleVoice
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Text(
                text = "QUICK VOICE PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            SidebarActionRow(
                icon = Icons.Rounded.RecordVoiceOver,
                label = "Deep Voice",
                badge = if (engineState.activePreset == VoicePreset.DEEP_MALE) "ON" else null,
                accent = arif.neonPrimary,
                isSelected = engineState.activePreset == VoicePreset.DEEP_MALE && engineState.isRunning,
                onClick = { onSelectPreset(VoicePreset.DEEP_MALE) }
            )
            SidebarActionRow(
                icon = Icons.Rounded.Speaker,
                label = "Heavy Bass",
                badge = if (engineState.activePreset == VoicePreset.HEAVY_BASS) "ON" else null,
                accent = arif.neonSecondary,
                isSelected = engineState.activePreset == VoicePreset.HEAVY_BASS && engineState.isRunning,
                onClick = { onSelectPreset(VoicePreset.HEAVY_BASS) }
            )
            SidebarActionRow(
                icon = Icons.Rounded.GraphicEq,
                label = "Monster Voice",
                badge = if (engineState.activePreset == VoicePreset.MONSTER) "ON" else null,
                accent = arif.neonPrimary,
                isSelected = engineState.activePreset == VoicePreset.MONSTER && engineState.isRunning,
                onClick = { onSelectPreset(VoicePreset.MONSTER) }
            )
            SidebarActionRow(
                icon = Icons.Rounded.SmartToy,
                label = "Robot Voice",
                badge = if (engineState.activePreset == VoicePreset.ROBOT) "ON" else null,
                accent = arif.neonSecondary,
                isSelected = engineState.activePreset == VoicePreset.ROBOT && engineState.isRunning,
                onClick = { onSelectPreset(VoicePreset.ROBOT) }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            SidebarActionRow(
                icon = Icons.Rounded.Speed,
                label = "Game Dashboard",
                badge = null,
                accent = arif.neonPrimary,
                isSelected = false,
                onClick = onOpenDashboard
            )
            SidebarActionRow(
                icon = Icons.Rounded.Settings,
                label = "Settings",
                badge = null,
                accent = TextSecondary,
                isSelected = false,
                onClick = onOpenSettings
            )

            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DangerRed.copy(alpha = 0.14f))
                    .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onHideService)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Hide Floating Handle",
                    style = MaterialTheme.typography.labelMedium,
                    color = DangerRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SidebarActionRow(
    icon: ImageVector,
    label: String,
    badge: String?,
    accent: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) accent.copy(alpha = 0.18f) else ArifSurfaceNavy.copy(alpha = 0.75f)
    val borderColor = if (isSelected) accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
