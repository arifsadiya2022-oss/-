package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.VoiceEngineState
import com.example.data.model.VoicePreset
import com.example.ui.SystemPermissionSnapshot
import com.example.ui.components.ArifGlassCard
import com.example.ui.components.ArifNeonButton
import com.example.ui.components.MicWaveformMeter
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberTelemetry
import com.example.ui.theme.ArifDarkNavy
import com.example.ui.theme.ArifDeepBlack
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun VoiceChangerScreen(
    voiceState: VoiceEngineState,
    permissions: SystemPermissionSnapshot,
    onRefreshPermissions: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSelectPreset: (VoicePreset) -> Unit,
    onPitchChange: (Float) -> Unit,
    onBassChange: (Float) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onLiveMonitoringChange: (Boolean) -> Unit,
    onClippingProtectionChange: (Boolean) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val arif = LocalArifColors.current
    var showMicDeniedNotice by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(permissions.hasRecordAudio) {
        if (permissions.hasRecordAudio) {
            showMicDeniedNotice = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onRefreshPermissions()
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true || permissions.hasRecordAudio
        if (micGranted) {
            showMicDeniedNotice = false
            onStartVoice()
        } else {
            showMicDeniedNotice = true
        }
    }

    fun requestAndStart() {
        val permsToRequest = buildList {
            if (!permissions.hasRecordAudio) add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.hasNotificationPermission) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permsToRequest.isEmpty()) {
            showMicDeniedNotice = false
            onStartVoice()
        } else {
            permissionLauncher.launch(permsToRequest.toTypedArray())
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("voice_changer_studio_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Voice Changer Studio",
                    subtitle = "100% local Android AudioRecord & AudioTrack DSP",
                    trailing = {
                        StatusPill(
                            label = if (voiceState.isRunning) "MIC LIVE" else "STANDBY",
                            isActive = voiceState.isRunning,
                            activeColor = NeonEmerald,
                            inactiveColor = TextSecondary
                        )
                    }
                )
            }

            // Live Mic Monitor & Primary Start/Stop Card
            item {
                ArifGlassCard(
                    accentBorder = true,
                    glowColor = if (voiceState.isRunning) NeonEmerald else arif.neonPrimary
                ) {
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
                                imageVector = if (voiceState.isRunning) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                                contentDescription = "Microphone status",
                                tint = if (voiceState.isRunning) NeonEmerald else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (voiceState.isRunning) {
                                    "Active: ${voiceState.activePreset.displayName}"
                                } else {
                                    "Ready: ${voiceState.activePreset.displayName}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Local processing only",
                                tint = NeonEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "LOCAL DSP",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    MicWaveformMeter(
                        waveformBins = voiceState.waveformBins,
                        normalizedLevel = voiceState.normalizedLevel,
                        isRunning = voiceState.isRunning,
                        isClipping = voiceState.isClippingDetected
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Telemetry readout strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MIC: ${String.format(Locale.US, "%.0f", voiceState.micLevelDb)} dBFS",
                            style = MaterialTheme.typography.labelSmall,
                            color = arif.neonPrimary
                        )
                        Text(
                            text = "${voiceState.sampleRateHz / 1000} kHz • ${if (voiceState.isRunning) "${voiceState.latencyMs} ms" else "Idle"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = when {
                                voiceState.isClippingDetected -> "LIMITING"
                                voiceState.hardwareNoiseSuppressorActive -> "NS ON"
                                else -> "CLIP SAFE"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (voiceState.isClippingDetected) AmberTelemetry else NeonEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (voiceState.isRunning) {
                        ArifNeonButton(
                            text = "Stop Voice Changer",
                            icon = Icons.Rounded.Stop,
                            primaryColor = DangerRed,
                            secondaryColor = AmberTelemetry,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "stop_voice_changer_button",
                            onClick = onStopVoice
                        )
                    } else {
                        ArifNeonButton(
                            text = "Start Voice Changer",
                            icon = Icons.Rounded.Mic,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "start_voice_changer_button",
                            onClick = { requestAndStart() }
                        )
                    }
                }
            }

            // Error / Permission Notice if needed
            if (showMicDeniedNotice || voiceState.errorMessage != null) {
                item {
                    ArifGlassCard(
                        accentBorder = true,
                        glowColor = DangerRed
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = AmberTelemetry
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = voiceState.errorMessage
                                        ?: "Microphone access is denied. Enable Microphone permission in App Settings to process live voice locally.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ArifNeonButton(
                                        text = if (showMicDeniedNotice) "App Settings" else "Dismiss",
                                        onClick = {
                                            if (showMicDeniedNotice) {
                                                context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.parse("package:${context.packageName}")
                                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                                )
                                            } else {
                                                onClearError()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Voice Presets Selector (8 Presets in clean 2-column rows)
            item {
                SectionHeader(
                    title = "Voice Effect Presets",
                    subtitle = "Tap any preset for instant real-time DSP switching"
                )
            }

            val presetRows = VoicePreset.entries.chunked(2)
            items(presetRows.size) { rowIndex ->
                val pair = presetRows[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { preset ->
                        PresetCard(
                            preset = preset,
                            isSelected = voiceState.activePreset == preset,
                            onClick = { onSelectPreset(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Fine-Tune DSP Sliders
            item {
                ArifGlassCard(accentBorder = true) {
                    SectionHeader(
                        title = "DSP Calibration",
                        subtitle = "Pitch shifting, bass shelf EQ, and effect blend"
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    DspSliderRow(
                        label = "Pitch Shift",
                        valueText = "${String.format(Locale.US, "%.2f", voiceState.pitchRatio)}x",
                        value = voiceState.pitchRatio,
                        valueRange = 0.55f..1.85f,
                        accent = arif.neonPrimary,
                        testTag = "pitch_slider",
                        onValueChange = onPitchChange
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DspSliderRow(
                        label = "Bass Enhancement",
                        valueText = "${if (voiceState.bassDb >= 0) "+" else ""}${String.format(Locale.US, "%.1f", voiceState.bassDb)} dB",
                        value = voiceState.bassDb,
                        valueRange = -6f..14f,
                        accent = arif.neonSecondary,
                        testTag = "bass_slider",
                        onValueChange = onBassChange
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DspSliderRow(
                        label = "Voice Intensity",
                        valueText = "${(voiceState.intensity * 100).toInt()}%",
                        value = voiceState.intensity,
                        valueRange = 0f..1f,
                        accent = NeonEmerald,
                        testTag = "intensity_slider",
                        onValueChange = onIntensityChange
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Local Monitoring Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ArifDarkNavy.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = arif.neonPrimary
                            )
                            Column {
                                Text(
                                    text = "Live Local Monitoring",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Use wired/Bluetooth headphones to prevent speaker feedback echo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = voiceState.liveMonitoring,
                            onCheckedChange = onLiveMonitoringChange,
                            modifier = Modifier.testTag("live_monitoring_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ArifDeepBlack,
                                checkedTrackColor = arif.neonPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clipping Protection Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ArifDarkNavy.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = NeonEmerald
                            )
                            Column {
                                Text(
                                    text = "Audio Clipping Protection",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Soft-knee limiter prevents harsh digital distortion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = voiceState.clippingProtection,
                            onCheckedChange = onClippingProtectionChange,
                            modifier = Modifier.testTag("clipping_protection_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ArifDeepBlack,
                                checkedTrackColor = NeonEmerald
                            )
                        )
                    }
                }
            }

            // Mandatory Honest Android Microphone Routing Limitation Disclosure
            item {
                ArifGlassCard(
                    accentBorder = true,
                    glowColor = AmberTelemetry,
                    modifier = Modifier.testTag("android_limitation_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = AmberTelemetry,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Android Microphone Routing Limitation",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberTelemetry
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Standard Android OS audio security isolates each app's microphone capture stream. A regular Android app cannot override or inject processed audio into another app's voice chat (such as Free Fire squad mic) without OEM system-level audio HAL support, root access, or an external USB/TRRS loopback adapter.\n\nWhat works here: real-time local microphone DSP processing, live headset monitoring, and instant preset tuning—processed strictly on-device with zero recordings saved or uploaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PresetCard(
    preset: VoicePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arif = LocalArifColors.current
    val borderColor = if (isSelected) arif.neonPrimary else Color.White.copy(alpha = 0.10f)
    val bgColor = if (isSelected) arif.neonPrimary.copy(alpha = 0.16f) else ArifDarkNavy.copy(alpha = 0.75f)
    val icon = presetIcon(preset)

    Column(
        modifier = modifier
            .testTag("preset_card_${preset.id}")
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) arif.neonPrimary else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = preset.displayName,
                    tint = if (isSelected) ArifDeepBlack else arif.neonPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (isSelected) {
                Text(
                    text = "ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = arif.neonPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = preset.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = preset.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DspSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
            )
        )
    }
}

private fun presetIcon(preset: VoicePreset): ImageVector {
    return when (preset) {
        VoicePreset.DEEP_MALE -> Icons.Rounded.RecordVoiceOver
        VoicePreset.HEAVY_BASS -> Icons.Rounded.Speaker
        VoicePreset.MONSTER -> Icons.Rounded.GraphicEq
        VoicePreset.ROBOT -> Icons.Rounded.SmartToy
        VoicePreset.FEMALE_STYLE -> Icons.Rounded.AutoFixHigh
        VoicePreset.CHIPMUNK -> Icons.Rounded.Mic
        VoicePreset.RADIO_VOICE -> Icons.Rounded.Radio
        VoicePreset.CUSTOM_VOICE -> Icons.Rounded.Tune
    }
}
