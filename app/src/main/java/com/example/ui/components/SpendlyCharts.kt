package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.util.MoneyFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class CategoryChartSlice(
    val categoryId: String,
    val label: String,
    val amountMinor: Long,
    val color: Color,
    val iconKey: String = "",
)

data class MonthlyBarPoint(
    val label: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
)

/**
 * Interactive filled Category Pie Chart for the main Dashboard (HomeScreen).
 * Supports tapping directly on pie wedges or legend rows to pop out a slice and filter Activity.
 */
@Composable
fun CategoryPieChart(
    slices: List<CategoryChartSlice>,
    currencyCode: String,
    hideBalances: Boolean,
    onSliceInspect: (CategoryChartSlice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    val surfaceColor = MaterialTheme.colorScheme.surface
    val total = remember(slices) { slices.sumOf { it.amountMinor }.coerceAtLeast(1L) }
    var highlightedId by remember(slices) { mutableStateOf(slices.firstOrNull()?.categoryId) }
    val highlightedSlice = slices.firstOrNull { it.categoryId == highlightedId }

    var revealed by remember(slices.size) { mutableStateOf(false) }
    LaunchedEffect(slices) { revealed = true }

    val sweepFraction by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "pieReveal",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(216.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .semantics {
                        contentDescription = "Spending by category pie chart totaling ${MoneyFormatter.formatMinor(total, currencyCode)}"
                    }
                    .pointerInput(slices, total) {
                        detectTapGestures { tap ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = tap.x - cx
                            val dy = tap.y - cy
                            val radius = (size.width.coerceAtMost(size.height)) / 2f
                            if (hypot(dx, dy) <= radius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f
                                var acc = 0f
                                for (slice in slices) {
                                    val sweep = (slice.amountMinor.toFloat() / total.toFloat()) * 360f
                                    if (angle >= acc && angle <= acc + sweep) {
                                        highlightedId = slice.categoryId
                                        break
                                    }
                                    acc += sweep
                                }
                            }
                        }
                    },
            ) {
                val basePad = 12.dp.toPx()
                val pieDiameter = size.minDimension - basePad * 2f
                val pieSize = Size(pieDiameter, pieDiameter)
                var startAngle = -90f

                slices.forEach { slice ->
                    val ratio = slice.amountMinor.toFloat() / total.toFloat()
                    val fullSweep = (ratio * 360f).coerceAtLeast(3f)
                    val sweep = (fullSweep * sweepFraction).coerceAtLeast(0.5f)
                    val isSelected = highlightedId == slice.categoryId
                    val isDimmed = highlightedId != null && !isSelected

                    val explodeDist = if (isSelected) 7.dp.toPx() else 0f
                    val midRad = Math.toRadians((startAngle + fullSweep / 2f).toDouble())
                    val offsetX = (cos(midRad) * explodeDist).toFloat()
                    val offsetY = (sin(midRad) * explodeDist).toFloat()
                    val topLeft = Offset(basePad + offsetX, basePad + offsetY)

                    drawArc(
                        color = if (isDimmed) slice.color.copy(alpha = 0.72f) else slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = pieSize,
                    )

                    if (slices.size > 1) {
                        drawArc(
                            color = surfaceColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = topLeft,
                            size = pieSize,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }

                    startAngle += ratio * 360f * sweepFraction
                }
            }
        }

        // Focused Slice Banner
        if (highlightedSlice != null) {
            val pct = ((highlightedSlice.amountMinor.toFloat() / total.toFloat()) * 100f).toInt()
            Surface(
                onClick = { onSliceInspect(highlightedSlice) },
                modifier = Modifier.pressBounce(pressScale = 0.96f, hapticOnPress = true),
                shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.surfaceBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(highlightedSlice.color),
                        )
                        Column {
                            Text(
                                text = "${highlightedSlice.label} ($pct%)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Tap to view in Activity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(highlightedSlice.amountMinor, currencyCode),
                        style = MoneyRowStyle,
                        color = highlightedSlice.color,
                    )
                }
            }
        }

        // Compact Legend List
        Surface(
            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, semantic.surfaceBorder),
        ) {
            Column {
                slices.forEachIndexed { idx, slice ->
                    val ratio = (slice.amountMinor.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    val pct = (ratio * 100f).toInt()
                    val isSelected = highlightedId == slice.categoryId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .pressBounce(pressScale = 0.97f, hapticOnPress = true)
                            .clickable {
                                highlightedId = slice.categoryId
                                onSliceInspect(slice)
                            }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color),
                            )
                            Text(
                                text = slice.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(slice.amountMinor, currencyCode),
                                style = MoneyRowStyle,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    if (idx < slices.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    slices: List<CategoryChartSlice>,
    currencyCode: String,
    onSliceClick: (CategoryChartSlice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    val total = remember(slices) { slices.sumOf { it.amountMinor }.coerceAtLeast(1L) }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerLow
    var highlightedId by remember { mutableStateOf<String?>(null) }
    val highlightedSlice = slices.firstOrNull { it.categoryId == highlightedId }

    var chartRevealed by remember(slices.size) { mutableStateOf(false) }
    LaunchedEffect(slices) {
        chartRevealed = true
    }
    val sweepFraction by animateFloatAsState(
        targetValue = if (chartRevealed) 1f else 0f,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "donutReveal",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(188.dp)
                    .semantics {
                        contentDescription = "Spending breakdown chart totaling ${MoneyFormatter.formatMinor(total, currencyCode)}"
                    },
            ) {
                val strokeWidth = 20.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )

                var startAngle = -90f
                val gapDegrees = if (slices.size > 1) 4f else 0f
                slices.forEach { slice ->
                    val ratio = slice.amountMinor.toFloat() / total.toFloat()
                    val fullSweep = (ratio * 360f - gapDegrees).coerceAtLeast(3f)
                    val sweep = (fullSweep * sweepFraction).coerceAtLeast(0.5f)
                    val isDimmed = highlightedId != null && highlightedId != slice.categoryId
                    val isFocused = highlightedId == slice.categoryId
                    drawArc(
                        color = if (isDimmed) slice.color.copy(alpha = 0.28f) else slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isFocused) strokeWidth + 4.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Butt,
                        ),
                    )
                    startAngle += ratio * 360f * sweepFraction
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 38.dp),
            ) {
                Text(
                    text = highlightedSlice?.label ?: "Total spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = MoneyFormatter.formatMinor(highlightedSlice?.amountMinor ?: total, currencyCode),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (highlightedSlice != null) {
                    val pct = (highlightedSlice.amountMinor * 100f / total.toFloat()).toInt()
                    Text(
                        text = "$pct% of spend",
                        style = MaterialTheme.typography.labelSmall,
                        color = highlightedSlice.color,
                    )
                }
            }
        }

        // Single grouped inset legend list instead of individual heavy card frames
        Surface(
            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, semantic.surfaceBorder),
        ) {
            Column {
                slices.forEachIndexed { idx, slice ->
                    val ratio = (slice.amountMinor.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = if (chartRevealed) ratio else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "sliceBar",
                    )
                    val pct = (ratio * 100f).toInt()
                    val isSelected = highlightedId == slice.categoryId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                            )
                            .pressBounce(pressScale = 0.97f, hapticOnPress = true)
                            .clickable {
                                highlightedId = if (isSelected) null else slice.categoryId
                                onSliceClick(slice)
                            }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color),
                                )
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = MoneyFormatter.formatMinor(slice.amountMinor, currencyCode),
                                    style = MoneyRowStyle,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surface),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedRatio)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(slice.color),
                            )
                        }
                    }
                    if (idx < slices.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCashFlowBarChart(
    points: List<MonthlyBarPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    val gridColor = semantic.surfaceBorder
    val maxVal = remember(points) {
        points.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) }?.coerceAtLeast(1L) ?: 1L
    }
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

    var barsRevealed by remember(points.size) { mutableStateOf(false) }
    LaunchedEffect(points) {
        barsRevealed = true
    }
    val heightScale by animateFloatAsState(
        targetValue = if (barsRevealed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "barGrowth",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(semantic.income))
                Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(semantic.expense))
                Text("Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .semantics {
                    contentDescription = "Monthly income versus expense bar chart in $currencyCode"
                },
        ) {
            val steps = 3
            for (i in 0..steps) {
                val y = size.height * (i.toFloat() / steps.toFloat())
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (i < steps) dashEffect else null,
                )
            }

            if (points.isNotEmpty()) {
                val groupWidth = size.width / points.size
                val barWidth = (groupWidth * 0.20f).coerceIn(12.dp.toPx(), 24.dp.toPx())
                val gap = 6.dp.toPx()
                val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                points.forEachIndexed { idx, pt ->
                    val centerX = groupWidth * idx + groupWidth / 2f
                    val incH = (((pt.incomeMinor.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx())) * heightScale)
                        .coerceAtLeast(if (pt.incomeMinor > 0L) 6.dp.toPx() else 2.dp.toPx())
                    val expH = (((pt.expenseMinor.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx())) * heightScale)
                        .coerceAtLeast(if (pt.expenseMinor > 0L) 6.dp.toPx() else 2.dp.toPx())

                    drawRoundRect(
                        color = semantic.income,
                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incH),
                        size = Size(barWidth, incH),
                        cornerRadius = corner,
                    )
                    drawRoundRect(
                        color = semantic.expense,
                        topLeft = Offset(centerX + gap / 2f, size.height - expH),
                        size = Size(barWidth, expH),
                        cornerRadius = corner,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            points.forEach { pt ->
                val net = pt.incomeMinor - pt.expenseMinor
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = MoneyFormatter.formatMinor(net, currencyCode, showPositivePlus = true),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (net >= 0) semantic.income else semantic.expense,
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetRingGauge(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "budgetGauge",
    )

    Box(
        modifier = modifier.size(104.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(104.dp)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = (animatedProgress * 360f).coerceAtLeast(if (animatedProgress > 0f) 4f else 0f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
