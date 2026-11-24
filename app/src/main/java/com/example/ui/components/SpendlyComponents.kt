package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Work
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
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

fun accountGradientColors(type: AccountType): Pair<Color, Color> = when (type) {
    AccountType.BANK_ACCOUNT -> Color(0xFF072E24) to Color(0xFF136A53)
    AccountType.DEBIT_CARD -> Color(0xFF16252B) to Color(0xFF2B4C59)
    AccountType.CREDIT_CARD -> Color(0xFF1D1B2A) to Color(0xFF423A61)
    AccountType.PREPAID_CARD -> Color(0xFF784506) to Color(0xFFC27803)
    AccountType.CASH_WALLET -> Color(0xFF0A4D38) to Color(0xFF178962)
    AccountType.DIGITAL_WALLET -> Color(0xFF0A4358) to Color(0xFF0E7490)
    AccountType.SAVINGS_ACCOUNT -> Color(0xFF0B3B37) to Color(0xFF0F766E)
    AccountType.CUSTOM -> Color(0xFF5A1D33) to Color(0xFF9F2D54)
}

@Composable
fun SpendlyBrandLogo(modifier: Modifier = Modifier) {
    val semantic = SpendlyTheme.semantic
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(semantic.heroCardStart, semantic.heroCardMid, semantic.heroCardEnd),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-5).dp, y = 5.dp)
                    .clip(CircleShape)
                    .background(semantic.heroGold),
            )
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = "Spendly logo mark",
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
        Column {
            Text(
                text = "Spendly",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun SpendlyBottomNavigationBar(
    currentRoute: String,
    onSelectTab: (SpendlyTopTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                BottomTabItem(
                    tab = SpendlyTopTab.HOME,
                    selected = currentRoute == SpendlyTopTab.HOME.route,
                    onClick = { onSelectTab(SpendlyTopTab.HOME) },
                    modifier = Modifier.weight(1f),
                )
                BottomTabItem(
                    tab = SpendlyTopTab.ACTIVITY,
                    selected = currentRoute == SpendlyTopTab.ACTIVITY.route,
                    onClick = { onSelectTab(SpendlyTopTab.ACTIVITY) },
                    modifier = Modifier.weight(1f),
                )

                // Sleek Centered Emerald Seal Action Button
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        onClick = onAddClick,
                        modifier = Modifier
                            .size(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("nav_add_button"),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 4.dp,
                        color = Color.Transparent,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        listOf(semantic.heroCardStart, semantic.heroCardMid, semantic.heroCardEnd),
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add transaction",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                BottomTabItem(
                    tab = SpendlyTopTab.BUDGETS,
                    selected = currentRoute == SpendlyTopTab.BUDGETS.route,
                    onClick = { onSelectTab(SpendlyTopTab.BUDGETS) },
                    modifier = Modifier.weight(1f),
                )
                BottomTabItem(
                    tab = SpendlyTopTab.MORE,
                    selected = currentRoute == SpendlyTopTab.MORE.route,
                    onClick = { onSelectTab(SpendlyTopTab.MORE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: SpendlyTopTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "tabPill",
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 3.dp)
            .testTag(tab.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(pillColor)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SpendlySideNavigationRail(
    currentRoute: String,
    onSelectTab: (SpendlyTopTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Spacer(Modifier.height(12.dp))
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                modifier = Modifier.testTag("rail_add_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
            }
            Spacer(Modifier.height(12.dp))
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
                modifier = Modifier.testTag("rail_${tab.route}"),
            )
        }
    }
}

@Composable
fun MonthSelectorChip(
    period: MonthPeriod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("month_selector_button"),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
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

@Composable
fun LedgerGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
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
        TransactionType.EXPENSE -> categoryColorPair(category?.iconKey)
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
            "$accountName → ${destinationAccountName ?: "Card"} • Repayment"
        transaction.type == TransactionType.TRANSFER ->
            "$accountName → ${destinationAccountName ?: "Account"}"
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = category?.name ?: transaction.type.label,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifBlank { transaction.merchant.ifBlank { transaction.type.label } },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$subtitleText • ${DateFormatters.formatShortDate(transaction.dateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
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

    if (grouped) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .minimumInteractiveComponentSize()
                .testTag("tx_row_${transaction.id}"),
        ) {
            rowContent()
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("tx_row_${transaction.id}"),
            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
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
        categoryColorPair(summary.category.iconKey)
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
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(catBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIconFor(summary.category?.iconKey ?: "pie"),
                            contentDescription = summary.budget.name,
                            tint = catTint,
                            modifier = Modifier.size(20.dp),
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Budget warning",
                                tint = if (summary.isOverspent) semantic.expense else semantic.warning,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = if (summary.isOverspent) "Over budget" else "Near limit",
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(barColor.copy(alpha = 0.8f), barColor)
                            )
                        ),
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
    val (gradStart, gradEnd) = accountGradientColors(account.type)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_card_${account.id}"),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
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
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(gradStart, gradEnd))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = size.minDimension * 0.55f,
                                center = Offset(size.width * 0.88f, size.height * 0.12f),
                            )
                        }
                        Icon(
                            imageVector = accountTypeIcon(account.type),
                            contentDescription = account.type.displayName,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
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

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isCredit) semantic.warningContainer else semantic.incomeContainer,
                    ) {
                        Text(
                            text = if (isCredit) "DUE" else "BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCredit) semantic.warning else semantic.income,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
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
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(utilization)
                                .height(7.dp)
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_ledger),
                contentDescription = "Empty ledger illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
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
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("empty_state_primary_cta"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(actionLabel)
            }
            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                OutlinedButton(
                    onClick = onSecondaryActionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
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
                placeholder = { Text("Search code, symbol, or country...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(filtered, key = { it.code }) { spec ->
                    val isSelected = spec.code == pendingSelected.code
                    Surface(
                        onClick = { pendingSelected = spec },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_option_${spec.code}"),
                        shape = RoundedCornerShape(16.dp),
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
                                        .size(38.dp)
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
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Confirm ${pendingSelected.code} (${pendingSelected.symbol})")
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
                        shape = RoundedCornerShape(14.dp),
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

fun categoryIconFor(key: String?): ImageVector = when (key) {
    "groceries" -> Icons.Rounded.ShoppingCart
    "dining" -> Icons.Rounded.Restaurant
    "housing" -> Icons.Rounded.Home
    "transport" -> Icons.Rounded.DirectionsCar
    "utilities" -> Icons.Rounded.Bolt
    "shopping" -> Icons.Rounded.ShoppingBag
    "health" -> Icons.Rounded.LocalHospital
    "travel" -> Icons.Rounded.Flight
    "salary" -> Icons.Rounded.Work
    "freelance" -> Icons.Rounded.LaptopMac
    "interest" -> Icons.AutoMirrored.Rounded.ShowChart
    else -> Icons.Rounded.PieChart
}

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
