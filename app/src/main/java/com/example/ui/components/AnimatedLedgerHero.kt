package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.util.MoneyFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlinx.coroutines.delay

private data class ReelHighlight(
    val title: String,
    val subtitle: String,
    val amount: String,
    val accent: Color,
    val icon: ImageVector,
)

@Composable
fun AnimatedLedgerHero(
    modifier: Modifier = Modifier,
    badgeLabel: String = "LIVE LEDGER MOTION",
) {
    val semantic = SpendlyTheme.semantic
    var isPlaying by remember { mutableStateOf(true) }
    var reelIndex by remember { mutableIntStateOf(0) }

    val reelItems = remember(semantic) {
        listOf(
            ReelHighlight(
                title = "Morning Cortado",
                subtitle = "Dining • Cash Pocket",
                amount = "-$4.80",
                accent = Color(0xFFFDA4AF),
                icon = Icons.Rounded.ArrowUpward,
            ),
            ReelHighlight(
                title = "Bi-weekly Payroll",
                subtitle = "Income • Everyday Checking",
                amount = "+$4,850.00",
                accent = semantic.heroAccent,
                icon = Icons.Rounded.ArrowDownward,
            ),
            ReelHighlight(
                title = "November Budget",
                subtitle = "68% Used • Safe Zone",
                amount = "$768 left",
                accent = semantic.heroGold,
                icon = Icons.Rounded.PieChart,
            ),
            ReelHighlight(
                title = "Cloud Ledger Sync",
                subtitle = "Encrypted • Real-time Backup",
                amount = "Synced",
                accent = semantic.heroAccent,
                icon = Icons.Rounded.CloudDone,
            ),
        )
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(2600L)
            reelIndex = (reelIndex + 1) % reelItems.size
        }
    }

    val infinite = rememberInfiniteTransition(label = "heroReel")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit",
    )
    val floatBob by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatBob",
    )
    val chartSweep by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chartSweep",
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val effectiveBob = if (isPlaying) floatBob else 0f
    val effectiveSweep = if (isPlaying) chartSweep else 0.88f
    val effectiveOrbit = if (isPlaying) orbitAngle else 45f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(236.dp),
        shape = RoundedCornerShape(SpendlyTokens.HeroRadius),
        shadowElevation = 8.dp,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(semantic.heroCardStart, semantic.heroCardMid, semantic.heroCardEnd),
                    )
                ),
        ) {
            // Orbiting aurora & self-drawing kinetic cash-flow curve
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rad = effectiveOrbit * (PI / 180.0)
                val orb1Center = Offset(
                    x = size.width * (0.80f + 0.08f * cos(rad).toFloat()),
                    y = size.height * (0.24f + 0.08f * sin(rad).toFloat()),
                )
                val orb2Center = Offset(
                    x = size.width * (0.16f - 0.06f * cos(rad).toFloat()),
                    y = size.height * (0.82f - 0.06f * sin(rad).toFloat()),
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(semantic.heroAccent.copy(alpha = 0.28f), Color.Transparent),
                        center = orb1Center,
                        radius = size.width * 0.42f,
                    ),
                    radius = size.width * 0.42f,
                    center = orb1Center,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(semantic.heroGold.copy(alpha = 0.22f), Color.Transparent),
                        center = orb2Center,
                        radius = size.width * 0.35f,
                    ),
                    radius = size.width * 0.35f,
                    center = orb2Center,
                )

                // Subtle grid rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.92f, size.height * 0.14f),
                    style = Stroke(width = 1.2.dp.toPx()),
                )

                // Self-drawing spline chart in the mid backdrop
                val startX = 18.dp.toPx()
                val endX = size.width - 18.dp.toPx()
                val w = endX - startX
                val baseY = size.height * 0.56f
                val fullPath = Path().apply {
                    moveTo(startX, baseY + 18.dp.toPx())
                    cubicTo(
                        startX + w * 0.18f, baseY - 24.dp.toPx(),
                        startX + w * 0.34f, baseY + 14.dp.toPx(),
                        startX + w * 0.52f, baseY - 12.dp.toPx(),
                    )
                    cubicTo(
                        startX + w * 0.68f, baseY - 34.dp.toPx(),
                        startX + w * 0.84f, baseY - 4.dp.toPx(),
                        endX, baseY - 30.dp.toPx(),
                    )
                }

                val measure = PathMeasure()
                measure.setPath(fullPath, false)
                val segPath = Path()
                val targetLen = measure.length * effectiveSweep
                measure.getSegment(0f, targetLen, segPath, true)

                drawPath(
                    path = fullPath,
                    color = Color.White.copy(alpha = 0.10f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )
                drawPath(
                    path = segPath,
                    brush = Brush.horizontalGradient(
                        listOf(semantic.heroGold, semantic.heroAccent),
                    ),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                )

                val tipPos = measure.getPosition(targetLen)
                drawCircle(
                    color = semantic.heroAccent.copy(alpha = 0.35f * pulseAlpha),
                    radius = 12.dp.toPx(),
                    center = tipPos,
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
                    center = tipPos,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Reel Control Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.28f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying) semantic.heroAccent.copy(alpha = pulseAlpha)
                                        else semantic.onHeroCardMuted
                                    ),
                            )
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.onHeroCard,
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.28f),
                    ) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause animation" else "Play animation",
                                tint = semantic.onHeroCard,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Floating Balance Preview Chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = effectiveBob
                            rotationX = effectiveBob * 0.45f
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.onHeroCardMuted,
                        )
                        Text(
                            text = "$16,580.45",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = semantic.onHeroCard,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.14f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(
                                text = "SAVINGS RATE",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.onHeroCardMuted,
                            )
                            Text(
                                text = "+64%",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = semantic.heroAccent,
                            )
                        }
                    }
                }

                // Animated Sliding Reel Pill
                AnimatedContent(
                    targetState = reelItems[reelIndex],
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn(tween(380)))
                            .togetherWith(slideOutVertically { -it / 2 } + fadeOut(tween(280)))
                    },
                    label = "reelCard",
                ) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.30f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(item.accent.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.accent,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = semantic.onHeroCard,
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semantic.onHeroCardMuted,
                                    )
                                }
                            }
                            Text(
                                text = item.amount,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = item.accent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMoneyText(
    amountMinor: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showPositivePlus: Boolean = false,
    hidden: Boolean = false,
    hiddenText: String = "••••••",
) {
    val animatedFloat by animateFloatAsState(
        targetValue = amountMinor.toFloat(),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "animatedMoney",
    )
    val displayText = if (hidden) {
        hiddenText
    } else {
        MoneyFormatter.formatMinor(
            amountMinor = animatedFloat.roundToLong(),
            currencyCode = currencyCode,
            showPositivePlus = showPositivePlus,
        )
    }
    Text(
        text = displayText,
        style = style,
        color = color,
        modifier = modifier,
    )
}
