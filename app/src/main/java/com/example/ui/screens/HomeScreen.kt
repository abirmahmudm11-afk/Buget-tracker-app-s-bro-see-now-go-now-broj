package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AccountSummary
import com.example.model.HomeTransactionFilter
import com.example.model.MonthPeriod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.AnimatedMoneyText
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.MonthPickerSheet
import com.example.ui.components.MonthSelectorChip
import com.example.ui.components.QuickExpenseModalSheet
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
    onSelectPeriod: (MonthPeriod) -> Unit,
    onSelectHomeFilter: (HomeTransactionFilter) -> Unit,
    onToggleTheme: (ThemeMode) -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddTransfer: () -> Unit,
    onQuickLogExpense: (
        amountMinor: Long,
        categoryId: String,
        dateMillis: Long,
        accountId: String?,
        note: String,
    ) -> Unit,
    onViewAllTransactions: () -> Unit,
    onManageAccounts: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAccountDetail: (String) -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenTransactionDetail: (TransactionRecord) -> Unit,
) {
    var showMonthDialog by remember { mutableStateOf(false) }
    var showQuickExpenseModal by remember { mutableStateOf(false) }
    var entranceVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entranceVisible = true
    }

    val semantic = SpendlyTheme.semantic
    val baseCode = uiState.defaultCurrencyCode
    val hide = uiState.privacyHideBalances

    val filteredHomeTransactions = remember(uiState.monthlyTransactions, uiState.homeFilter) {
        val list = when (uiState.homeFilter) {
            HomeTransactionFilter.ALL -> uiState.monthlyTransactions
            HomeTransactionFilter.INCOME -> uiState.monthlyTransactions.filter { it.type == TransactionType.INCOME }
            HomeTransactionFilter.EXPENSE -> uiState.monthlyTransactions.filter { it.type == TransactionType.EXPENSE }
        }
        list.sortedByDescending { it.dateMillis }.take(8)
    }

    val topCategories = remember(uiState.monthlyTransactions, uiState.categories, baseCode) {
        val totalExpense = uiState.monthlyExpensesBaseMinor.coerceAtLeast(1L)
        uiState.monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .entries
            .mapNotNull { (catId, txs) ->
                val cat = uiState.categoryById(catId) ?: return@mapNotNull null
                val total = txs.sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
                val ratio = (total.toFloat() / totalExpense.toFloat()).coerceIn(0f, 1f)
                Triple(cat, total, ratio)
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Bar + Editorial Greeting
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpendlyBrandLogo()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                            ) {
                                IconButton(
                                    onClick = onTogglePrivacy,
                                    modifier = Modifier.size(40.dp).testTag("home_privacy_toggle"),
                                ) {
                                    Icon(
                                        imageVector = if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (hide) "Show balances" else "Hide balances",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                            ) {
                                IconButton(
                                    onClick = {
                                        val next = if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                        onToggleTheme(next)
                                    },
                                    modifier = Modifier.size(40.dp).testTag("home_theme_toggle"),
                                ) {
                                    Icon(
                                        imageVector = if (uiState.themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = "Toggle theme appearance",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                            ) {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier.size(40.dp).testTag("home_settings_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Profile and settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
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
                                text = "Hello, ${uiState.profileName.substringBefore(" ")}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (uiState.isPreviewDataActive) {
                                    "Cloud Ledger • Sample Data Active"
                                } else {
                                    "Cloud Ledger • Real-Time Sync"
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

            // Signature Obsidian-Emerald Header Card: Total Balance & Monthly Spending
            item {
                AnimatedVisibility(
                    visible = entranceVisible,
                    enter = fadeIn() + slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        initialOffsetY = { it / 8 },
                    ),
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxWidth()
                            .testTag("home_header_card"),
                        shape = RoundedCornerShape(SpendlyTokens.HeroRadius),
                        shadowElevation = 6.dp,
                        color = Color.Transparent,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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
                            Canvas(modifier = Modifier.matchParentSize()) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.06f),
                                    radius = size.width * 0.42f,
                                    center = Offset(size.width * 0.92f, size.height * 0.14f),
                                    style = Stroke(width = 1.5.dp.toPx()),
                                )
                                drawCircle(
                                    color = semantic.heroAccent.copy(alpha = 0.08f),
                                    radius = size.width * 0.28f,
                                    center = Offset(size.width * 0.92f, size.height * 0.14f),
                                )
                                drawCircle(
                                    color = semantic.heroGold.copy(alpha = 0.06f),
                                    radius = size.width * 0.22f,
                                    center = Offset(size.width * 0.08f, size.height * 0.95f),
                                )
                            }

                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Total Balance",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = semantic.onHeroCardMuted,
                                        )
                                        AnimatedMoneyText(
                                            amountMinor = uiState.totalNetBalanceBaseMinor,
                                            currencyCode = baseCode,
                                            style = MoneyHeroStyle,
                                            color = semantic.onHeroCard,
                                            hidden = hide,
                                            hiddenText = "••••••",
                                            modifier = Modifier.testTag("home_total_balance_value"),
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.White.copy(alpha = 0.14f),
                                    ) {
                                        Text(
                                            text = baseCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = semantic.heroGold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    HeroMetricTile(
                                        label = "Monthly Spending",
                                        amountMinor = uiState.monthlyExpensesBaseMinor,
                                        currencyCode = baseCode,
                                        hide = hide,
                                        icon = Icons.Rounded.ArrowUpward,
                                        badgeColor = Color(0xFFFDA4AF),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("home_monthly_spending_tile"),
                                    )
                                    HeroMetricTile(
                                        label = "Monthly Income",
                                        amountMinor = uiState.monthlyIncomeBaseMinor,
                                        currencyCode = baseCode,
                                        hide = hide,
                                        icon = Icons.Rounded.ArrowDownward,
                                        badgeColor = semantic.heroAccent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("home_monthly_income_tile"),
                                    )
                                }

                                val overallBudget = uiState.overallBudgetSummary
                                val remainingBudget = uiState.remainingOverallBudgetBaseMinor
                                val animatedBudgetProgress by animateFloatAsState(
                                    targetValue = (overallBudget?.progress ?: 0f).coerceIn(0f, 1f),
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "heroBudgetBar",
                                )

                                Surface(
                                    onClick = onOpenBudgets,
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black.copy(alpha = 0.22f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_remaining_budget_pill"),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
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
                                                    tint = semantic.heroAccent,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(
                                                    text = if (remainingBudget != null) "Remaining Monthly Budget" else "Set Monthly Budget",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                    color = semantic.onHeroCardMuted,
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
                                        if (overallBudget != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(Color.White.copy(alpha = 0.16f)),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(animatedBudgetProgress)
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(
                                                            when {
                                                                overallBudget.isOverspent -> Color(0xFFFB7185)
                                                                overallBudget.isNearLimit -> semantic.heroGold
                                                                else -> semantic.heroAccent
                                                            }
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Airy Quick Actions Bar
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
                        tint = semantic.warning,
                        bg = semantic.warningContainer,
                        onClick = onOpenReports,
                        testTag = "home_open_reports",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Main Transaction List with All / Income / Expense Filter Toggle
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Transactions",
                        actionLabel = "View All",
                        onAction = onViewAllTransactions,
                        testTag = "home_view_all",
                    )

                    HomeTransactionFilterBar(
                        selectedFilter = uiState.homeFilter,
                        onSelectFilter = onSelectHomeFilter,
                    )

                    AnimatedContent(
                        targetState = uiState.homeFilter to filteredHomeTransactions,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "homeTxList",
                    ) { (activeFilter, txs) ->
                        if (txs.isEmpty()) {
                            val emptySubtitle = when (activeFilter) {
                                HomeTransactionFilter.ALL ->
                                    "Tap the + Log Expense button to record your first entry for ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}."
                                HomeTransactionFilter.INCOME ->
                                    "No income entries recorded for ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}."
                                HomeTransactionFilter.EXPENSE ->
                                    "No expense entries recorded for ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}."
                            }
                            EmptyLedgerState(
                                title = "No ${if (activeFilter == HomeTransactionFilter.ALL) "" else activeFilter.label.lowercase() + " "}transactions",
                                subtitle = emptySubtitle,
                                actionLabel = if (activeFilter == HomeTransactionFilter.INCOME) "Add Income" else "Log Expense",
                                onActionClick = {
                                    if (activeFilter == HomeTransactionFilter.INCOME) onAddIncome()
                                    else showQuickExpenseModal = true
                                },
                            )
                        } else {
                            LedgerGroupCard {
                                txs.forEachIndexed { idx, tx ->
                                    val accName = uiState.accountById(tx.accountId)?.name ?: "Account"
                                    val destName = tx.destinationAccountId?.let { uiState.accountById(it)?.name }
                                    val cat = uiState.categoryById(tx.categoryId)
                                    TransactionLedgerRow(
                                        transaction = tx,
                                        category = cat,
                                        accountName = accName,
                                        destinationAccountName = destName,
                                        defaultCurrencyCode = baseCode,
                                        convertedBaseMinor = uiState.convertToBase(tx.amountMinor, tx.currencyCode),
                                        hideBalances = hide,
                                        onClick = { onOpenTransactionDetail(tx) },
                                        grouped = true,
                                    )
                                    if (idx < txs.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Spending Categories Strip
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
                            topCategories.forEach { (cat, totalMinor, ratio) ->
                                val (tint, bg) = categoryColorPair(cat.iconKey)
                                Surface(
                                    onClick = onOpenBudgets,
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.width(164.dp),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
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
                                            Text(
                                                text = "${(ratio * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tint,
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = if (hide) "••••" else MoneyFormatter.formatMinor(totalMinor, baseCode),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
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
                                                    .background(tint),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Budget Health
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
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
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

            // Accounts Carousel Section
            item {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Accounts (${uiState.activeAccounts.size})",
                        actionLabel = "Manage All",
                        onAction = onManageAccounts,
                        testTag = "home_manage_accounts",
                    )

                    if (uiState.activeAccounts.isEmpty()) {
                        Surface(
                            onClick = onManageAccounts,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
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
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("ASSETS", style = MaterialTheme.typography.labelSmall, color = semantic.income)
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
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("CARD DUE", style = MaterialTheme.typography.labelSmall, color = semantic.warning)
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
                                    modifier = if (isWide) Modifier.width(220.dp) else Modifier.width(196.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Screen Floating Action Button -> Opens Quick Expense Modal Form
        ExtendedFloatingActionButton(
            onClick = { showQuickExpenseModal = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = "Log expense") },
            text = { Text("Log Expense", fontWeight = FontWeight.SemiBold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("home_fab_log_expense"),
        )
    }

    if (showQuickExpenseModal) {
        QuickExpenseModalSheet(
            uiState = uiState,
            onDismiss = { showQuickExpenseModal = false },
            onSaveExpense = onQuickLogExpense,
        )
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
private fun HomeTransactionFilterBar(
    selectedFilter: HomeTransactionFilter,
    onSelectFilter: (HomeTransactionFilter) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HomeTransactionFilter.entries.forEach { filter ->
                val active = selectedFilter == filter
                val bgColor by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "homeFilterBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "homeFilterText",
                )
                Surface(
                    onClick = { onSelectFilter(filter) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("home_filter_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = bgColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricTile(
    label: String,
    amountMinor: Long,
    currencyCode: String,
    hide: Boolean,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.11f),
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.22f)),
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
            AnimatedMoneyText(
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = semantic.onHeroCard,
                hidden = hide,
                hiddenText = "••••",
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
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
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(gradStart, gradEnd))),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.95f, size.height * 0.1f),
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = accountTypeIcon(account.type),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = if (account.lastFour.isNotBlank()) "•• ${account.lastFour}" else account.currencyCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
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
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column {
                    Text(
                        text = if (isCredit) "OUTSTANDING" else "BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(
            onClick = onAction,
            modifier = Modifier.testTag(testTag),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
