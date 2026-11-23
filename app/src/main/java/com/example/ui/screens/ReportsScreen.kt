package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MonthPeriod
import com.example.model.ReportPeriod
import com.example.model.TransactionType
import com.example.ui.components.CategoryChartSlice
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.MonthlyBarPoint
import com.example.ui.components.MonthlyCashFlowBarChart
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
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

    val chartColors = listOf(
        Color(0xFF2563EB),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEC4899),
        Color(0xFF7C3AED),
        Color(0xFF06B6D4),
        Color(0xFFEF4444),
    )

    val categorySlices = remember(periodTransactions, uiState.categories, baseCode) {
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reports_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            val csv = buildString {
                                appendLine("Date,Type,Description,Merchant,Category,Account,Amount,Currency")
                                periodTransactions.forEach { tx ->
                                    val cat = uiState.categoryById(tx.categoryId)?.name ?: ""
                                    val acc = uiState.accountById(tx.accountId)?.name ?: ""
                                    appendLine(
                                        "${DateFormatters.formatIsoDate(tx.dateMillis)},${tx.type.label},\"${tx.description}\",\"${tx.merchant}\",\"$cat\",\"$acc\",${tx.amountMinor / 100.0},${tx.currencyCode}"
                                    )
                                }
                            }
                            val file = File(context.cacheDir, "spendly_report_${uiState.reportPeriod.name.lowercase()}.csv")
                            file.writeText(csv)
                            exportedFileSummary = "Created ${file.name} (${periodTransactions.size} rows, ${file.length()} bytes) in app cache."
                        },
                        modifier = Modifier.padding(end = 12.dp).testTag("reports_export_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Export report")
                        Spacer(Modifier.padding(2.dp))
                        Text("Export")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxSize()
                    .testTag("reports_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Period Selector
                item {
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
                                modifier = Modifier.testTag("report_period_${rp.name.lowercase()}"),
                            )
                        }
                    }
                }

                // KPI Summary + Comparison
                item {
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ReportKpiBlock(
                                    label = "Income",
                                    value = if (hide) "••••" else MoneyFormatter.formatMinor(totalIncome, baseCode),
                                    color = semantic.income,
                                    modifier = Modifier.weight(1f),
                                )
                                ReportKpiBlock(
                                    label = "Expenses",
                                    value = if (hide) "••••" else MoneyFormatter.formatMinor(totalExpenses, baseCode),
                                    color = semantic.expense,
                                    modifier = Modifier.weight(1f),
                                )
                                ReportKpiBlock(
                                    label = "Net Flow",
                                    value = if (hide) "••••" else MoneyFormatter.formatMinor(netCashFlow, baseCode, showPositivePlus = true),
                                    color = if (netCashFlow >= 0) semantic.income else semantic.expense,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                            )
                        }
                    }
                }

                if (periodTransactions.isEmpty()) {
                    item {
                        EmptyLedgerState(
                            title = "No analytical data for this period",
                            subtitle = "Charts and category breakdowns automatically appear once you record transactions.",
                            actionLabel = "Add Transaction",
                            onActionClick = onAddTransaction,
                        )
                    }
                } else {
                    // Category Breakdown Donut
                    if (categorySlices.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column {
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
                    }

                    // 3-Month Trend Bar Chart
                    item {
                        Surface(
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("3-Month Cash Flow Trend", style = MaterialTheme.typography.titleMedium)
                                MonthlyCashFlowBarChart(
                                    points = trendPoints,
                                    currencyCode = baseCode,
                                )
                            }
                        }
                    }

                    // Account-Level Spending
                    if (accountSpendings.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text("Account-Level Spending", style = MaterialTheme.typography.titleMedium)
                                    val maxAccountSpend = accountSpendings.maxOf { it.second }.coerceAtLeast(1L)
                                    accountSpendings.forEach { (accName, spent) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(accName, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    text = if (hide) "••••" else MoneyFormatter.formatMinor(spent, baseCode),
                                                    style = MoneyRowStyle,
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { (spent.toFloat() / maxAccountSpend.toFloat()).coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Budget Performance
                    uiState.overallBudgetSummary?.let { bSum ->
                        item {
                            Surface(
                                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("Budget Performance (${DateFormatters.formatShortMonth(currentPeriod)})", style = MaterialTheme.typography.titleMedium)
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
                Button(onClick = { exportedFileSummary = null }) {
                    Text("Done")
                }
            },
        )
    }
}

@Composable
private fun ReportKpiBlock(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
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
