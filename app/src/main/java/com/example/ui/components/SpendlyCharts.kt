package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(174.dp)
                    .semantics {
                        contentDescription = "Spending breakdown chart totaling ${MoneyFormatter.formatMinor(total, currencyCode)}"
                    },
            ) {
                val strokeWidth = 26.dp.toPx()
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
                val gapDegrees = if (slices.size > 1) 3f else 0f
                slices.forEach { slice ->
                    val ratio = slice.amountMinor.toFloat() / total.toFloat()
                    val sweep = (ratio * 360f - gapDegrees).coerceAtLeast(2f)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                    startAngle += ratio * 360f
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TOTAL SPENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = MoneyFormatter.formatMinor(total, currencyCode),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEach { slice ->
                val pct = (slice.amountMinor * 100f / total.toFloat()).toInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSliceClick(slice) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                .size(12.dp)
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
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = MoneyFormatter.formatMinor(slice.amountMinor, currencyCode),
                            style = MoneyRowStyle,
                            color = MaterialTheme.colorScheme.onSurface,
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxVal = points.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) }?.coerceAtLeast(1L) ?: 1L

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(semantic.income))
                Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(semantic.expense))
                Text("Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
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
                )
            }

            if (points.isNotEmpty()) {
                val groupWidth = size.width / points.size
                val barWidth = (groupWidth * 0.24f).coerceIn(10.dp.toPx(), 24.dp.toPx())
                val gap = 6.dp.toPx()

                points.forEachIndexed { idx, pt ->
                    val centerX = groupWidth * idx + groupWidth / 2f
                    val incH = (pt.incomeMinor.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx())
                    val expH = (pt.expenseMinor.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx())

                    drawRoundRect(
                        color = semantic.income,
                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incH),
                        size = Size(barWidth, incH.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                    drawRoundRect(
                        color = semantic.expense,
                        topLeft = Offset(centerX + gap / 2f, size.height - expH),
                        size = Size(barWidth, expH.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            points.forEach { pt ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = MoneyFormatter.formatMinor(pt.expenseMinor, currencyCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
