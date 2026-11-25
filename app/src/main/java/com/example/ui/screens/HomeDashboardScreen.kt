package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppSettings
import com.example.data.model.DeviceTelemetry
import com.example.data.model.InstalledGame
import com.example.data.model.VoiceEngineState
import com.example.ui.SystemPermissionSnapshot
import com.example.ui.components.ArifGlassCard
import com.example.ui.components.ArifLogoBadge
import com.example.ui.components.ArifNeonButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.ArifDarkNavy
import com.example.ui.theme.ArifDeepBlack
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    telemetry: DeviceTelemetry,
    settings: AppSettings,
    voiceState: VoiceEngineState,
    isSidebarRunning: Boolean,
    permissions: SystemPermissionSnapshot,
    installedGames: List<InstalledGame>,
    brightnessFraction: Float,
    onRefreshTelemetry: () -> Unit,
    onToggleGamingMode: (Boolean) -> Unit,
    onToggleDnd: () -> Boolean,
    onUpdateBrightness: (Float) -> Unit,
    onToggleSidebar: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onLaunchGame: (InstalledGame) -> Unit,
    onNavigateToVoiceStudio: () -> Unit,
    onNavigateToLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val arif = LocalArifColors.current
    var showBrightnessSheet by remember { mutableStateOf(false) }

    val quickLaunchGame = remember(installedGames, settings.pinnedGamePackage) {
        installedGames.firstOrNull { it.packageName == settings.pinnedGamePackage }
            ?: installedGames.firstOrNull { it.isFreeFire }
            ?: installedGames.firstOrNull()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Brand Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ArifLogoBadge(size = 46.dp)
                        Column {
                            Text(
                                text = "ARIF GAME BOOSTER",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "ULTIMATE EDITION",
                                style = MaterialTheme.typography.labelSmall,
                                color = arif.neonPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPill(
                            label = if (voiceState.isRunning) "MIC DSP ON" else "DSP READY",
                            isActive = voiceState.isRunning,
                            activeColor = NeonEmerald,
                            inactiveColor = arif.neonPrimary
                        )
                        IconButton(
                            onClick = onRefreshTelemetry,
                            modifier = Modifier.testTag("refresh_telemetry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh device telemetry",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // Hero Banner Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(arif.neonPrimary.copy(alpha = 0.6f), arif.neonSecondary.copy(alpha = 0.4f))),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_arif_hero_banner),
                        contentDescription = "ARIF Gaming Command Center Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        ArifDeepBlack.copy(alpha = 0.35f),
                                        ArifDeepBlack.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                label = if (settings.gamingModeKeepScreenOn) "GAMING MODE ON" else "STANDARD MODE",
                                isActive = settings.gamingModeKeepScreenOn,
                                activeColor = arif.neonPrimary
                            )
                            StatusPill(
                                label = if (isSidebarRunning) "SIDEBAR ACTIVE" else "SIDEBAR OFF",
                                isActive = isSidebarRunning,
                                activeColor = arif.neonSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quickLaunchGame?.let { "Ready: ${it.appName}" } ?: "Game Launcher Ready",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = quickLaunchGame?.packageName ?: "Detect installed games or pin your favorite title",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            ArifNeonButton(
                                text = if (quickLaunchGame != null) "Launch" else "Games",
                                icon = if (quickLaunchGame != null) Icons.Rounded.PlayArrow else Icons.Rounded.RocketLaunch,
                                onClick = {
                                    if (quickLaunchGame != null) {
                                        onLaunchGame(quickLaunchGame)
                                    } else {
                                        onNavigateToLauncher()
                                    }
                                },
                                testTag = "dashboard_quick_launch_button"
                            )
                        }
                    }
                }
            }

            // Gaming Mode & Supported System Shortcuts
            item {
                ArifGlassCard(accentBorder = true) {
                    SectionHeader(
                        title = "Gaming Session Controls",
                        subtitle = "App-level preferences & Android system shortcuts"
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // App-Level Gaming Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ArifDarkNavy.copy(alpha = 0.7f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = arif.neonPrimary
                            )
                            Column {
                                Text(
                                    text = "App Gaming Mode (Keep Screen Awake)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Prevents screen timeout while ARIF telemetry & studio are open",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.gamingModeKeepScreenOn,
                            onCheckedChange = onToggleGamingMode,
                            modifier = Modifier.testTag("gaming_mode_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ArifDeepBlack,
                                checkedTrackColor = arif.neonPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3 Supported Shortcuts Row: DND, Brightness, Floating Sidebar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardShortcutTile(
                            icon = Icons.Rounded.DoNotDisturbOn,
                            title = "Do Not Disturb",
                            status = when {
                                !permissions.hasDndPolicyAccess -> "Grant Access"
                                permissions.isDndActive -> "DND Active"
                                else -> "Tap to Enable"
                            },
                            isHighlighted = permissions.isDndActive,
                            accent = arif.neonSecondary,
                            testTag = "dnd_shortcut_button",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!onToggleDnd()) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            }
                        )

                        DashboardShortcutTile(
                            icon = Icons.Rounded.BrightnessHigh,
                            title = "Brightness",
                            status = "${(brightnessFraction * 100).toInt()}%",
                            isHighlighted = true,
                            accent = arif.neonPrimary,
                            testTag = "brightness_shortcut_button",
                            modifier = Modifier.weight(1f),
                            onClick = { showBrightnessSheet = true }
                        )

                        DashboardShortcutTile(
                            icon = Icons.Rounded.Layers,
                            title = "Overlay Bar",
                            status = when {
                                !permissions.hasOverlayPermission -> "Grant Overlay"
                                isSidebarRunning -> "Running"
                                else -> "Tap to Show"
                            },
                            isHighlighted = isSidebarRunning,
                            accent = if (isSidebarRunning) NeonEmerald else arif.neonPrimary,
                            testTag = "sidebar_shortcut_button",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!permissions.hasOverlayPermission) {
                                    onRequestOverlayPermission()
                                } else {
                                    onToggleSidebar()
                                }
                            }
                        )
                    }
                }
            }

            // Voice Changer Studio Quick Entry
            item {
                ArifGlassCard(
                    accentBorder = voiceState.isRunning,
                    glowColor = if (voiceState.isRunning) NeonEmerald else arif.neonSecondary,
                    onClick = onNavigateToVoiceStudio,
                    modifier = Modifier.testTag("open_voice_studio_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(arif.neonSecondary.copy(alpha = 0.16f))
                                    .border(1.dp, arif.neonSecondary.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = arif.neonSecondary
                                )
                            }
                            Column {
                                Text(
                                    text = "Real-Time Voice Changer Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Preset: ${voiceState.activePreset.displayName} • ${voiceState.sampleRateHz / 1000} kHz local DSP",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        StatusPill(
                            label = if (voiceState.isRunning) "LIVE" else "OPEN",
                            isActive = voiceState.isRunning,
                            activeColor = NeonEmerald,
                            inactiveColor = arif.neonPrimary
                        )
                    }
                }
            }

            // Live Device Telemetry Section
            item {
                SectionHeader(
                    title = "Live Device Telemetry",
                    subtitle = "Verified Android OS hardware & resource readouts"
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryCard(
                            icon = Icons.Rounded.PhoneAndroid,
                            title = "Device & OS",
                            primaryValue = "${telemetry.manufacturer} ${telemetry.deviceModel}".trim(),
                            secondaryValue = "Android ${telemetry.androidVersion} (API ${telemetry.sdkInt})",
                            accent = arif.neonPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            icon = if (telemetry.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                            title = "Battery",
                            primaryValue = "${telemetry.batteryPercent}%",
                            secondaryValue = buildString {
                                append(if (telemetry.isCharging) "Charging" else "Discharging")
                                if (telemetry.batteryTempC > 0f) {
                                    append(" • ${String.format(Locale.US, "%.1f", telemetry.batteryTempC)}°C")
                                }
                            },
                            progress = telemetry.batteryPercent / 100f,
                            accent = if (telemetry.batteryPercent > 20) NeonEmerald else arif.danger,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryProgressCard(
                            icon = Icons.Rounded.Memory,
                            title = "Available RAM",
                            primaryValue = "${telemetry.availableRamMb} MB free",
                            secondaryValue = "${telemetry.usedRamMb} / ${telemetry.totalRamMb} MB used",
                            progress = telemetry.ramUsageFraction,
                            accent = arif.neonPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryProgressCard(
                            icon = Icons.Rounded.SdStorage,
                            title = "Internal Storage",
                            primaryValue = String.format(
                                Locale.US,
                                "%.1f / %.1f GB",
                                telemetry.usedStorageGb,
                                telemetry.totalStorageGb
                            ),
                            secondaryValue = "${((1f - telemetry.storageUsageFraction) * 100).toInt()}% free",
                            progress = telemetry.storageUsageFraction,
                            accent = arif.neonSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TelemetryCard(
                        icon = Icons.Rounded.DeveloperBoard,
                        title = "CPU Architecture & Clocks",
                        primaryValue = "${telemetry.cpuCores} Cores • ${telemetry.cpuAbi}",
                        secondaryValue = when {
                            telemetry.cpuCurFreqMhz != null && telemetry.cpuMaxFreqMhz != null ->
                                "Current: ${telemetry.cpuCurFreqMhz} MHz / Max: ${telemetry.cpuMaxFreqMhz} MHz"
                            telemetry.cpuMaxFreqMhz != null ->
                                "Max accessible clock: ${telemetry.cpuMaxFreqMhz} MHz"
                            else ->
                                "Per-core sysfs clock readouts are restricted by kernel SELinux on this Android build"
                        },
                        accent = arif.neonPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Honest System Capability Footnote
            item {
                ArifGlassCard {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = arif.neonPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Transparent Utility Guarantee: ARIF monitors real Android telemetry and adjusts supported app/system settings (DND, brightness, keep-screen-on, floating shortcuts, local audio DSP). It does not claim to overclock the CPU, artificially boost FPS, or clear protected OS memory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showBrightnessSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBrightnessSheet = false },
            containerColor = ArifDarkNavy,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Screen Brightness Control",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = if (permissions.canWriteSystemSettings) {
                        "Adjusting system screen brightness (${(brightnessFraction * 100).toInt()}%)."
                    } else {
                        "Adjusts brightness inside ARIF immediately. Grant 'Modify system settings' to control global device brightness."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Slider(
                    value = brightnessFraction,
                    onValueChange = onUpdateBrightness,
                    valueRange = 0.08f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = arif.neonPrimary,
                        activeTrackColor = arif.neonPrimary
                    )
                )
                if (!permissions.canWriteSystemSettings) {
                    ArifNeonButton(
                        text = "Grant Modify System Settings",
                        icon = Icons.Rounded.BrightnessHigh,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DashboardShortcutTile(
    icon: ImageVector,
    title: String,
    status: String,
    isHighlighted: Boolean,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isHighlighted) accent.copy(alpha = 0.14f) else ArifDarkNavy.copy(alpha = 0.72f)
    val stroke = if (isHighlighted) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f)

    ArifGlassCard(
        onClick = onClick,
        modifier = modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg)
                .border(1.dp, stroke, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHighlighted) accent else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TelemetryCard(
    icon: ImageVector,
    title: String,
    primaryValue: String,
    secondaryValue: String,
    accent: Color,
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    ArifGlassCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = primaryValue,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = secondaryValue,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (progress != null) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
private fun TelemetryProgressCard(
    icon: ImageVector,
    title: String,
    primaryValue: String,
    secondaryValue: String,
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    TelemetryCard(
        icon = icon,
        title = title,
        primaryValue = primaryValue,
        secondaryValue = secondaryValue,
        accent = accent,
        modifier = modifier,
        progress = progress
    )
}
