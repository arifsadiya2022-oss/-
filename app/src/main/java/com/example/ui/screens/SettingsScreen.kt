package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AppSettings
import com.example.data.model.AudioQuality
import com.example.data.model.SidebarEdge
import com.example.ui.SystemPermissionSnapshot
import com.example.ui.components.ArifGlassCard
import com.example.ui.components.ArifNeonButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberTelemetry
import com.example.ui.theme.ArifDarkNavy
import com.example.ui.theme.ArifDeepBlack
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ArifThemePreset
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    isSidebarRunning: Boolean,
    permissions: SystemPermissionSnapshot,
    onSelectTheme: (ArifThemePreset) -> Unit,
    onSelectAudioQuality: (AudioQuality) -> Unit,
    onMicSensitivityChange: (Float) -> Unit,
    onHardwareNoiseSuppressionChange: (Boolean) -> Unit,
    onClippingProtectionChange: (Boolean) -> Unit,
    onLiveMonitoringDefaultChange: (Boolean) -> Unit,
    onToggleSidebar: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onSelectSidebarEdge: (SidebarEdge) -> Unit,
    onSidebarOpacityChange: (Float) -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val arif = LocalArifColors.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Settings",
                    subtitle = "Configure theme, local DSP engine, and floating sidebar"
                )
            }

            // Theme Selection
            item {
                ArifGlassCard(accentBorder = true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = arif.neonPrimary
                        )
                        Text(
                            text = "ARIF Neon Theme",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArifThemePreset.entries.forEach { preset ->
                            val selected = settings.themePreset == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("theme_option_${preset.name}")
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) arif.neonPrimary.copy(alpha = 0.14f)
                                        else ArifDarkNavy.copy(alpha = 0.65f)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) arif.neonPrimary.copy(alpha = 0.65f)
                                        else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectTheme(preset) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectTheme(preset) },
                                    colors = RadioButtonDefaults.colors(selectedColor = arif.neonPrimary)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Voice Processing & Audio Quality Settings
            item {
                ArifGlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = arif.neonSecondary
                        )
                        Text(
                            text = "Voice Processing & Audio Quality",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    AudioQuality.entries.forEach { quality ->
                        val selected = settings.audioQuality == quality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) arif.neonSecondary.copy(alpha = 0.14f)
                                    else ArifDarkNavy.copy(alpha = 0.6f)
                                )
                                .clickable { onSelectAudioQuality(quality) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onSelectAudioQuality(quality) },
                                colors = RadioButtonDefaults.colors(selectedColor = arif.neonSecondary)
                            )
                            Column {
                                Text(
                                    text = quality.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = quality.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Microphone Sensitivity",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", settings.micSensitivity)}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = arif.neonPrimary
                        )
                    }
                    Slider(
                        value = settings.micSensitivity,
                        onValueChange = onMicSensitivityChange,
                        valueRange = 0.4f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = arif.neonPrimary,
                            activeTrackColor = arif.neonPrimary
                        )
                    )

                    SettingsToggleRow(
                        title = "Hardware Noise Suppression",
                        subtitle = "Uses Android NoiseSuppressor where supported by device",
                        checked = settings.hardwareNoiseSuppression,
                        onCheckedChange = onHardwareNoiseSuppressionChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleRow(
                        title = "Clipping Limiter Protection",
                        subtitle = "Soft-clips peak transients above threshold",
                        checked = settings.clippingProtection,
                        onCheckedChange = onClippingProtectionChange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleRow(
                        title = "Live Monitoring on Startup",
                        subtitle = "Default headset playback state when Voice Studio opens",
                        checked = settings.liveMonitoringDefault,
                        onCheckedChange = onLiveMonitoringDefaultChange
                    )
                }
            }

            // Floating Gaming Sidebar Settings
            item {
                ArifGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Layers,
                                contentDescription = null,
                                tint = arif.neonPrimary
                            )
                            Text(
                                text = "Floating Gaming Sidebar",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        StatusPill(
                            label = if (isSidebarRunning) "ACTIVE" else "OFF",
                            isActive = isSidebarRunning,
                            activeColor = NeonEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ArifNeonButton(
                        text = when {
                            !permissions.hasOverlayPermission -> "Grant Overlay Permission"
                            isSidebarRunning -> "Hide Floating Sidebar"
                            else -> "Launch Floating Sidebar"
                        },
                        icon = Icons.Rounded.Layers,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_sidebar_toggle_button",
                        onClick = {
                            if (!permissions.hasOverlayPermission) {
                                onRequestOverlayPermission()
                            } else {
                                onToggleSidebar()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Sidebar Handle Position",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SidebarEdge.entries.forEach { edge ->
                            val selected = settings.sidebarEdge == edge
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) arif.neonPrimary.copy(alpha = 0.18f)
                                        else ArifDarkNavy.copy(alpha = 0.7f)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) arif.neonPrimary else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectSidebarEdge(edge) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = edge.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) arif.neonPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Overlay Opacity",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(settings.sidebarOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = arif.neonPrimary
                        )
                    }
                    Slider(
                        value = settings.sidebarOpacity,
                        onValueChange = onSidebarOpacityChange,
                        valueRange = 0.55f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = arif.neonPrimary,
                            activeTrackColor = arif.neonPrimary
                        )
                    )
                }
            }

            // Battery Optimization Guidance
            item {
                ArifGlassCard(
                    accentBorder = !permissions.isIgnoringBatteryOptimizations,
                    glowColor = AmberTelemetry
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BatterySaver,
                                contentDescription = null,
                                tint = if (permissions.isIgnoringBatteryOptimizations) NeonEmerald else AmberTelemetry
                            )
                            Text(
                                text = "Battery Optimization Guidance",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        StatusPill(
                            label = if (permissions.isIgnoringBatteryOptimizations) "UNRESTRICTED" else "OPTIMIZED",
                            isActive = permissions.isIgnoringBatteryOptimizations,
                            activeColor = NeonEmerald,
                            inactiveColor = AmberTelemetry
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (permissions.isIgnoringBatteryOptimizations) {
                            "ARIF is set to Unrestricted battery usage so the Floating Sidebar and local audio DSP foreground services stay stable during long gaming sessions."
                        } else {
                            "Aggressive OEM battery savers may pause the Floating Sidebar or local Voice DSP in the background. Set ARIF to 'Unrestricted' in Android Battery settings for uninterrupted sessions."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ArifNeonButton(
                        text = "Open Battery Optimization Settings",
                        icon = Icons.Rounded.SettingsInputComponent,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }.onFailure {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                )
                            }
                        }
                    )
                }
            }

            // App Information & About Navigation
            item {
                ArifGlassCard(
                    onClick = onNavigateToAbout,
                    modifier = Modifier.testTag("open_about_screen_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = arif.neonPrimary
                            )
                            Column {
                                Text(
                                    text = "About ARIF Game Booster",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Version ${BuildConfig.VERSION_NAME} • Architecture & Privacy",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "View",
                            style = MaterialTheme.typography.labelLarge,
                            color = arif.neonPrimary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val arif = LocalArifColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ArifDarkNavy.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ArifDeepBlack,
                checkedTrackColor = arif.neonPrimary
            )
        )
    }
}
