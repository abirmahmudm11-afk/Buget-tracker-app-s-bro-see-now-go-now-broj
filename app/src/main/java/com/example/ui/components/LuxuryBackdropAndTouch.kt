package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.SpendlyTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tactile 3D touch-animation modifier for all cards and interactive surfaces.
 * Uses PointerEventPass.Initial without consuming events so LazyColumn scrolling
 * and HorizontalPager swiping continue smoothly while every touch triggers
 * physical 3D tilt, spring press bounce, and a champagne-gold specular spotlight.
 */
fun Modifier.touchSpringCard(
    enabled: Boolean = true,
    pressScale: Float = 0.974f,
    maxTiltDeg: Float = 5.5f,
    hapticOnPress: Boolean = false,
): Modifier = composed {
    if (!enabled) return@composed this

    val semantic = SpendlyTheme.semantic
    val haptic = LocalHapticFeedback.current
    var isTouched by remember { mutableStateOf(false) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    val targetScale = if (isTouched) pressScale else 1f
    val targetGlowAlpha = if (isTouched) 1f else 0f

    val normX = if (cardSize.width > 0) ((pointerPos.x / cardSize.width.toFloat()) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f else 0f
    val normY = if (cardSize.height > 0) ((pointerPos.y / cardSize.height.toFloat()) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f else 0f

    val targetRotY = if (isTouched) normX * maxTiltDeg else 0f
    val targetRotX = if (isTouched) -normY * (maxTiltDeg * 0.72f) else 0f

    val animScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "touchCardScale",
    )
    val animRotX by animateFloatAsState(
        targetValue = targetRotX,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "touchCardRotX",
    )
    val animRotY by animateFloatAsState(
        targetValue = targetRotY,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "touchCardRotY",
    )
    val animGlow by animateFloatAsState(
        targetValue = targetGlowAlpha,
        animationSpec = tween(durationMillis = 160),
        label = "touchCardGlow",
    )

    val spotlightColor = if (semantic.isDark) {
        semantic.heroGold.copy(alpha = 0.14f * animGlow)
    } else {
        semantic.heroGold.copy(alpha = 0.12f * animGlow)
    }

    this
        .onSizeChanged { cardSize = it }
        .pointerInput(enabled, hapticOnPress) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                isTouched = true
                pointerPos = down.position
                if (hapticOnPress) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val active = event.changes.firstOrNull { it.pressed }
                    if (active != null) {
                        pointerPos = active.position
                    } else {
                        break
                    }
                }
                isTouched = false
            }
        }
        .graphicsLayer {
            cameraDistance = 16f * density
            scaleX = animScale
            scaleY = animScale
            rotationX = animRotX
            rotationY = animRotY
        }
        .drawWithContent {
            drawContent()
            if (animGlow > 0.01f && cardSize.width > 0 && cardSize.height > 0) {
                val center = if (pointerPos == Offset.Zero) {
                    Offset(size.width * 0.5f, size.height * 0.5f)
                } else {
                    pointerPos
                }
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(spotlightColor, Color.Transparent),
                        center = center,
                        radius = size.maxDimension * 0.58f,
                    ),
                )
            }
        }
}

/**
 * Full-screen bespoke architectural vault background layer behind cards & ledger surfaces.
 * Combines sculpted emerald/gold stone artwork with slow ambient aurora drift & guilloche arcs.
 */
@Composable
fun LuxuryVaultBackdrop(
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
) {
    val semantic = SpendlyTheme.semantic
    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = semantic.heroGold

    val infinite = rememberInfiniteTransition(label = "luxuryBackdropDrift")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "backdropPhase",
    )

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Image(
            painter = painterResource(id = R.drawable.img_luxury_vault_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.06f + sin(phase) * 0.02f
                    scaleY = 1.06f + cos(phase) * 0.02f
                    alpha = when {
                        semantic.isDark && prominent -> 0.42f
                        semantic.isDark -> 0.28f
                        prominent -> 0.20f
                        else -> 0.13f
                    }
                },
        )

        // Theme-adaptive silk scrim & living atmospheric champagne/spruce aurora
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (semantic.isDark) {
                        listOf(
                            bgColor.copy(alpha = if (prominent) 0.35f else 0.58f),
                            bgColor.copy(alpha = if (prominent) 0.62f else 0.80f),
                            bgColor.copy(alpha = if (prominent) 0.82f else 0.92f),
                        )
                    } else {
                        listOf(
                            bgColor.copy(alpha = if (prominent) 0.55f else 0.72f),
                            bgColor.copy(alpha = if (prominent) 0.78f else 0.86f),
                            bgColor.copy(alpha = if (prominent) 0.88f else 0.94f),
                        )
                    },
                ),
            )

            val driftX = sin(phase) * size.width * 0.06f
            val driftY = cos(phase * 0.8f) * size.height * 0.04f

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (semantic.isDark) 0.18f else 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.82f + driftX, size.height * 0.16f + driftY),
                    radius = size.maxDimension * 0.62f,
                ),
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldColor.copy(alpha = if (semantic.isDark) 0.14f else 0.11f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.18f - driftX, size.height * 0.52f - driftY),
                    radius = size.maxDimension * 0.54f,
                ),
            )

            // Fine architectural hairline arcs
            val arcTint = goldColor.copy(alpha = if (semantic.isDark) 0.10f else 0.12f)
            val arcCenter = Offset(size.width * 0.92f, size.height * 0.10f)
            drawCircle(
                color = arcTint,
                radius = size.minDimension * 0.55f,
                center = arcCenter,
                style = Stroke(width = 0.8.dp.toPx()),
            )
            drawCircle(
                color = arcTint,
                radius = size.minDimension * 0.82f,
                center = arcCenter,
                style = Stroke(width = 0.7.dp.toPx()),
            )
        }
    }
}

/**
 * Sculpted dark-spruce & champagne-gold stage positioned directly behind 3D Wallet Card decks.
 */
@Composable
fun WalletDeckStage(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val semantic = SpendlyTheme.semantic
    val infinite = rememberInfiniteTransition(label = "walletStageGlow")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stagePhase",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .touchSpringCard(pressScale = 0.99f, maxTiltDeg = 2.5f),
        shape = RoundedCornerShape(24.dp),
        color = semantic.heroSurface,
        border = BorderStroke(1.dp, semantic.heroGold.copy(alpha = 0.30f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle architectural vault sculpture texture behind the 3D cards
            Image(
                painter = painterResource(id = R.drawable.img_vault_hero_1795520877573),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .graphicsLayer { alpha = 0.24f },
            )

            // Radial champagne-gold & emerald rim halo behind the cards
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            semantic.heroSurfaceElevated.copy(alpha = 0.72f),
                            semantic.heroSurface.copy(alpha = 0.88f),
                        ),
                    ),
                )

                val haloX = size.width * (0.5f + sin(phase) * 0.14f)
                val haloY = size.height * (0.42f + cos(phase) * 0.08f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            semantic.heroGold.copy(alpha = 0.22f),
                            semantic.heroAccent.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(haloX, haloY),
                        radius = size.maxDimension * 0.58f,
                    ),
                )

                // Precision vault guilloche rings behind card
                val ringInk = semantic.heroGold.copy(alpha = 0.14f)
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(ringInk, size.height * 0.44f, center, style = Stroke(width = 0.8.dp.toPx()))
                drawCircle(ringInk, size.height * 0.66f, center, style = Stroke(width = 0.7.dp.toPx()))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                content = content,
            )
        }
    }
}
