package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.games.GameLauncherRepository
import com.example.data.model.InstalledGame
import com.example.data.model.LaunchableAppEntry
import com.example.ui.components.ArifGlassCard
import com.example.ui.components.ArifNeonButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberTelemetry
import com.example.ui.theme.ArifDarkNavy
import com.example.ui.theme.ArifDeepBlack
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLauncherScreen(
    installedGames: List<InstalledGame>,
    allLaunchableApps: List<LaunchableAppEntry>,
    isLoading: Boolean,
    pinnedQuickLaunchPkg: String?,
    onRefreshGames: () -> Unit,
    onLaunchGame: (InstalledGame) -> Unit,
    onToggleCustomGame: (String) -> Unit,
    onSetQuickLaunchPin: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val arif = LocalArifColors.current
    var showCustomPicker by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val freeFireInstalled = remember(installedGames) {
        installedGames.firstOrNull {
            it.packageName == GameLauncherRepository.FREE_FIRE_PACKAGE ||
                it.packageName == GameLauncherRepository.FREE_FIRE_MAX_PACKAGE
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
                .testTag("game_launcher_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Game Launcher",
                    subtitle = "Detects installed games via Android PackageManager",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onRefreshGames,
                                modifier = Modifier.testTag("refresh_games_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Rescan installed games",
                                    tint = TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { showCustomPicker = true },
                                modifier = Modifier.testTag("open_custom_game_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCircleOutline,
                                    contentDescription = "Select custom games",
                                    tint = arif.neonPrimary
                                )
                            }
                        }
                    }
                )
            }

            // Dedicated Free Fire Launcher Shortcut Card
            item {
                ArifGlassCard(
                    accentBorder = true,
                    glowColor = if (freeFireInstalled != null) AmberTelemetry else arif.neonSecondary,
                    modifier = Modifier.testTag("free_fire_spotlight_card")
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
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AmberTelemetry.copy(alpha = 0.16f))
                                    .border(1.dp, AmberTelemetry.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (freeFireInstalled?.iconBitmap != null) {
                                    Image(
                                        bitmap = freeFireInstalled.iconBitmap,
                                        contentDescription = freeFireInstalled.appName,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = "Free Fire launcher shortcut",
                                        tint = AmberTelemetry,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = freeFireInstalled?.appName ?: "Free Fire Shortcut",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (freeFireInstalled != null) {
                                        "${freeFireInstalled.packageName} • v${freeFireInstalled.versionName}"
                                    } else {
                                        "Checked com.dts.freefireth & com.dts.freefiremax"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        StatusPill(
                            label = if (freeFireInstalled != null) "INSTALLED" else "NOT INSTALLED",
                            isActive = freeFireInstalled != null,
                            activeColor = NeonEmerald,
                            inactiveColor = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (freeFireInstalled != null) {
                        ArifNeonButton(
                            text = "Launch ${freeFireInstalled.appName}",
                            icon = Icons.Rounded.PlayArrow,
                            primaryColor = AmberTelemetry,
                            secondaryColor = arif.neonPrimary,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "launch_free_fire_button",
                            onClick = { onLaunchGame(freeFireInstalled) }
                        )
                    } else {
                        Text(
                            text = "Free Fire is not currently installed on this device. Install Free Fire or Free Fire MAX, or add any installed game below using Custom Game Selection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Custom Game Selection Action Card
            item {
                ArifGlassCard(
                    onClick = { showCustomPicker = true },
                    modifier = Modifier.testTag("custom_game_selection_card")
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
                            Icon(
                                imageVector = Icons.Rounded.AddCircleOutline,
                                contentDescription = null,
                                tint = arif.neonPrimary
                            )
                            Column {
                                Text(
                                    text = "Custom Game Selection",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Pin any installed app from ${allLaunchableApps.size} launchable packages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "Manage",
                            style = MaterialTheme.typography.labelLarge,
                            color = arif.neonPrimary
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = arif.neonPrimary)
                    }
                }
            } else if (installedGames.isEmpty()) {
                item {
                    ArifGlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = arif.neonPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "No Android Category Games Detected",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "No installed packages declare CATEGORY_GAME yet. Tap 'Custom Game Selection' above to add any installed app to your launcher.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "DETECTED & PINNED TITLES (${installedGames.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }

                items(installedGames, key = { it.packageName }) { game ->
                    val isQuickPinned = pinnedQuickLaunchPkg == game.packageName
                    ArifGlassCard(
                        accentBorder = isQuickPinned,
                        modifier = Modifier.testTag("game_card_${game.packageName}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ArifDarkNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                if (game.iconBitmap != null) {
                                    Image(
                                        bitmap = game.iconBitmap,
                                        contentDescription = game.appName,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.SportsEsports,
                                        contentDescription = game.appName,
                                        tint = arif.neonPrimary
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = game.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${game.packageName} • v${game.versionName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (game.isGameCategory) {
                                        TagChip("ANDROID GAME", arif.neonPrimary)
                                    }
                                    if (game.isCustomPinned) {
                                        TagChip("CUSTOM PIN", arif.neonSecondary)
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    onSetQuickLaunchPin(if (isQuickPinned) null else game.packageName)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isQuickPinned) Icons.Rounded.CheckCircle else Icons.Rounded.PushPin,
                                    contentDescription = "Pin to dashboard quick launch",
                                    tint = if (isQuickPinned) NeonEmerald else TextMuted
                                )
                            }

                            ArifNeonButton(
                                text = "Play",
                                icon = Icons.Rounded.PlayArrow,
                                onClick = { onLaunchGame(game) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showCustomPicker) {
        val filteredApps = remember(allLaunchableApps, searchQuery) {
            if (searchQuery.isBlank()) allLaunchableApps
            else {
                val q = searchQuery.trim().lowercase()
                allLaunchableApps.filter {
                    it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showCustomPicker = false },
            containerColor = ArifDarkNavy,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Custom Game Selection",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Toggle any installed app to include it in your ARIF Game Launcher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps or package name...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = arif.neonPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ArifDeepBlack.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ArifDarkNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap,
                                        contentDescription = app.appName,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.SportsEsports,
                                        contentDescription = null,
                                        tint = arif.neonPrimary
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = app.isPinned || app.isSystemGame,
                                enabled = !app.isSystemGame,
                                onCheckedChange = { onToggleCustomGame(app.packageName) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ArifDeepBlack,
                                    checkedTrackColor = arif.neonPrimary
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun TagChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
