package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.components.ArifCyberBackground
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.GameLauncherScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VoiceChangerScreen
import com.example.ui.theme.ArifDarkNavy
import com.example.ui.theme.ArifDeepBlack
import com.example.ui.theme.ArifGameBoosterTheme
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingIntentRoute by mutableStateOf<String?>(null)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
        if (Settings.canDrawOverlays(this)) {
            viewModel.startFloatingSidebar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntentRoute = intent?.getStringExtra(EXTRA_ROUTE)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
            val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
            val isSidebarRunning by viewModel.isSidebarRunning.collectAsStateWithLifecycle()
            val installedGames by viewModel.installedGames.collectAsStateWithLifecycle()
            val allLaunchableApps by viewModel.allLaunchableApps.collectAsStateWithLifecycle()
            val isLoadingGames by viewModel.isLoadingGames.collectAsStateWithLifecycle()
            val permissions by viewModel.permissions.collectAsStateWithLifecycle()
            val brightness by viewModel.appBrightness.collectAsStateWithLifecycle()

            LaunchedEffect(settings.gamingModeKeepScreenOn) {
                if (settings.gamingModeKeepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            LaunchedEffect(brightness) {
                val attrs = window.attributes
                attrs.screenBrightness = brightness
                window.attributes = attrs
            }

            ArifGameBoosterTheme(themePreset = settings.themePreset) {
                ArifMainApp(
                    viewModel = viewModel,
                    pendingRoute = pendingIntentRoute,
                    onConsumePendingRoute = { pendingIntentRoute = null },
                    onRequestOverlayPermission = { launchOverlaySettings() },
                    settings = settings,
                    telemetry = telemetry,
                    voiceState = voiceState,
                    isSidebarRunning = isSidebarRunning,
                    installedGames = installedGames,
                    allLaunchableApps = allLaunchableApps,
                    isLoadingGames = isLoadingGames,
                    permissions = permissions,
                    brightness = brightness
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
        viewModel.refreshTelemetryNow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentRoute = intent.getStringExtra(EXTRA_ROUTE)
    }

    private fun launchOverlaySettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        overlayPermissionLauncher.launch(intent)
    }

    companion object {
        const val EXTRA_ROUTE = "extra_route"
        const val ROUTE_SPLASH = "splash"
        const val ROUTE_DASHBOARD = "dashboard"
        const val ROUTE_LAUNCHER = "launcher"
        const val ROUTE_VOICE_STUDIO = "voice_studio"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_ABOUT = "about"
    }
}

private data class TopDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TOP_DESTINATIONS = listOf(
    TopDestination(
        route = MainActivity.ROUTE_DASHBOARD,
        label = "Dashboard",
        selectedIcon = Icons.Filled.Speed,
        unselectedIcon = Icons.Outlined.Speed
    ),
    TopDestination(
        route = MainActivity.ROUTE_LAUNCHER,
        label = "Launcher",
        selectedIcon = Icons.Filled.RocketLaunch,
        unselectedIcon = Icons.Outlined.RocketLaunch
    ),
    TopDestination(
        route = MainActivity.ROUTE_VOICE_STUDIO,
        label = "Voice",
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq
    ),
    TopDestination(
        route = MainActivity.ROUTE_SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@Composable
private fun ArifMainApp(
    viewModel: MainViewModel,
    pendingRoute: String?,
    onConsumePendingRoute: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    settings: com.example.data.model.AppSettings,
    telemetry: com.example.data.model.DeviceTelemetry,
    voiceState: com.example.data.model.VoiceEngineState,
    isSidebarRunning: Boolean,
    installedGames: List<com.example.data.model.InstalledGame>,
    allLaunchableApps: List<com.example.data.model.LaunchableAppEntry>,
    isLoadingGames: Boolean,
    permissions: com.example.ui.SystemPermissionSnapshot,
    brightness: Float
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val arif = LocalArifColors.current

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(pendingRoute, currentRoute) {
        if (!pendingRoute.isNullOrBlank() && currentRoute != null && currentRoute != MainActivity.ROUTE_SPLASH) {
            navController.navigate(pendingRoute) {
                popUpTo(MainActivity.ROUTE_DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            onConsumePendingRoute()
        }
    }

    val showNavigationChrome = currentRoute in TOP_DESTINATIONS.map { it.route }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ArifCyberBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (showNavigationChrome && !useNavigationRail) {
                        NavigationBar(
                            containerColor = ArifDarkNavy.copy(alpha = 0.96f),
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            TOP_DESTINATIONS.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTop(dest.route) },
                                    modifier = Modifier.testTag("nav_${dest.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ArifDeepBlack,
                                        selectedTextColor = arif.neonPrimary,
                                        indicatorColor = arif.neonPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .then(if (currentRoute != MainActivity.ROUTE_SPLASH) Modifier.statusBarsPadding() else Modifier)
                ) {
                    if (showNavigationChrome && useNavigationRail) {
                        NavigationRail(
                            containerColor = ArifDarkNavy.copy(alpha = 0.96f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            TOP_DESTINATIONS.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTop(dest.route) },
                                    modifier = Modifier.testTag("rail_${dest.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ArifDeepBlack,
                                        selectedTextColor = arif.neonPrimary,
                                        indicatorColor = arif.neonPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = MainActivity.ROUTE_SPLASH
                        ) {
                            composable(MainActivity.ROUTE_SPLASH) {
                                SplashScreen(
                                    onFinished = {
                                        val target = pendingRoute ?: MainActivity.ROUTE_DASHBOARD
                                        navController.navigate(target) {
                                            popUpTo(MainActivity.ROUTE_SPLASH) { inclusive = true }
                                        }
                                        if (pendingRoute != null) onConsumePendingRoute()
                                    }
                                )
                            }

                            composable(MainActivity.ROUTE_DASHBOARD) {
                                HomeDashboardScreen(
                                    telemetry = telemetry,
                                    settings = settings,
                                    voiceState = voiceState,
                                    isSidebarRunning = isSidebarRunning,
                                    permissions = permissions,
                                    installedGames = installedGames,
                                    brightnessFraction = brightness,
                                    onRefreshTelemetry = { viewModel.refreshTelemetryNow() },
                                    onToggleGamingMode = { viewModel.toggleGamingMode(it) },
                                    onToggleDnd = { viewModel.toggleDndMode() },
                                    onUpdateBrightness = { viewModel.updateBrightness(it) },
                                    onToggleSidebar = {
                                        if (isSidebarRunning) viewModel.stopFloatingSidebar()
                                        else viewModel.startFloatingSidebar()
                                    },
                                    onRequestOverlayPermission = onRequestOverlayPermission,
                                    onLaunchGame = { game -> viewModel.launchGame(game.packageName, game.appName) },
                                    onNavigateToVoiceStudio = { navigateTop(MainActivity.ROUTE_VOICE_STUDIO) },
                                    onNavigateToLauncher = { navigateTop(MainActivity.ROUTE_LAUNCHER) }
                                )
                            }

                            composable(MainActivity.ROUTE_LAUNCHER) {
                                GameLauncherScreen(
                                    installedGames = installedGames,
                                    allLaunchableApps = allLaunchableApps,
                                    isLoading = isLoadingGames,
                                    pinnedQuickLaunchPkg = settings.pinnedGamePackage,
                                    onRefreshGames = { viewModel.refreshGames() },
                                    onLaunchGame = { game -> viewModel.launchGame(game.packageName, game.appName) },
                                    onToggleCustomGame = { pkg -> viewModel.toggleCustomGame(pkg) },
                                    onSetQuickLaunchPin = { pkg -> viewModel.setPinnedQuickLaunchGame(pkg) }
                                )
                            }

                            composable(MainActivity.ROUTE_VOICE_STUDIO) {
                                VoiceChangerScreen(
                                    voiceState = voiceState,
                                    permissions = permissions,
                                    onRefreshPermissions = { viewModel.refreshPermissions() },
                                    onStartVoice = { viewModel.startVoiceChanger() },
                                    onStopVoice = { viewModel.stopVoiceChanger() },
                                    onSelectPreset = { viewModel.selectPreset(it) },
                                    onPitchChange = { viewModel.setPitchRatio(it) },
                                    onBassChange = { viewModel.setBassDb(it) },
                                    onIntensityChange = { viewModel.setIntensity(it) },
                                    onLiveMonitoringChange = { viewModel.setLiveMonitoring(it) },
                                    onClippingProtectionChange = { viewModel.setClippingProtection(it) },
                                    onClearError = { viewModel.clearVoiceError() }
                                )
                            }

                            composable(MainActivity.ROUTE_SETTINGS) {
                                SettingsScreen(
                                    settings = settings,
                                    isSidebarRunning = isSidebarRunning,
                                    permissions = permissions,
                                    onSelectTheme = { viewModel.setThemePreset(it) },
                                    onSelectAudioQuality = { viewModel.setAudioQuality(it) },
                                    onMicSensitivityChange = { viewModel.setMicSensitivity(it) },
                                    onHardwareNoiseSuppressionChange = { viewModel.setHardwareNoiseSuppression(it) },
                                    onClippingProtectionChange = { viewModel.setClippingProtection(it) },
                                    onLiveMonitoringDefaultChange = { viewModel.setLiveMonitoringDefault(it) },
                                    onToggleSidebar = {
                                        if (isSidebarRunning) viewModel.stopFloatingSidebar()
                                        else viewModel.startFloatingSidebar()
                                    },
                                    onRequestOverlayPermission = onRequestOverlayPermission,
                                    onSelectSidebarEdge = { viewModel.setSidebarEdge(it) },
                                    onSidebarOpacityChange = { viewModel.setSidebarOpacity(it) },
                                    onNavigateToAbout = { navController.navigate(MainActivity.ROUTE_ABOUT) }
                                )
                            }

                            composable(MainActivity.ROUTE_ABOUT) {
                                AboutScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
