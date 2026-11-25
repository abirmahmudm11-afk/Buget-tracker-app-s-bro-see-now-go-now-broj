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
import androidx.compose.material.icons.rounded.CreditCard
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
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.example.model.AccountSummary
import com.example.model.HomeTransactionFilter
import com.example.model.MonthPeriod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.AnimatedMoneyText
import com.example.ui.components.AnimatedTransactionItemEntrance
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CategoryChartSlice
import com.example.ui.components.CategoryPieChart
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.Interactive3DWalletCard
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.MonthPickerSheet
import com.example.ui.components.MonthSelectorChip
import com.example.ui.components.QuickExpenseModalSheet
import com.example.ui.components.SpendlyBrandLogo
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.components.WalletDeckStage
import com.example.ui.components.WalletMiniCardThumbnail
import com.example.ui.components.touchSpringCard
import com.example.ui.components.accountAccentPair
import com.example.ui.components.accountTypeIcon
import com.example.ui.theme.MoneyHeroStyle
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.categoryColorPair
import com.example.util.CardArtStore
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
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
                val catName = uiState.categoryById(catId)?.name ?: "Other"
                val total = txs.sumOf { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: 0L }
                Triple(catId, catName, total)
            }
            .filter { it.third > 0L }
            .sortedByDescending { it.third }
            .mapIndexed { idx, (catId, catName, total) ->
                CategoryChartSlice(
                    categoryId = catId,
                    label = catName,
                    amountMinor = total,
                    color = categoryPieColors[idx % categoryPieColors.size],
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Clean Header & Greeting
            item(key = "home_top_bar") {
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
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            IconButton(
                                onClick = onTogglePrivacy,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_privacy_toggle"),
                            ) {
                                Icon(
                                    imageVector = if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (hide) "Show balances" else "Hide balances",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    val next = if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                    onToggleTheme(next)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_theme_toggle"),
                            ) {
                                Icon(
                                    imageVector = if (uiState.themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "Switch appearance",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_settings_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "PRIVATE LEDGER",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                            Text(
                                text = "Hello, ${uiState.profileName.substringBefore(" ")}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        MonthSelectorChip(
                            period = uiState.selectedPeriod,
                            onClick = { showMonthDialog = true },
                        )
                    }
                }
            }

            // Google Wallet 3D Card Carousel of User's Real Added Cards
            item(key = "home_wallet_deck") {
                val context = LocalContext.current
                val artVersion by CardArtStore.version.collectAsStateWithLifecycle()
                val walletSummaries = remember(uiState.accountSummaries) {
                    uiState.accountSummaries.filter { !it.account.isArchived }
                }

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
                            text = if (walletSummaries.isEmpty()) "My Wallet" else "My Wallet (${walletSummaries.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        TextButton(
                            onClick = onManageAccounts,
                            modifier = Modifier.testTag("home_wallet_manage_btn"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (walletSummaries.isEmpty()) "Add Card" else "Manage",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    if (walletSummaries.isEmpty()) {
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
                        val pagerState = rememberPagerState(pageCount = { walletSummaries.size })
                        WalletDeckStage {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = if (walletSummaries.size > 1) 14.dp else 0.dp),
                                    pageSpacing = 12.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { page ->
                                    val summary = walletSummaries[page]
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

                                if (walletSummaries.size > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    ) {
                                        repeat(walletSummaries.size) { index ->
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

            // Architectural Balance Summary Card
            item(key = "home_hero_card") {
                AnimatedVisibility(
                    visible = entranceVisible,
                    enter = fadeIn(tween(240)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        initialOffsetY = { it / 10 },
                    ),
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxWidth()
                            .touchSpringCard(pressScale = 0.982f, maxTiltDeg = 5.5f, hapticOnPress = true)
                            .testTag("home_header_card"),
                        shape = RoundedCornerShape(SpendlyTokens.HeroRadius),
                        color = semantic.heroSurface,
                        border = BorderStroke(1.dp, semantic.heroGold.copy(alpha = 0.28f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .drawBehind {
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                semantic.heroSurfaceElevated,
                                                semantic.heroSurface,
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, size.height),
                                        ),
                                    )
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                semantic.heroGold.copy(alpha = 0.14f),
                                                Color.Transparent,
                                            ),
                                            center = Offset(size.width * 0.92f, size.height * 0.08f),
                                            radius = size.maxDimension * 0.55f,
                                        ),
                                    )
                                }
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Total balance",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = semantic.onHeroSurfaceMuted,
                                    )
                                    AnimatedMoneyText(
                                        amountMinor = uiState.totalNetBalanceBaseMinor,
                                        currencyCode = baseCode,
                                        style = MoneyHeroStyle,
                                        color = semantic.onHeroSurface,
                                        hidden = hide,
                                        hiddenText = "••••••",
                                        maxLines = 1,
                                        modifier = Modifier.testTag("home_total_balance_value"),
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = semantic.onHeroWell,
                                    border = BorderStroke(1.dp, semantic.heroGold.copy(alpha = 0.35f)),
                                ) {
                                    Text(
                                        text = baseCode,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = semantic.heroGold,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                HeroMetricWell(
                                    label = "Monthly spending",
                                    amountMinor = uiState.monthlyExpensesBaseMinor,
                                    currencyCode = baseCode,
                                    hide = hide,
                                    icon = Icons.Rounded.ArrowUpward,
                                    accentColor = Color(0xFFF5A3AC),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_monthly_spending_tile"),
                                )
                                HeroMetricWell(
                                    label = "Monthly income",
                                    amountMinor = uiState.monthlyIncomeBaseMinor,
                                    currencyCode = baseCode,
                                    hide = hide,
                                    icon = Icons.Rounded.ArrowDownward,
                                    accentColor = semantic.heroAccent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("home_monthly_income_tile"),
                                )
                            }

                            val overallBudget = uiState.overallBudgetSummary
                            val remainingBudget = uiState.remainingOverallBudgetBaseMinor
                            val animatedBudgetProgress by animateFloatAsState(
                                targetValue = (overallBudget?.progress ?: 0f).coerceIn(0f, 1f),
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "heroBudgetBar",
                            )

                            Surface(
                                onClick = onOpenBudgets,
                                shape = RoundedCornerShape(12.dp),
                                color = semantic.onHeroWell,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .touchSpringCard(pressScale = 0.975f, maxTiltDeg = 4f)
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
                                                text = if (remainingBudget != null) "Remaining monthly budget" else "Set monthly budget",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = semantic.onHeroSurfaceMuted,
                                            )
                                        }
                                        Text(
                                            text = when {
                                                remainingBudget == null -> "Create"
                                                hide -> "••••"
                                                remainingBudget < 0 -> "Over ${MoneyFormatter.formatMinor(-remainingBudget, baseCode)}"
                                                else -> MoneyFormatter.formatMinor(remainingBudget, baseCode)
                                            },
                                            style = MoneyRowStyle,
                                            color = semantic.onHeroSurface,
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
                                                            overallBudget.isOverspent -> Color(0xFFF5A3AC)
                                                            overallBudget.isNearLimit -> semantic.heroWarn
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

            // Transactions Feed & Category Pie (Side-by-side on wide screens, stacked on phones)
            item(key = "home_transactions_and_categories") {
                val transactionsSection: @Composable (Modifier) -> Unit = { sectionModifier ->
                    Column(
                        modifier = sectionModifier
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

                val categoryPieSection: @Composable (Modifier) -> Unit = { sectionModifier ->
                    Column(
                        modifier = sectionModifier.testTag("home_category_pie_card"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionHeader(
                            title = "Spending by Category",
                            actionLabel = "Reports",
                            onAction = onOpenReports,
                            testTag = "home_categories_reports",
                        )

                        if (categoryPieSlices.isEmpty()) {
                            EmptyLedgerState(
                                title = "No category spending yet",
                                subtitle = "Record an expense for ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)} to view your category pie chart.",
                                actionLabel = "Add Expense",
                                onActionClick = onAddExpense,
                            )
                        } else {
                            LedgerGroupCard {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = DateFormatters.formatMonthPeriod(uiState.selectedPeriod),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = if (hide) "Spending ••••" else "Total ${MoneyFormatter.formatMinor(uiState.monthlyExpensesBaseMinor, baseCode)}",
                                            style = MoneyRowStyle,
                                            color = semantic.expense,
                                        )
                                    }
                                    CategoryPieChart(
                                        slices = categoryPieSlices,
                                        currencyCode = baseCode,
                                        hideBalances = hide,
                                        onSliceInspect = { slice ->
                                            onOpenCategoryInActivity(slice.categoryId)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (isWide) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        transactionsSection(Modifier.weight(1f))
                        categoryPieSection(Modifier.weight(1f))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 860.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        transactionsSection(Modifier.fillMaxWidth())
                        categoryPieSection(Modifier.fillMaxWidth())
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

            // Accounts Summary & Carousel
            item(key = "home_accounts_carousel") {
                Column(
                    modifier = Modifier
                        .widthIn(max = 860.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SectionHeader(
                        title = "Accounts (${uiState.activeAccounts.size})",
                        actionLabel = "Manage all",
                        onAction = onManageAccounts,
                        testTag = "home_manage_accounts",
                    )

                    if (uiState.activeAccounts.isEmpty()) {
                        Surface(
                            onClick = onManageAccounts,
                            modifier = Modifier.fillMaxWidth(),
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
                                    Text("Add your first account", style = MaterialTheme.typography.titleSmall)
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
                        LedgerGroupCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text("Assets", style = MaterialTheme.typography.labelMedium, color = semantic.income)
                                    Text(
                                        text = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.totalAssetsBaseMinor, baseCode),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                VerticalDivider(
                                    modifier = Modifier.height(34.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text("Card due", style = MaterialTheme.typography.labelMedium, color = semantic.warning)
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            activeSummaries.forEach { summary ->
                                AccountCarouselCard(
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

        ExtendedFloatingActionButton(
            onClick = { showQuickExpenseModal = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = "Log expense") },
            text = { Text("Log Expense", fontWeight = FontWeight.SemiBold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 14.dp)
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
private fun HeroMetricWell(
    label: String,
    amountMinor: Long,
    currencyCode: String,
    hide: Boolean,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        modifier = modifier.touchSpringCard(pressScale = 0.968f, maxTiltDeg = 6.5f),
        shape = RoundedCornerShape(12.dp),
        color = semantic.onHeroWell,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.onHeroSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedMoneyText(
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = semantic.onHeroSurface,
                hidden = hide,
                hiddenText = "••••",
                maxLines = 1,
            )
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "quickActionScale",
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .touchSpringCard(pressScale = 0.96f, maxTiltDeg = 7.5f, hapticOnPress = true)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
private fun AccountCarouselCard(
    summary: AccountSummary,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = summary.account
    val isCredit = account.type.isLiability
    val semantic = SpendlyTheme.semantic
    val context = LocalContext.current
    val artVersion by CardArtStore.version.collectAsStateWithLifecycle()
    val artPrefs = remember(account.id, artVersion) {
        CardArtStore.getCardArt(context, account.id)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .touchSpringCard(pressScale = 0.968f, maxTiltDeg = 7f, hapticOnPress = true)
            .minimumInteractiveComponentSize()
            .testTag("home_account_pill_${account.id}"),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (semantic.isDark) 0.92f else 0.96f),
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WalletMiniCardThumbnail(
                    account = account,
                    artPrefs = artPrefs,
                )
                Text(
                    text = if (account.lastFour.isNotBlank()) "•• ${account.lastFour}" else account.currencyCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (isCredit) "Outstanding" else "Balance",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCredit) semantic.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(summary.calculatedBalanceMinor, account.currencyCode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
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
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
