package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ArifCyberBackground
import com.example.ui.theme.LocalArifColors
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val arif = LocalArifColors.current
    var started by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.72f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "splashLogoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(750),
        label = "splashAlpha"
    )

    val ringPulse = rememberInfiniteTransition(label = "splashRingPulse")
    val ringScale by ringPulse.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(1750L)
        onFinished()
    }

    ArifCyberBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("splash_screen")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFinished
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(contentAlpha)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseRadius = size.minDimension * 0.42f * ringScale
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    arif.neonPrimary,
                                    arif.neonSecondary,
                                    arif.neonPrimary
                                )
                            ),
                            radius = baseRadius,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = arif.neonPrimary.copy(alpha = 0.25f),
                            radius = baseRadius * 1.15f,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.img_arif_launcher_fg),
                        contentDescription = "ARIF Game Booster Logo",
                        modifier = Modifier
                            .size(116.dp)
                            .scale(logoScale)
                            .clip(RoundedCornerShape(28.dp))
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(arif.neonPrimary, arif.neonSecondary)),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.brand_title),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    arif.neonPrimary.copy(alpha = 0.2f),
                                    arif.neonSecondary.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            arif.neonPrimary.copy(alpha = 0.55f),
                            CircleShape
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.brand_edition),
                        style = MaterialTheme.typography.labelLarge,
                        color = arif.neonPrimary,
                        letterSpacing = 2.4.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
