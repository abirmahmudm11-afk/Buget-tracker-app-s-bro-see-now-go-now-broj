package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AccountSummary
import com.example.model.AccountType
import com.example.model.BudgetSummary
import com.example.model.CategoryRecord
import com.example.model.CurrencySpec
import com.example.model.MonthPeriod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.categoryColorPair
import com.example.util.CurrencyCatalog
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter

enum class SpendlyTopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    HOME("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    ACTIVITY("activity", "Activity", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_activity"),
    BUDGETS("budgets", "Budgets", Icons.Rounded.PieChart, Icons.Outlined.PieChart, "nav_budgets"),
    MORE("more", "More", Icons.Rounded.GridView, Icons.Outlined.GridView, "nav_more"),
}

fun accountAccentPair(type: AccountType, isDark: Boolean = false): Pair<Color, Color> {
    if (isDark) {
        return when (type) {
            AccountType.BANK_ACCOUNT -> Color(0xFF7CB4E8) to Color(0xFF1A3147)
            AccountType.DEBIT_CARD -> Color(0xFF67C9D6) to Color(0xFF15373D)
            AccountType.CREDIT_CARD -> Color(0xFFB4A7EB) to Color(0xFF2B2547)
            AccountType.PREPAID_CARD -> Color(0xFFEBB04D) to Color(0xFF3E2D12)
            AccountType.CASH_WALLET -> Color(0xFF52D1A2) to Color(0xFF143A2C)
            AccountType.DIGITAL_WALLET -> Color(0xFF6AB3E8) to Color(0xFF163348)
            AccountType.SAVINGS_ACCOUNT -> Color(0xFF5AD6B3) to Color(0xFF173D34)
            AccountType.CUSTOM -> Color(0xFFF27C89) to Color(0xFF451C22)
        }
    }
    return when (type) {
        AccountType.BANK_ACCOUNT -> Color(0xFF1D4E89) to Color(0xFFE0EAF6)
        AccountType.DEBIT_CARD -> Color(0xFF156778) to Color(0xFFDDF0F4)
        AccountType.CREDIT_CARD -> Color(0xFF4F4698) to Color(0xFFEAE8F8)
        AccountType.PREPAID_CARD -> Color(0xFF9A5B00) to Color(0xFFFDF0D5)
        AccountType.CASH_WALLET -> Color(0xFF0D6E4E) to Color(0xFFDCF3EA)
        AccountType.DIGITAL_WALLET -> Color(0xFF1D5A8A) to Color(0xFFE1EFFA)
        AccountType.SAVINGS_ACCOUNT -> Color(0xFF0F5C4D) to Color(0xFFDCEEE9)
        AccountType.CUSTOM -> Color(0xFFB42334) to Color(0xFFFCE6E9)
    }
}

@Composable
fun SpendlyBrandLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = "Spendly emblem",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = "Spendly",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Clean native Material 3 bottom navigation bar: tabs navigate cleanly without inline action orbs.
 */
@Composable
fun SpendlyBottomNavigationBar(
    currentRoute: String,
    onSelectTab: (SpendlyTopTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            SpendlyTopTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag(tab.testTag),
                )
            }
        }
    }
}

@Composable
fun SpendlySideNavigationRail(
    currentRoute: String,
    onSelectTab: (SpendlyTopTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            header = {
                Spacer(Modifier.height(14.dp))
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(14.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                    modifier = Modifier.testTag("rail_add_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
                }
                Spacer(Modifier.height(14.dp))
            },
        ) {
            SpendlyTopTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                        )
                    },
                    label = { Text(tab.label) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.testTag("rail_${tab.route}"),
                )
            }
        }
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
fun MonthSelectorChip(
    period: MonthPeriod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("month_selector_button"),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = "Selected month",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = DateFormatters.formatShortMonth(period),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Grouped inset surface following the single-elevation rule (clean hairline border, zero drop shadow).
 */
@Composable
fun LedgerGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Column(content = content)
    }
}

@Composable
fun TransactionLedgerRow(
    transaction: TransactionRecord,
    category: CategoryRecord?,
    accountName: String,
    destinationAccountName: String?,
    defaultCurrencyCode: String,
    convertedBaseMinor: Long?,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    grouped: Boolean = false,
) {
    val semantic = SpendlyTheme.semantic
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> semantic.expense
        TransactionType.INCOME -> semantic.income
        TransactionType.TRANSFER -> semantic.transfer
    }

    val (iconTint, iconContainer) = when (transaction.type) {
        TransactionType.TRANSFER -> semantic.transfer to semantic.transferContainer
        TransactionType.INCOME -> semantic.income to semantic.incomeContainer
        TransactionType.EXPENSE -> categoryColorPair(category?.iconKey, semantic.isDark)
    }
    val icon = when (transaction.type) {
        TransactionType.TRANSFER -> Icons.AutoMirrored.Rounded.CompareArrows
        else -> categoryIconFor(category?.iconKey)
    }

    val amountPrefix = when (transaction.type) {
        TransactionType.EXPENSE -> "-"
        TransactionType.INCOME -> "+"
        TransactionType.TRANSFER -> ""
    }
    val subtitleText = when {
        transaction.type == TransactionType.TRANSFER && transaction.isCreditCardRepayment ->
            "$accountName -> ${destinationAccountName ?: "Card"} • Repayment"
        transaction.type == TransactionType.TRANSFER ->
            "$accountName -> ${destinationAccountName ?: "Account"}"
        else -> "${category?.name ?: "Uncategorized"} • $accountName"
    }

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category?.name ?: transaction.type.label,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp),
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = transaction.description.ifBlank { transaction.merchant.ifBlank { transaction.type.label } },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$subtitleText • ${DateFormatters.formatShortDate(transaction.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (hideBalances) "••••" else "$amountPrefix${MoneyFormatter.formatMinor(transaction.amountMinor, transaction.currencyCode)}",
                    style = MoneyRowStyle,
                    color = amountColor,
                )
                if (!hideBalances && transaction.currencyCode != defaultCurrencyCode) {
                    Text(
                        text = if (convertedBaseMinor != null) {
                            "≈ ${MoneyFormatter.formatMinor(convertedBaseMinor, defaultCurrencyCode)}"
                        } else {
                            "Rate unavailable"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "txRowScale",
    )

    if (grouped) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = rowScale
                    scaleY = rowScale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .minimumInteractiveComponentSize()
                .testTag("tx_row_${transaction.id}"),
        ) {
            rowContent()
        }
    } else {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = rowScale
                    scaleY = rowScale
                }
                .minimumInteractiveComponentSize()
                .testTag("tx_row_${transaction.id}"),
            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, semantic.surfaceBorder),
        ) {
            rowContent()
        }
    }
}

@Composable
fun BudgetProgressCard(
    summary: BudgetSummary,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "budgetProgress",
    )
    val barColor = when {
        summary.isOverspent -> semantic.expense
        summary.isNearLimit -> semantic.warning
        else -> semantic.income
    }
    val (catTint, catBg) = if (summary.category != null) {
        categoryColorPair(summary.category.iconKey, semantic.isDark)
    } else {
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_card_${summary.budget.id}"),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(catBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIconFor(summary.category?.iconKey ?: "pie"),
                            contentDescription = summary.budget.name,
                            tint = catTint,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Column {
                        Text(
                            text = summary.category?.name ?: summary.budget.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (hideBalances) "Limit hidden" else "Limit ${MoneyFormatter.formatMinor(summary.budget.limitMinor, summary.budget.currencyCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (summary.isOverspent || summary.isNearLimit) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (summary.isOverspent) semantic.expenseContainer else semantic.warningContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Budget alert",
                                tint = if (summary.isOverspent) semantic.expense else semantic.warning,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = if (summary.isOverspent) "Over" else "Near limit",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (summary.isOverspent) semantic.expense else semantic.warning,
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = semantic.incomeContainer,
                    ) {
                        Text(
                            text = "${(summary.progress * 100).toInt()}% used",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.income,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
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
                        .fillMaxWidth(animatedProgress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(barColor),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (hideBalances) "Spent ••••" else "Spent ${MoneyFormatter.formatMinor(summary.spentMinor, summary.budget.currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val remainingText = if (summary.remainingMinor < 0) {
                    "Over by ${MoneyFormatter.formatMinor(-summary.remainingMinor, summary.budget.currencyCode)}"
                } else {
                    "${MoneyFormatter.formatMinor(summary.remainingMinor, summary.budget.currencyCode)} left"
                }
                Text(
                    text = if (hideBalances) "•••• left" else remainingText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (summary.isOverspent) semantic.expense else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun AccountLedgerCard(
    summary: AccountSummary,
    defaultCurrencyCode: String,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = summary.account
    val semantic = SpendlyTheme.semantic
    val isCredit = account.type.isLiability
    val context = LocalContext.current
    val artVersion by com.example.util.CardArtStore.version.collectAsStateWithLifecycle()
    val artPrefs = remember(account.id, artVersion) {
        com.example.util.CardArtStore.getCardArt(context, account.id)
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_card_${account.id}"),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    WalletMiniCardThumbnail(
                        account = account,
                        artPrefs = artPrefs,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val meta = buildString {
                            append(account.type.displayName)
                            if (account.institutionName.isNotBlank()) {
                                append(" • ")
                                append(account.institutionName)
                            }
                            if (account.lastFour.isNotBlank()) {
                                append(" ••")
                                append(account.lastFour)
                            }
                        }
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isCredit) "Outstanding" else "Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCredit) semantic.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (hideBalances) "••••" else MoneyFormatter.formatMinor(summary.calculatedBalanceMinor, account.currencyCode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isCredit && summary.calculatedBalanceMinor > 0) semantic.warning else MaterialTheme.colorScheme.onSurface,
                    )
                    if (!hideBalances && account.currencyCode != defaultCurrencyCode) {
                        Text(
                            text = summary.convertedBaseMinor?.let {
                                "≈ ${MoneyFormatter.formatMinor(it, defaultCurrencyCode)}"
                            } ?: "Rate unavailable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (isCredit && account.creditLimitMinor != null && account.creditLimitMinor > 0) {
                val utilization = (summary.calculatedBalanceMinor.toFloat() / account.creditLimitMinor.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(utilization)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (utilization >= 0.8f) semantic.expense else semantic.warning
                                ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Utilization ${(utilization * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (hideBalances) "Limit ••••" else "Limit ${MoneyFormatter.formatMinor(account.creditLimitMinor, account.currencyCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean geometric empty state with light/dark adaptive ledger diagram, honest copy, and clear action.
 */
@Composable
fun EmptyLedgerState(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
) {
    val semantic = SpendlyTheme.semantic
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val lineColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Canvas(modifier = Modifier.size(64.dp)) {
                drawRoundRect(
                    color = container,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                )
                val pad = 16.dp.toPx()
                val w = size.width - pad * 2
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(pad, pad + 2.dp.toPx()),
                    size = Size(w * 0.55f, 5.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(pad, pad + 13.dp.toPx()),
                    size = Size(w, 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(pad, pad + 23.dp.toPx()),
                    size = Size(w * 0.78f, 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_state_primary_cta"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(actionLabel)
            }
            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                OutlinedButton(
                    onClick = onSecondaryActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableCurrencySheet(
    currentCurrencyCode: String,
    onDismiss: () -> Unit,
    onConfirmCurrency: (CurrencySpec) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var pendingSelected by remember { mutableStateOf(CurrencyCatalog.findByCode(currentCurrencyCode)) }
    val semantic = SpendlyTheme.semantic

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) CurrencyCatalog.supportedCurrencies
        else CurrencyCatalog.supportedCurrencies.filter {
            it.code.lowercase().contains(q) ||
                it.displayName.lowercase().contains(q) ||
                it.symbol.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Select Currency",
                    style = MaterialTheme.typography.headlineSmall,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close currency picker")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_search_input"),
                placeholder = { Text("Search code, symbol, or country") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = semantic.surfaceBorder,
                ),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(filtered, key = { it.code }) { spec ->
                    val isSelected = spec.code == pendingSelected.code
                    Surface(
                        onClick = { pendingSelected = spec },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_option_${spec.code}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = spec.symbol,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${spec.code} — ${spec.displayName}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = if (spec.rateToUsd != null) {
                                            "Rate ref: ${spec.rateSource}"
                                        } else {
                                            "Conversion rate unavailable"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Button(
                onClick = { onConfirmCurrency(pendingSelected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_currency_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Use ${pendingSelected.code} (${pendingSelected.symbol})")
            }
        }
    }
}

@Composable
fun MonthPickerSheet(
    periods: List<MonthPeriod>,
    selectedPeriod: MonthPeriod,
    onSelect: (MonthPeriod) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reporting Month", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEach { period ->
                    val selected = period == selectedPeriod
                    Surface(
                        onClick = {
                            onSelect(period)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("month_option_${period.year}_${period.month}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = DateFormatters.formatMonthPeriod(period),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

fun categoryIconFor(key: String?): ImageVector = extendedCategoryIconFor(key)

fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.BANK_ACCOUNT -> Icons.Rounded.AccountBalance
    AccountType.DEBIT_CARD -> Icons.Rounded.CreditCard
    AccountType.CREDIT_CARD -> Icons.Rounded.CreditCard
    AccountType.PREPAID_CARD -> Icons.Rounded.Payments
    AccountType.CASH_WALLET -> Icons.Rounded.Wallet
    AccountType.DIGITAL_WALLET -> Icons.Rounded.Smartphone
    AccountType.SAVINGS_ACCOUNT -> Icons.Rounded.Savings
    AccountType.CUSTOM -> Icons.Rounded.AccountBalance
}
