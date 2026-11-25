package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.ArifGlassCard
import com.example.ui.components.ArifLogoBadge
import com.example.ui.theme.AmberTelemetry
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arif = LocalArifColors.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .testTag("about_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("about_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "About ARIF",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
            }

            item {
                ArifGlassCard(accentBorder = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ArifLogoBadge(size = 84.dp)
                        Text(
                            text = stringResource(R.string.brand_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${stringResource(R.string.brand_edition)} • v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelLarge,
                            color = arif.neonPrimary
                        )
                        Text(
                            text = stringResource(R.string.brand_tagline),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }

            item {
                AboutInfoCard(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Local Audio DSP Architecture",
                    body = "Built natively on Android AudioRecord and low-latency AudioTrack streams. Voice presets use a Hann-windowed dual-tap granular pitch shifter, biquad low-shelf bass enhancer, peaking formant equalizer, tactical bandpass / ring-modulation stages, and a soft-knee clipping limiter."
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Rounded.Lock,
                    title = "Strict Privacy & Offline Guarantee",
                    body = "ARIF requires zero internet permission for voice processing. Audio is processed frame-by-frame in volatile RAM and is never recorded to storage or transmitted to any server. A persistent foreground notification and active mic badge let you stop capture immediately."
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Rounded.Security,
                    title = "Android Platform Transparency",
                    body = "On standard unrooted Android devices, apps cannot hijack or overwrite another app's microphone input (including Free Fire squad voice chat). ARIF provides real local voice processing and monitoring, real PackageManager game launching, verified system telemetry, and an optional overlay sidebar.",
                    accent = AmberTelemetry
                )
            }

            item {
                AboutInfoCard(
                    icon = Icons.Rounded.VerifiedUser,
                    title = "Permissions Used Only On Demand",
                    body = "• RECORD_AUDIO: Requested only when starting Voice Changer.\n• SYSTEM_ALERT_WINDOW: Requested only when enabling Floating Gaming Sidebar.\n• POST_NOTIFICATIONS: Required on Android 13+ for foreground service controls.\n• ACCESS_NOTIFICATION_POLICY / WRITE_SETTINGS: Optional shortcuts for DND and Brightness.",
                    accent = NeonEmerald
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun AboutInfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    accent: androidx.compose.ui.graphics.Color? = null
) {
    val arif = LocalArifColors.current
    val tint = accent ?: arif.neonPrimary
    ArifGlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = tint
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
