package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MonthPeriod
import com.example.model.ReportPeriod
import com.example.model.TransactionType
import com.example.ui.components.CategoryChartSlice
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.MonthlyBarPoint
import com.example.ui.components.MonthlyCashFlowBarChart
import com.example.ui.components.pressBounce
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.util.CsvUtils
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    uiState: SpendlyUiState,
    onNavigateBack: () -> Unit,
    onSelectReportPeriod: (ReportPeriod) -> Unit,
    onOpenCategoryInActivity: (String) -> Unit,
    onAddTransaction: () -> Unit,
) {
    val context = LocalContext.current
    var exportedFileSummary by remember { mutableStateOf<String?>(null) }

    val baseCode = uiState.defaultCurrencyCode
    val semantic = SpendlyTheme.semantic
    val hide = uiState.privacyHideBalances

    val currentPeriod = uiState.selectedPeriod
    val includedPeriods = remember(currentPeriod, uiState.reportPeriod) {
        when (uiState.reportPeriod) {
            ReportPeriod.THIS_MONTH -> listOf(currentPeriod)
            ReportPeriod.LAST_MONTH -> listOf(previousMonth(currentPeriod, 1))
            ReportPeriod.LAST_3_MONTHS -> listOf(
                previousMonth(currentPeriod, 2),
                previousMonth(currentPeriod, 1),
                currentPeriod,
            )
        }
    }

    val periodTransactions = remember(uiState.transactions, includedPeriods) {
        uiState.transactions.filter { DateFormatters.monthPeriodOf(it.dateMillis) in includedPeriods }
    }

    val prevComparisonPeriod = remember(currentPeriod, uiState.reportPeriod) {
        when (uiState.reportPeriod) {
            ReportPeriod.THIS_MONTH -> previousMonth(currentPeriod, 1)
            ReportPeriod.LAST_MONTH -> previousMonth(currentPeriod, 2)
            ReportPeriod.LAST_3_MONTHS -> previousMonth(currentPeriod, 3)
        }
    }

    val prevPeriodExpenses = remember(uiState.transactions, prevComparisonPeriod) {
        uiState.transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    DateFormatters.monthPeriodOf(it.dateMillis) == prevComparisonPeriod
            }
            .sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
    }

    val totalExpenses = remember(periodTransactions, baseCode) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
    }
    val totalIncome = remember(periodTransactions, baseCode) {
        periodTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
    }
    val netCashFlow = totalIncome - totalExpenses
    val savingsRatePct = if (totalIncome > 0L) {
        (((totalIncome - totalExpenses).toDouble() / totalIncome.toDouble()) * 100.0).toInt()
    } else 0

    // High-contrast, distinct Nordic editorial palette
    val chartColors = if (semantic.isDark) {
        listOf(
            Color(0xFF4CC2A5),
            Color(0xFF6AB3E8),
            Color(0xFFF27C89),
            Color(0xFFEBB04D),
            Color(0xFFB4A7EB),
            Color(0xFF67C9D6),
            Color(0xFFF09A6A),
        )
    } else {
        listOf(
            Color(0xFF0F5C4D),
            Color(0xFF1D5A8A),
            Color(0xFFB42334),
            Color(0xFF9A5B00),
            Color(0xFF4F4698),
            Color(0xFF156778),
            Color(0xFFB54718),
        )
    }

    val categorySlices = remember(periodTransactions, uiState.categories, baseCode, chartColors) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId ?: "uncategorized" }
            .entries
            .map { (catId, txs) ->
                val sum = txs.sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
                val label = uiState.categoryById(catId)?.name ?: "Other"
                Triple(catId, label, sum)
            }
            .filter { it.third > 0L }
            .sortedByDescending { it.third }
            .mapIndexed { idx, (catId, label, sum) ->
                CategoryChartSlice(
                    categoryId = catId,
                    label = label,
                    amountMinor = sum,
                    color = chartColors[idx % chartColors.size],
                )
            }
    }

    val trendPoints = remember(uiState.transactions, currentPeriod, baseCode) {
        listOf(
            previousMonth(currentPeriod, 2),
            previousMonth(currentPeriod, 1),
            currentPeriod,
        ).map { p ->
            val txs = uiState.transactions.filter { DateFormatters.monthPeriodOf(it.dateMillis) == p }
            val inc = txs.filter { it.type == TransactionType.INCOME }
                .sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
            val exp = txs.filter { it.type == TransactionType.EXPENSE }
                .sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
            MonthlyBarPoint(
                label = DateFormatters.formatShortMonth(p),
                incomeMinor = inc,
                expenseMinor = exp,
            )
        }
    }

    val accountSpendings = remember(periodTransactions, uiState.accounts, baseCode) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.accountId }
            .mapNotNull { (accId, txs) ->
                val acc = uiState.accountById(accId) ?: return@mapNotNull null
                val spent = txs.sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
                acc.name to spent
            }
            .sortedByDescending { it.second }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        "Reports & Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pressBounce().testTag("reports_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        onClick = {
                            val csv = buildString {
                                appendLine("Date,Type,Description,Merchant,Category,Account,Amount,Currency")
                                periodTransactions.forEach { tx ->
                                    val cat = uiState.categoryById(tx.categoryId)?.name ?: ""
                                    val acc = uiState.accountById(tx.accountId)?.name ?: ""
                                    appendLine(
                                        listOf(
                                            DateFormatters.formatIsoDate(tx.dateMillis),
                                            tx.type.label,
                                            CsvUtils.escapeCsvField(tx.description),
                                            CsvUtils.escapeCsvField(tx.merchant),
                                            CsvUtils.escapeCsvField(cat),
                                            CsvUtils.escapeCsvField(acc),
                                            CsvUtils.formatMinorForCsv(tx.amountMinor, tx.currencyCode),
                                            tx.currencyCode,
                                        ).joinToString(",")
                                    )
                                }
                            }
                            val file = File(context.cacheDir, "spendly_report_${uiState.reportPeriod.name.lowercase()}.csv")
                            file.writeText(csv)
                            exportedFileSummary = "Created ${file.name} (${periodTransactions.size} rows, ${file.length()} bytes) in app cache."
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .minimumInteractiveComponentSize()
                            .pressBounce(pressScale = 0.93f)
                            .testTag("reports_export_button"),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                Icons.Rounded.FileDownload,
                                contentDescription = "Export report",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text("Export", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWide = maxWidth >= 720.dp
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxSize()
                    .testTag("reports_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReportPeriod.entries.forEach { rp ->
                            FilterChip(
                                selected = uiState.reportPeriod == rp,
                                onClick = { onSelectReportPeriod(rp) },
                                label = { Text(rp.label) },
                                shape = RoundedCornerShape(999.dp),
                                colors = chipColors,
                                modifier = Modifier.pressBounce(pressScale = 0.94f).testTag("report_period_${rp.name.lowercase()}"),
                            )
                        }
                    }
                }

                // Clean Grouped 2x2 Summary Sheet
                item {
                    LedgerGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                        ) {
                            ReportKpiCell(
                                label = "Income",
                                value = if (hide) "••••" else MoneyFormatter.formatMinor(totalIncome, baseCode),
                                valueColor = semantic.income,
                                modifier = Modifier.weight(1f),
                            )
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            ReportKpiCell(
                                label = "Expenses",
                                value = if (hide) "••••" else MoneyFormatter.formatMinor(totalExpenses, baseCode),
                                valueColor = semantic.expense,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                        ) {
                            ReportKpiCell(
                                label = "Net Flow",
                                value = if (hide) "••••" else MoneyFormatter.formatMinor(netCashFlow, baseCode, showPositivePlus = true),
                                valueColor = if (netCashFlow >= 0) semantic.income else semantic.expense,
                                modifier = Modifier.weight(1f),
                            )
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            ReportKpiCell(
                                label = "Savings Rate",
                                value = if (totalIncome > 0L) "$savingsRatePct%" else "—",
                                valueColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        val deltaText = if (prevPeriodExpenses > 0L) {
                            val pct = (((totalExpenses - prevPeriodExpenses).toDouble() / prevPeriodExpenses.toDouble()) * 100.0).toInt()
                            val dir = if (pct >= 0) "+$pct%" else "$pct%"
                            "Spending vs ${DateFormatters.formatShortMonth(prevComparisonPeriod)}: $dir (${MoneyFormatter.formatMinor(prevPeriodExpenses, baseCode)})"
                        } else {
                            "No prior-period expense baseline available for comparison."
                        }
                        Text(
                            text = deltaText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }

                if (periodTransactions.isEmpty()) {
                    item {
                        EmptyLedgerState(
                            title = "No data for this period",
                            subtitle = "Charts and category breakdowns appear once you record transactions.",
                            actionLabel = "Add Transaction",
                            onActionClick = onAddTransaction,
                        )
                    }
                } else {
                    item {
                        val categoryCard: @Composable (Modifier) -> Unit = { cardModifier ->
                            LedgerGroupCard(modifier = cardModifier) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Tap any category to inspect matching transactions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    CategoryDonutChart(
                                        slices = categorySlices,
                                        currencyCode = baseCode,
                                        onSliceClick = { onOpenCategoryInActivity(it.categoryId) },
                                    )
                                }
                            }
                        }

                        val trendCard: @Composable (Modifier) -> Unit = { cardModifier ->
                            LedgerGroupCard(modifier = cardModifier) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text("3-Month Cash Flow", style = MaterialTheme.typography.titleMedium)
                                    MonthlyCashFlowBarChart(
                                        points = trendPoints,
                                        currencyCode = baseCode,
                                    )
                                }
                            }
                        }

                        if (isWide && categorySlices.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                categoryCard(Modifier.weight(1f))
                                trendCard(Modifier.weight(1f))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (categorySlices.isNotEmpty()) {
                                    categoryCard(Modifier.fillMaxWidth())
                                }
                                trendCard(Modifier.fillMaxWidth())
                            }
                        }
                    }

                    if (accountSpendings.isNotEmpty()) {
                        item {
                            LedgerGroupCard {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text("Spending by Account", style = MaterialTheme.typography.titleMedium)
                                    val maxAccountSpend = accountSpendings.maxOf { it.second }.coerceAtLeast(1L)
                                    accountSpendings.forEach { (accName, spent) ->
                                        val ratio = (spent.toFloat() / maxAccountSpend.toFloat()).coerceIn(0f, 1f)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(accName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                                Text(
                                                    text = if (hide) "••••" else MoneyFormatter.formatMinor(spent, baseCode),
                                                    style = MoneyRowStyle,
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(ratio)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(MaterialTheme.colorScheme.primary),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    uiState.overallBudgetSummary?.let { bSum ->
                        item {
                            LedgerGroupCard {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = "Budget Performance (${DateFormatters.formatShortMonth(currentPeriod)})",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "Used ${(bSum.progress * 100).toInt()}% of ${MoneyFormatter.formatMinor(bSum.budget.limitMinor, bSum.budget.currencyCode)} monthly limit.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    exportedFileSummary?.let { msg ->
        AlertDialog(
            onDismissRequest = { exportedFileSummary = null },
            title = { Text("Export Created") },
            text = { Text(msg) },
            confirmButton = {
                Button(
                    onClick = { exportedFileSummary = null },
                    modifier = Modifier.pressBounce(pressScale = 0.94f),
                ) {
                    Text("Done")
                }
            },
        )
    }
}

@Composable
private fun ReportKpiCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum",
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun previousMonth(period: MonthPeriod, minusMonths: Int): MonthPeriod {
    var y = period.year
    var m = period.month - minusMonths
    while (m <= 0) {
        m += 12
        y -= 1
    }
    return MonthPeriod(y, m)
}
