package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.HomeTransactionFilter
import com.example.model.MonthPeriod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.AnimatedTransactionItemEntrance
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CategoryChartSlice
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.HomeAlertsBottomSheet
import com.example.ui.components.Interactive3DWalletCard
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.MonthPickerSheet
import com.example.ui.components.QuickAccountSummaryCard
import com.example.ui.components.QuickExpenseModalSheet
import com.example.ui.components.SpendingAnalyticsArcCard
import com.example.ui.components.SpendlyHorizonHeroCard
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.components.WalletDeckStage
import com.example.ui.components.pressBounce
import com.example.ui.components.touchSpringCard
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.ThemeMode
import com.example.util.CardArtStore
import com.example.util.DateFormatters
import com.example.viewmodel.SpendlyUiState
import kotlin.math.abs

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
    onOpenCategoryInActivity: (String) -> Unit = {},
) {
    var showMonthDialog by remember { mutableStateOf(false) }
    var showQuickExpenseModal by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var entranceVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entranceVisible = true
    }

    val semantic = SpendlyTheme.semantic
    val baseCode = uiState.defaultCurrencyCode
    val hide = uiState.privacyHideBalances
    val context = LocalContext.current
    val artVersion by CardArtStore.version.collectAsStateWithLifecycle()

    val activeSummaries = remember(uiState.accountSummaries) {
        uiState.accountSummaries.filter { !it.account.isArchived }
    }

    val alertBudgets = remember(uiState.overallBudgetSummary, uiState.categoryBudgetSummaries) {
        (listOfNotNull(uiState.overallBudgetSummary) + uiState.categoryBudgetSummaries)
            .filter { it.isOverspent || it.isNearLimit }
    }

    val filteredHomeTransactions = remember(uiState.monthlyTransactions, uiState.homeFilter) {
        val list = when (uiState.homeFilter) {
            HomeTransactionFilter.ALL -> uiState.monthlyTransactions
            HomeTransactionFilter.INCOME -> uiState.monthlyTransactions.filter { it.type == TransactionType.INCOME }
            HomeTransactionFilter.EXPENSE -> uiState.monthlyTransactions.filter { it.type == TransactionType.EXPENSE }
        }
        list.sortedByDescending { it.dateMillis }.take(8)
    }

    val categoryPieColors = remember(semantic.isDark) {
        if (semantic.isDark) {
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
    }

    val categoryPieSlices = remember(uiState.monthlyTransactions, uiState.categories, baseCode, categoryPieColors) {
        uiState.monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId ?: "uncategorized" }
            .entries
            .map { (catId, txs) ->
                val cat = uiState.categoryById(catId)
                val total = txs.sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
                Triple(catId, cat, total)
            }
            .filter { it.third > 0L }
            .sortedByDescending { it.third }
            .mapIndexed { idx, (catId, cat, total) ->
                CategoryChartSlice(
                    categoryId = catId,
                    label = cat?.name ?: "Other",
                    amountMinor = total,
                    color = categoryPieColors[idx % categoryPieColors.size],
                    iconKey = cat?.iconKey ?: "",
                )
            }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        val isCompact = maxWidth < 360.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Compact Single-Row Top Header: Avatar + Name (left) & Theme / Period / Alerts (right)
            item(key = "home_top_bar") {
                val initial = uiState.profileName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                val isNonCurrentMonth = uiState.selectedPeriod != MonthPeriod.current()
                Row(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .pressBounce(pressScale = 0.95f, hapticOnPress = true)
                            .clickable(onClick = onOpenSettings)
                            .minimumInteractiveComponentSize()
                            .padding(end = 8.dp)
                            .testTag("home_settings_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(
                            text = uiState.profileName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HeaderCircleButton(
                            onClick = {
                                val next = if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                onToggleTheme(next)
                            },
                            testTag = "home_theme_toggle",
                        ) {
                            Icon(
                                imageVector = if (uiState.themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                contentDescription = "Switch appearance",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }

                        Box {
                            HeaderCircleButton(
                                onClick = { showMonthDialog = true },
                                testTag = "month_selector_button",
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = "Select month (${DateFormatters.formatShortMonth(uiState.selectedPeriod)})",
                                    tint = if (isNonCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            if (isNonCurrentMonth) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-3).dp, y = 3.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }

                        Box {
                            HeaderCircleButton(
                                onClick = { showAlertsSheet = true },
                                testTag = "home_alerts_button",
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsNone,
                                    contentDescription = "Ledger alerts and quick settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            if (alertBudgets.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = semantic.expense,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 1.dp, y = 1.dp)
                                        .size(16.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (alertBudgets.size > 9) "9+" else alertBudgets.size.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Spendly Horizon Landscape Hero Card (Light sunrise default, twilight in dark mode)
            item(key = "home_hero_card") {
                AnimatedVisibility(
                    visible = entranceVisible,
                    enter = fadeIn(tween(220)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        initialOffsetY = { it / 12 },
                    ),
                ) {
                    SpendlyHorizonHeroCard(
                        profileName = uiState.profileName,
                        balanceMinor = uiState.totalNetBalanceBaseMinor,
                        monthlyIncomeMinor = uiState.monthlyIncomeBaseMinor,
                        monthlyExpenseMinor = uiState.monthlyExpensesBaseMinor,
                        currencyCode = baseCode,
                        hideBalances = hide,
                        onTogglePrivacy = onTogglePrivacy,
                        onIncomeClick = { onSelectHomeFilter(HomeTransactionFilter.INCOME) },
                        onExpenseClick = { onSelectHomeFilter(HomeTransactionFilter.EXPENSE) },
                        modifier = Modifier.widthIn(max = 860.dp),
                    )
                }
            }

            // 2-Column Quick Account Summary Cards with Crisp Bank & Card Network Logos
            item(key = "home_quick_accounts") {
                BoxWithConstraints(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                ) {
                    if (activeSummaries.isEmpty()) {
                        Surface(
                            onClick = onManageAccounts,
                            modifier = Modifier
                                .fillMaxWidth()
                                .touchSpringCard(pressScale = 0.98f, maxTiltDeg = 4.5f, hapticOnPress = true)
                                .testTag("home_manage_accounts"),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, semantic.surfaceBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Add your accounts & cards",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = "Track checking, savings, Visa/Mastercard & cash balances.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Manage accounts",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    } else {
                        val cardWidth = if (activeSummaries.size == 1) {
                            maxWidth
                        } else if (isWide) {
                            ((maxWidth - 20.dp) / 3f).coerceAtLeast(180.dp)
                        } else {
                            ((maxWidth - 10.dp) / 2f).coerceAtLeast(148.dp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            activeSummaries.forEach { summary ->
                                val artPrefs = remember(summary.account.id, artVersion) {
                                    CardArtStore.getCardArt(context, summary.account.id)
                                }
                                QuickAccountSummaryCard(
                                    summary = summary,
                                    artPrefs = artPrefs,
                                    isFeatured = false,
                                    hideBalances = hide,
                                    onClick = { onOpenAccountDetail(summary.account.id) },
                                    modifier = Modifier.width(cardWidth),
                                )
                            }
                        }
                    }
                }
            }

            // Spending Analytics Arc Gauge + Surplus/Deficit Badge + Ranked Top Categories
            item(key = "home_spending_analytics") {
                SpendingAnalyticsArcCard(
                    period = uiState.selectedPeriod,
                    incomeMinor = uiState.monthlyIncomeBaseMinor,
                    expenseMinor = uiState.monthlyExpensesBaseMinor,
                    currencyCode = baseCode,
                    slices = categoryPieSlices,
                    hideBalances = hide,
                    onCategoryClick = onOpenCategoryInActivity,
                    onOpenReports = onOpenReports,
                    modifier = Modifier.widthIn(max = 860.dp),
                )
            }

            // Quick Actions Row
            item(key = "home_quick_actions") {
                Row(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp),
                ) {
                    QuickActionTile(
                        label = "Expense",
                        icon = Icons.Rounded.ArrowUpward,
                        tint = semantic.expense,
                        bg = semantic.expenseContainer,
                        onClick = onAddExpense,
                        testTag = "home_add_expense",
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Income",
                        icon = Icons.Rounded.ArrowDownward,
                        tint = semantic.income,
                        bg = semantic.incomeContainer,
                        onClick = onAddIncome,
                        testTag = "home_add_income",
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
                        label = "Transfer",
                        icon = Icons.AutoMirrored.Rounded.CompareArrows,
                        tint = semantic.transfer,
                        bg = semantic.transferContainer,
                        onClick = onAddTransfer,
                        testTag = "home_add_transfer",
                        modifier = Modifier.weight(1f),
                    )
                    QuickActionTile(
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

            // Recent Transactions Feed
            item(key = "home_transactions_feed") {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Transactions",
                        actionLabel = "View all",
                        onAction = onViewAllTransactions,
                        testTag = "home_view_all",
                    )

                    HomeTransactionFilterBar(
                        selectedFilter = uiState.homeFilter,
                        onSelectFilter = onSelectHomeFilter,
                    )

                    AnimatedContent(
                        targetState = uiState.homeFilter,
                        transitionSpec = {
                            (fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                    initialOffsetY = { (it * 0.05f).toInt() },
                                )) togetherWith fadeOut(tween(140))
                        },
                        label = "homeTxList",
                    ) { activeFilter ->
                        val txs = filteredHomeTransactions
                        if (txs.isEmpty()) {
                            val emptySubtitle = when (activeFilter) {
                                HomeTransactionFilter.ALL ->
                                    "Log an expense or income entry for ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}."
                                HomeTransactionFilter.INCOME ->
                                    "No income recorded in ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}."
                                HomeTransactionFilter.EXPENSE ->
                                    "No expenses recorded in ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}."
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
                                    androidx.compose.runtime.key(activeFilter, tx.id) {
                                        val accName = uiState.accountById(tx.accountId)?.name ?: "Account"
                                        val destName = tx.destinationAccountId?.let { uiState.accountById(it)?.name }
                                        val cat = uiState.categoryById(tx.categoryId)
                                        AnimatedTransactionItemEntrance(index = idx) {
                                            Column {
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
                                                        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
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
                    }
                }
            }

            // Monthly Budget
            item(key = "home_monthly_budget") {
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
                                .touchSpringCard(pressScale = 0.978f, maxTiltDeg = 5f)
                                .testTag("home_create_budget_cta"),
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, semantic.surfaceBorder),
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

            // 3D Wallet Card Carousel (Below the fold for inspecting custom card art & bank cards)
            item(key = "home_wallet_deck") {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth()
                        .testTag("home_wallet_deck"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (activeSummaries.isEmpty()) "My Wallet" else "My Wallet (${activeSummaries.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        TextButton(
                            onClick = onManageAccounts,
                            modifier = Modifier
                                .pressBounce(pressScale = 0.92f, hapticOnPress = true)
                                .testTag("home_wallet_manage_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (activeSummaries.isEmpty()) "Add Card" else "Manage",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    if (activeSummaries.isEmpty()) {
                        Surface(
                            onClick = onManageAccounts,
                            modifier = Modifier
                                .fillMaxWidth()
                                .touchSpringCard(pressScale = 0.976f, maxTiltDeg = 5.5f, hapticOnPress = true),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, semantic.surfaceBorder),
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Add your cards to Wallet",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                    Text(
                                        text = "Add real card photos, bank logos, Visa/Mastercard marks, and opening balances.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Add card",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    } else {
                        val pagerState = rememberPagerState(pageCount = { activeSummaries.size })
                        WalletDeckStage {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = if (activeSummaries.size > 1) 14.dp else 0.dp),
                                    pageSpacing = 12.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { page ->
                                    val summary = activeSummaries[page]
                                    val artPrefs = remember(summary.account.id, artVersion) {
                                        CardArtStore.getCardArt(context, summary.account.id)
                                    }
                                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    val cardScale = (1f - abs(pageOffset).coerceIn(0f, 1f) * 0.06f)

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = cardScale
                                                scaleY = cardScale
                                            },
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Interactive3DWalletCard(
                                            account = summary.account,
                                            balanceMinor = summary.calculatedBalanceMinor,
                                            hideBalances = hide,
                                            artPrefs = artPrefs,
                                            enableDragTilt = false,
                                            externalTiltY = (-pageOffset * 18f).coerceIn(-22f, 22f),
                                            secondaryHint = "Tap card",
                                            testTag = "home_wallet_card_${summary.account.id}",
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "${summary.transactionCount} transactions",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = semantic.onHeroSurfaceMuted,
                                            )
                                            TextButton(
                                                onClick = { onOpenAccountDetail(summary.account.id) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.pressBounce(pressScale = 0.92f, hapticOnPress = true),
                                            ) {
                                                Text(
                                                    "Inspect & Edit",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = semantic.heroGold,
                                                )
                                            }
                                        }
                                    }
                                }

                                if (activeSummaries.size > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    ) {
                                        repeat(activeSummaries.size) { index ->
                                            val active = pagerState.currentPage == index
                                            Box(
                                                modifier = Modifier
                                                    .height(6.dp)
                                                    .width(if (active) 22.dp else 6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (active) semantic.heroGold
                                                        else Color.White.copy(alpha = 0.28f)
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

    if (showAlertsSheet) {
        HomeAlertsBottomSheet(
            alerts = alertBudgets,
            hideBalances = hide,
            onTogglePrivacy = onTogglePrivacy,
            onOpenBudgets = onOpenBudgets,
            onOpenSettings = onOpenSettings,
            onDismiss = { showAlertsSheet = false },
        )
    }
}

@Composable
private fun HeaderCircleButton(
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
        modifier = Modifier
            .pressBounce(pressScale = 0.88f, hapticOnPress = true)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun HomeTransactionFilterBar(
    selectedFilter: HomeTransactionFilter,
    onSelectFilter: (HomeTransactionFilter) -> Unit,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
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
                    targetValue = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                    label = "homeFilterBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "homeFilterText",
                )
                Surface(
                    onClick = { onSelectFilter(filter) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .pressBounce(pressScale = 0.93f, hapticOnPress = true)
                        .testTag("home_filter_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(999.dp),
                    color = bgColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
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
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic

    Surface(
        onClick = onClick,
        modifier = modifier
            .touchSpringCard(pressScale = 0.92f, maxTiltDeg = 7.5f, hapticOnPress = true)
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (semantic.isDark) 0.92f else 0.96f),
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            modifier = Modifier
                .pressBounce(pressScale = 0.92f, hapticOnPress = true)
                .testTag(testTag),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
