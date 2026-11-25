package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ArifDeepBlack
import com.example.ui.theme.ArifSurfaceNavy
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ArifCyberBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val arif = LocalArifColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArifDeepBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(arif.neonPrimary.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.08f),
                    radius = size.maxDimension * 0.55f
                ),
                radius = size.maxDimension * 0.55f,
                center = Offset(size.width * 0.12f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(arif.neonSecondary.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.42f),
                    radius = size.maxDimension * 0.5f
                ),
                radius = size.maxDimension * 0.5f,
                center = Offset(size.width * 0.92f, size.height * 0.42f)
            )
        }
        content()
    }
}

@Composable
fun ArifGlassCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val arif = LocalArifColors.current
    val resolvedGlow = glowColor ?: arif.neonPrimary
    val shape = RoundedCornerShape(20.dp)
    val borderBrush = if (accentBorder) {
        Brush.linearGradient(listOf(resolvedGlow.copy(alpha = 0.75f), arif.neonSecondary.copy(alpha = 0.55f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f)))
    }

    Surface(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = arif.glassSurface,
        border = BorderStroke(1.dp, borderBrush),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ArifSurfaceNavy.copy(alpha = 0.42f),
                            ArifDeepBlack.copy(alpha = 0.28f)
                        )
                    )
                )
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ArifNeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryColor: Color? = null,
    secondaryColor: Color? = null,
    testTag: String? = null
) {
    val arif = LocalArifColors.current
    val c1 = primaryColor ?: arif.neonPrimary
    val c2 = secondaryColor ?: arif.neonSecondary
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .height(50.dp)
            .shadow(10.dp, shape, ambientColor = c1, spotColor = c1)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(c1, c2)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ArifDeepBlack,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = ArifDeepBlack,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ArifLogoBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val arif = LocalArifColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(arif.neonPrimary, arif.neonSecondary)),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_arif_launcher_fg),
            contentDescription = "ARIF Game Booster emblem",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun StatusPill(
    label: String,
    isActive: Boolean,
    activeColor: Color = NeonEmerald,
    inactiveColor: Color = TextSecondary
) {
    val color by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        label = "statusPillColor"
    )
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isActive) pulseAlpha else 0.7f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MicWaveformMeter(
    waveformBins: List<Float>,
    normalizedLevel: Float,
    isRunning: Boolean,
    isClipping: Boolean,
    modifier: Modifier = Modifier
) {
    val arif = LocalArifColors.current
    val animatedLevel by animateFloatAsState(
        targetValue = if (isRunning) normalizedLevel else 0f,
        animationSpec = tween(90),
        label = "micLevel"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ArifDeepBlack.copy(alpha = 0.75f))
                .border(
                    1.dp,
                    if (isClipping) arif.danger.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(14.dp)
                )
        ) {
            val count = waveformBins.size.coerceAtLeast(1)
            val horizontalPad = 14.dp.toPx()
            val usableWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
            val slotWidth = usableWidth / count
            val barWidth = (slotWidth * 0.58f).coerceAtLeast(3.dp.toPx())
            val maxBarHeight = size.height * 0.74f

            for (i in 0 until count) {
                val amp = if (isRunning) waveformBins[i].coerceIn(0.05f, 1f) else 0.08f
                val barHeight = maxBarHeight * amp
                val x = horizontalPad + i * slotWidth + (slotWidth - barWidth) / 2f
                val y = (size.height - barHeight) / 2f
                val barColor = when {
                    isClipping && amp > 0.8f -> arif.danger
                    i % 2 == 0 -> arif.neonPrimary
                    else -> arif.neonSecondary
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(barColor, barColor.copy(alpha = 0.35f))
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        // RMS Horizontal Level Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedLevel)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                arif.neonPrimary,
                                arif.neonSecondary,
                                if (isClipping) arif.danger else NeonEmerald
                            )
                        )
                    )
            )
        }
    }
}
