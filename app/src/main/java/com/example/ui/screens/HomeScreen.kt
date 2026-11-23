package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AccountSummary
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.MonthPickerSheet
import com.example.ui.components.MonthSelectorChip
import com.example.ui.components.SpendlyBrandLogo
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.components.accountGradientColors
import com.example.ui.components.accountTypeIcon
import com.example.ui.components.categoryIconFor
import com.example.ui.theme.MoneyHeroStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.categoryColorPair
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState

@Composable
fun HomeScreen(
    uiState: SpendlyUiState,
    onSelectPeriod: (com.example.model.MonthPeriod) -> Unit,
    onToggleTheme: (ThemeMode) -> Unit,
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onManageAccounts: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAccountDetail: (String) -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenTransactionDetail: (TransactionRecord) -> Unit,
) {
    var showMonthDialog by remember { mutableStateOf(false) }
    val semantic = SpendlyTheme.semantic
    val baseCode = uiState.defaultCurrencyCode
    val hide = uiState.privacyHideBalances

    val recentTx = remember(uiState.monthlyTransactions) {
        uiState.monthlyTransactions.sortedByDescending { it.dateMillis }.take(5)
    }

    val topCategories = remember(uiState.monthlyTransactions, uiState.categories, baseCode) {
        uiState.monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .entries
            .mapNotNull { (catId, txs) ->
                val cat = uiState.categoryById(catId) ?: return@mapNotNull null
                val total = txs.sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
                cat to total
            }
            .sortedByDescending { it.second }
            .take(4)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Bar Area
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpendlyBrandLogo()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = {
                                    val next = if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                    onToggleTheme(next)
                                },
                                modifier = Modifier.testTag("home_theme_toggle"),
                            ) {
                                Icon(
                                    imageVector = if (uiState.themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Toggle theme appearance",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("home_settings_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Profile and settings",
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hello, ${uiState.profileName.substringBefore(" ")}!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (uiState.isPreviewDataActive) {
                                    "Manual Ledger • Interactive UI Preview"
                                } else {
                                    "Manual Ledger • Local-first tracking"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        MonthSelectorChip(
                            period = uiState.selectedPeriod,
                            onClick = { showMonthDialog = true },
                        )
                    }
                }
            }

            // Vivid 3-Stop Gradient Hero Summary Card
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SpendlyTokens.CardRadius))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    semantic.heroCardStart,
                                    semantic.heroCardMid,
                                    semantic.heroCardEnd,
                                )
                            )
                        ),
                ) {
                    // Subtle geometric highlight orb
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 36.dp, y = (-36).dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.09f)),
                    )

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "NET CASH FLOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.onHeroCardMuted,
                                )
                                Text(
                                    text = if (hide) "••••••" else MoneyFormatter.formatMinor(
                                        uiState.monthlyNetCashFlowBaseMinor,
                                        baseCode,
                                        showPositivePlus = true,
                                    ),
                                    style = MoneyHeroStyle,
                                    color = semantic.onHeroCard,
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.18f),
                            ) {
                                Text(
                                    text = baseCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = semantic.onHeroCard,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            HeroMetricTile(
                                label = "Income",
                                value = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.monthlyIncomeBaseMinor, baseCode),
                                icon = Icons.Rounded.ArrowDownward,
                                badgeColor = Color(0xFF34D399),
                                modifier = Modifier.weight(1f),
                            )
                            HeroMetricTile(
                                label = "Expenses",
                                value = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.monthlyExpensesBaseMinor, baseCode),
                                icon = Icons.Rounded.ArrowUpward,
                                badgeColor = Color(0xFFFCA5A5),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        val remainingBudget = uiState.remainingOverallBudgetBaseMinor
                        Surface(
                            onClick = onOpenBudgets,
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_remaining_budget_pill"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PieChart,
                                        contentDescription = null,
                                        tint = semantic.onHeroCard,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = if (remainingBudget != null) "Remaining Monthly Budget" else "Set Monthly Budget",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = semantic.onHeroCard,
                                    )
                                }
                                Text(
                                    text = when {
                                        remainingBudget == null -> "Create →"
                                        hide -> "••••"
                                        remainingBudget < 0 -> "Over ${MoneyFormatter.formatMinor(-remainingBudget, baseCode)}"
                                        else -> MoneyFormatter.formatMinor(remainingBudget, baseCode)
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = semantic.onHeroCard,
                                )
                            }
                        }
                    }
                }
            }

            // Colorful 4-Tile Quick Actions Row
            item {
                Row(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ColorfulQuickActionTile(
                        label = "Expense",
                        icon = Icons.Rounded.ArrowUpward,
                        tint = semantic.expense,
                        bg = semantic.expenseContainer,
                        onClick = onAddExpense,
                        testTag = "home_add_expense",
                        modifier = Modifier.weight(1f),
                    )
                    ColorfulQuickActionTile(
                        label = "Income",
                        icon = Icons.Rounded.ArrowDownward,
                        tint = semantic.income,
                        bg = semantic.incomeContainer,
                        onClick = onAddIncome,
                        testTag = "home_add_income",
                        modifier = Modifier.weight(1f),
                    )
                    ColorfulQuickActionTile(
                        label = "Transfer",
                        icon = Icons.AutoMirrored.Rounded.CompareArrows,
                        tint = semantic.transfer,
                        bg = semantic.transferContainer,
                        onClick = onAddTransfer,
                        testTag = "home_add_transfer",
                        modifier = Modifier.weight(1f),
                    )
                    ColorfulQuickActionTile(
                        label = "Reports",
                        icon = Icons.Rounded.BarChart,
                        tint = semantic.violet,
                        bg = semantic.violetContainer,
                        onClick = onOpenReports,
                        testTag = "home_open_reports",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Colorful Top Spending Categories Strip
            if (topCategories.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionHeader(
                            title = "Top Categories",
                            actionLabel = "Analytics",
                            onAction = onOpenReports,
                            testTag = "home_categories_reports",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            topCategories.forEach { (cat, totalMinor) ->
                                val (tint, bg) = categoryColorPair(cat.iconKey)
                                Surface(
                                    onClick = onOpenBudgets,
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    shadowElevation = 1.dp,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(bg),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = categoryIconFor(cat.iconKey),
                                                contentDescription = cat.name,
                                                tint = tint,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = if (hide) "••••" else MoneyFormatter.formatMinor(totalMinor, baseCode),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Budget Health Section
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Monthly Budget",
                        actionLabel = if (uiState.overallBudgetSummary != null) "Manage" else "Create",
                        onAction = onOpenBudgets,
                        testTag = "home_budgets_header_action",
                    )

                    val overall = uiState.overallBudgetSummary
                    if (overall != null) {
                        BudgetProgressCard(
                            summary = overall,
                            hideBalances = hide,
                            onClick = onOpenBudgets,
                        )
                    } else {
                        Surface(
                            onClick = onOpenBudgets,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_create_budget_cta"),
                            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Set a monthly spending limit", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "No budget is configured for this month yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Create budget",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // Colorful Gradient Accounts Carousel Section
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Accounts (${uiState.activeAccounts.size} active)",
                        actionLabel = "Manage Accounts",
                        onAction = onManageAccounts,
                        testTag = "home_manage_accounts",
                    )

                    if (uiState.activeAccounts.isEmpty()) {
                        Surface(
                            onClick = onManageAccounts,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Add your first account or wallet", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "Track bank accounts, cards, and cash wallets manually.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(Icons.Rounded.AccountBalance, contentDescription = "Add account", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = semantic.incomeContainer.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, semantic.income.copy(alpha = 0.25f)),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("CALCULATED ASSETS", style = MaterialTheme.typography.labelSmall, color = semantic.income)
                                    Text(
                                        text = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.totalAssetsBaseMinor, baseCode),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = semantic.violetContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, semantic.violet.copy(alpha = 0.25f)),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("CARD OUTSTANDING", style = MaterialTheme.typography.labelSmall, color = semantic.violet)
                                    Text(
                                        text = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.totalCreditOutstandingBaseMinor, baseCode),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        val activeSummaries = uiState.accountSummaries.filter { !it.account.isArchived }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            activeSummaries.forEach { summary ->
                                GradientAccountCardPill(
                                    summary = summary,
                                    hideBalances = hide,
                                    onClick = { onOpenAccountDetail(summary.account.id) },
                                    modifier = if (isWide) Modifier.width(215.dp) else Modifier.width(188.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Recent Activity Section
            item {
                Box(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                ) {
                    SectionHeader(
                        title = "Recent Activity",
                        actionLabel = "View All",
                        onAction = onViewAllTransactions,
                        testTag = "home_view_all",
                    )
                }
            }

            if (recentTx.isEmpty()) {
                item {
                    Box(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
                        EmptyLedgerState(
                            title = "No transactions recorded yet",
                            subtitle = "Add your first expense or income entry for ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}.",
                            actionLabel = "Record Expense",
                            onActionClick = onAddExpense,
                        )
                    }
                }
            } else {
                items(recentTx, key = { it.id }) { tx ->
                    val accName = uiState.accountById(tx.accountId)?.name ?: "Account"
                    val destName = tx.destinationAccountId?.let { uiState.accountById(it)?.name }
                    val cat = uiState.categoryById(tx.categoryId)
                    Box(modifier = Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
                        TransactionLedgerRow(
                            transaction = tx,
                            category = cat,
                            accountName = accName,
                            destinationAccountName = destName,
                            defaultCurrencyCode = baseCode,
                            convertedBaseMinor = uiState.convertToBase(tx.amountMinor, tx.currencyCode),
                            hideBalances = hide,
                            onClick = { onOpenTransactionDetail(tx) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showMonthDialog) {
        MonthPickerSheet(
            periods = uiState.availablePeriods,
            selectedPeriod = uiState.selectedPeriod,
            onSelect = onSelectPeriod,
            onDismiss = { showMonthDialog = false },
        )
    }
}

@Composable
private fun HeroMetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.14f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(13.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.onHeroCardMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = semantic.onHeroCard,
            )
        }
    }
}

@Composable
private fun ColorfulQuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GradientAccountCardPill(
    summary: AccountSummary,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = summary.account
    val isCredit = account.type.isLiability
    val (gradStart, gradEnd) = accountGradientColors(account.type)

    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("home_account_pill_${account.id}"),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
                .padding(15.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = accountTypeIcon(account.type),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                    Text(
                        text = if (account.lastFour.isNotBlank()) "•••• ${account.lastFour}" else account.currencyCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }

                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = account.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column {
                    Text(
                        text = if (isCredit) "OUTSTANDING" else "BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Text(
                        text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(summary.calculatedBalanceMinor, account.currencyCode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(
            onClick = onAction,
            modifier = Modifier.testTag(testTag),
        ) {
            Text(actionLabel)
        }
    }
}
