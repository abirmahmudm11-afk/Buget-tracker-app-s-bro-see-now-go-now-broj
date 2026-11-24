package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.util.MoneyFormatter

data class CategoryChartSlice(
    val categoryId: String,
    val label: String,
    val amountMinor: Long,
    val color: Color,
)

data class MonthlyBarPoint(
    val label: String,
    val incomeMinor: Long,
    val expenseMinor: Long,
)

@Composable
fun CategoryDonutChart(
    slices: List<CategoryChartSlice>,
    currencyCode: String,
    onSliceClick: (CategoryChartSlice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.amountMinor }.coerceAtLeast(1L)
    val trackColor = MaterialTheme.colorScheme.surfaceContainer
    var highlightedId by remember { mutableStateOf<String?>(null) }
    val highlightedSlice = slices.firstOrNull { it.categoryId == highlightedId }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(206.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(188.dp)
                    .semantics {
                        contentDescription = "Spending breakdown chart totaling ${MoneyFormatter.formatMinor(total, currencyCode)}"
                    },
            ) {
                val strokeWidth = 24.dp.toPx()
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
                    val sweep = (ratio * 360f - gapDegrees).coerceAtLeast(3f)
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
                            cap = StrokeCap.Round,
                        ),
                    )
                    startAngle += ratio * 360f
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 36.dp),
            ) {
                Text(
                    text = highlightedSlice?.label?.uppercase() ?: "TOTAL SPENT",
                    style = MaterialTheme.typography.labelSmall,
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

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEach { slice ->
                val ratio = (slice.amountMinor.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                val pct = (ratio * 100f).toInt()
                val isSelected = highlightedId == slice.categoryId
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            highlightedId = if (isSelected) null else slice.categoryId
                            onSliceClick(slice)
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = slice.color.copy(alpha = 0.14f),
                                ) {
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = slice.color,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    )
                                }
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
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(slice.color),
                            )
                        }
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxVal = points.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) }?.coerceAtLeast(1L) ?: 1L

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
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
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
                val barWidth = (groupWidth * 0.22f).coerceIn(12.dp.toPx(), 24.dp.toPx())
                val gap = 6.dp.toPx()

                points.forEachIndexed { idx, pt ->
                    val centerX = groupWidth * idx + groupWidth / 2f
                    val incH = ((pt.incomeMinor.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx()))
                        .coerceAtLeast(if (pt.incomeMinor > 0L) 6.dp.toPx() else 2.dp.toPx())
                    val expH = ((pt.expenseMinor.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx()))
                        .coerceAtLeast(if (pt.expenseMinor > 0L) 6.dp.toPx() else 2.dp.toPx())

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(semantic.income, semantic.income.copy(alpha = 0.72f))
                        ),
                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incH),
                        size = Size(barWidth, incH),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(semantic.expense, semantic.expense.copy(alpha = 0.72f))
                        ),
                        topLeft = Offset(centerX + gap / 2f, size.height - expH),
                        size = Size(barWidth, expH),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
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
        modifier = modifier.size(114.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(114.dp)) {
            val strokeWidth = 11.dp.toPx()
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
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
