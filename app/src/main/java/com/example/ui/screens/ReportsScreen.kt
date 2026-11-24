package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
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
    val savingsRatePct = if (totalIncome > 0L) {
        (((totalIncome - totalExpenses).toDouble() / totalIncome.toDouble()) * 100.0).toInt()
    } else 0

    // Harmonized Warm Editorial Chart Palette
    val chartColors = listOf(
        Color(0xFF0B4F3E), // Deep Forest Emerald
        Color(0xFFC27803), // Warm Ochre Gold
        Color(0xFF0E7490), // Deep Lagoon Teal
        Color(0xFFC92A3E), // Editorial Crimson
        Color(0xFF178962), // Fresh Sage
        Color(0xFF6B46C1), // Plum
        Color(0xFFEA580C), // Terracotta
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text("Reports & Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reports_back")) {
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
                                        "${DateFormatters.formatIsoDate(tx.dateMillis)},${tx.type.label},\"${tx.description}\",\"${tx.merchant}\",\"$cat\",\"$acc\",${tx.amountMinor / 100.0},${tx.currencyCode}"
                                    )
                                }
                            }
                            val file = File(context.cacheDir, "spendly_report_${uiState.reportPeriod.name.lowercase()}.csv")
                            file.writeText(csv)
                            exportedFileSummary = "Created ${file.name} (${periodTransactions.size} rows, ${file.length()} bytes) in app cache."
                        },
                        modifier = Modifier.padding(end = 12.dp).testTag("reports_export_button"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = "Export report", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Export", style = MaterialTheme.typography.labelLarge)
                        }
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Period Selector
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
                                colors = chipColors,
                                modifier = Modifier.testTag("report_period_${rp.name.lowercase()}"),
                            )
                        }
                    }
                }

                // KPI Summary Grid
                item {
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ReportKpiTile(
                                    label = "Income",
                                    value = if (hide) "••••" else MoneyFormatter.formatMinor(totalIncome, baseCode),
                                    color = semantic.income,
                                    bg = semantic.incomeContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.weight(1f),
                                )
                                ReportKpiTile(
                                    label = "Expenses",
                                    value = if (hide) "••••" else MoneyFormatter.formatMinor(totalExpenses, baseCode),
                                    color = semantic.expense,
                                    bg = semantic.expenseContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ReportKpiTile(
                                    label = "Net Flow",
                                    value = if (hide) "••••" else MoneyFormatter.formatMinor(netCashFlow, baseCode, showPositivePlus = true),
                                    color = if (netCashFlow >= 0) semantic.income else semantic.expense,
                                    bg = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.weight(1f),
                                )
                                ReportKpiTile(
                                    label = "Savings Rate",
                                    value = if (totalIncome > 0L) "$savingsRatePct%" else "—",
                                    color = MaterialTheme.colorScheme.primary,
                                    bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
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
                    if (categorySlices.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column {
                                        Text("Category Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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

                    item {
                        Surface(
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("3-Month Cash Flow Trend", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                MonthlyCashFlowBarChart(
                                    points = trendPoints,
                                    currencyCode = baseCode,
                                )
                            }
                        }
                    }

                    if (accountSpendings.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text("Account-Level Spending", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                                                    .height(7.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(ratio)
                                                        .height(7.dp)
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
                            Surface(
                                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text("Budget Performance (${DateFormatters.formatShortMonth(currentPeriod)})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
private fun ReportKpiTile(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bg,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
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
