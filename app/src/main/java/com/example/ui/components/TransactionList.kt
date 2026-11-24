package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AccountRecord
import com.example.model.CategoryRecord
import com.example.model.TransactionData
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.theme.MoneyRowStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.categoryColorPair
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import kotlinx.coroutines.delay

/**
 * Scrollable list of user expenses and transactions backed by LazyColumn virtualization,
 * smooth entrance animations, and `material-icons-extended` category icons.
 */
@Composable
fun TransactionList(
    transactions: List<TransactionRecord>,
    categoryLookup: (String?) -> CategoryRecord?,
    accountLookup: (String) -> AccountRecord?,
    defaultCurrencyCode: String,
    convertToBase: (Long, String) -> Long?,
    hideBalances: Boolean,
    onTransactionClick: (TransactionRecord) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    emptyTitle: String = "No expenses yet",
    emptySubtitle: String = "Record an expense with a category, date, and note to get started.",
    onEmptyActionClick: (() -> Unit)? = null,
) {
    val semantic = SpendlyTheme.semantic
    if (transactions.isEmpty()) {
        if (onEmptyActionClick != null) {
            EmptyLedgerState(
                title = emptyTitle,
                subtitle = emptySubtitle,
                actionLabel = "Log Expense",
                onActionClick = onEmptyActionClick,
                modifier = modifier.padding(contentPadding),
            )
        } else {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
                    .testTag("transaction_list_empty"),
                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.surfaceBorder),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = emptyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = emptySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.testTag("transaction_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = transactions,
            key = { _, tx -> tx.id },
            contentType = { _, _ -> "transaction_card" },
        ) { index, tx ->
            val category = categoryLookup(tx.categoryId)
            val accountName = accountLookup(tx.accountId)?.name ?: "Account"
            val destinationName = tx.destinationAccountId?.let { accountLookup(it)?.name }
            AnimatedTransactionItemEntrance(
                index = index,
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
            ) {
                TransactionListItemCard(
                    transaction = tx,
                    category = category,
                    accountName = accountName,
                    destinationAccountName = destinationName,
                    defaultCurrencyCode = defaultCurrencyCode,
                    convertedBaseMinor = convertToBase(tx.amountMinor, tx.currencyCode),
                    hideBalances = hideBalances,
                    onClick = { onTransactionClick(tx) },
                )
            }
        }
    }
}

/**
 * Convenience overload for displaying raw [TransactionData] collections directly
 * from [com.example.data.FirestoreRepository].
 */
@Composable
fun TransactionList(
    transactions: List<TransactionData>,
    defaultCurrencyCode: String = "USD",
    hideBalances: Boolean = false,
    onTransactionClick: (TransactionData) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    val semantic = SpendlyTheme.semantic
    val mapped = remember(transactions) {
        transactions.map { it to it.toTransactionRecord() }
    }
    if (mapped.isEmpty()) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .testTag("transaction_list_empty"),
            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, semantic.surfaceBorder),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "No expenses recorded",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Expenses synced from Firestore appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.testTag("transaction_list"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = mapped,
            key = { _, (raw, _) -> raw.id },
            contentType = { _, _ -> "transaction_data_card" },
        ) { index, (raw, record) ->
            val categoryLabel = raw.category.removePrefix("cat_")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                .ifBlank { "Expense" }
            val syntheticCategory = CategoryRecord(
                id = raw.category,
                name = categoryLabel,
                type = record.type,
                iconKey = raw.category.removePrefix("cat_"),
            )
            AnimatedTransactionItemEntrance(
                index = index,
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
            ) {
                TransactionListItemCard(
                    transaction = record,
                    category = syntheticCategory,
                    accountName = raw.accountId.ifBlank { "Wallet" },
                    destinationAccountName = raw.destinationAccountId,
                    defaultCurrencyCode = defaultCurrencyCode,
                    convertedBaseMinor = null,
                    hideBalances = hideBalances,
                    onClick = { onTransactionClick(raw) },
                )
            }
        }
    }
}

/**
 * Emits virtualized grouped date headers + clean grouped inset ledger rows into a parent [LazyColumn].
 */
fun LazyListScope.groupedTransactionItems(
    groupedByDate: Map<String, List<TransactionRecord>>,
    categoryLookup: (String?) -> CategoryRecord?,
    accountLookup: (String) -> AccountRecord?,
    defaultCurrencyCode: String,
    convertToBase: (Long, String) -> Long?,
    hideBalances: Boolean,
    onTransactionClick: (TransactionRecord) -> Unit,
) {
    groupedByDate.forEach { (header, groupItems) ->
        item(
            key = "hdr_$header",
            contentType = "date_group_header",
        ) {
            Text(
                text = header,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
            )
        }

        itemsIndexed(
            items = groupItems,
            key = { _, tx -> tx.id },
            contentType = { _, _ -> "grouped_tx_row" },
        ) { idx, tx ->
            val isFirst = idx == 0
            val isLast = idx == groupItems.lastIndex
            val cardShape: Shape = when {
                isFirst && isLast -> RoundedCornerShape(SpendlyTokens.CardRadius)
                isFirst -> RoundedCornerShape(
                    topStart = SpendlyTokens.CardRadius,
                    topEnd = SpendlyTokens.CardRadius,
                )
                isLast -> RoundedCornerShape(
                    bottomStart = SpendlyTokens.CardRadius,
                    bottomEnd = SpendlyTokens.CardRadius,
                )
                else -> RoundedCornerShape(0.dp)
            }

            val accName = accountLookup(tx.accountId)?.name ?: "Account"
            val destName = tx.destinationAccountId?.let { accountLookup(it)?.name }
            val cat = categoryLookup(tx.categoryId)

            AnimatedTransactionItemEntrance(
                index = idx,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
            ) {
                Surface(
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        TransactionLedgerRow(
                            transaction = tx,
                            category = cat,
                            accountName = accName,
                            destinationAccountName = destName,
                            defaultCurrencyCode = defaultCurrencyCode,
                            convertedBaseMinor = convertToBase(tx.amountMinor, tx.currencyCode),
                            hideBalances = hideBalances,
                            onClick = { onTransactionClick(tx) },
                            grouped = true,
                        )
                        if (!isLast) {
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

/**
 * Provides an [AnimatedVisibility] entrance combining subtle slide and fade.
 */
@Composable
fun AnimatedTransactionItemEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    maxStaggeredItems: Int = 6,
    staggerStepMs: Long = 28L,
    content: @Composable () -> Unit,
) {
    val staggerMs = if (index in 1..maxStaggeredItems) index * staggerStepMs else 0L
    val visibleState = remember {
        MutableTransitionState(false).apply {
            if (staggerMs == 0L) targetState = true
        }
    }

    if (staggerMs > 0L) {
        LaunchedEffect(Unit) {
            delay(staggerMs)
            visibleState.targetState = true
        }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialOffsetY = { (it * 0.14f).toInt() },
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 160),
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        ),
    ) {
        content()
    }
}

@Composable
fun TransactionListItemCard(
    transaction: TransactionRecord,
    category: CategoryRecord?,
    accountName: String,
    destinationAccountName: String?,
    defaultCurrencyCode: String,
    convertedBaseMinor: Long?,
    hideBalances: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "txCardScale",
    )

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
        else -> extendedCategoryIconFor(category?.iconKey ?: category?.name)
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
        else -> "${category?.name ?: "Expense"} • $accountName"
    }

    Surface(
        shape = RoundedCornerShape(SpendlyTokens.RowRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.surfaceBorder),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(SpendlyTokens.RowRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .minimumInteractiveComponentSize()
            .testTag("tx_row_${transaction.id}"),
    ) {
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
}

fun extendedCategoryIconFor(keyOrName: String?): ImageVector {
    return when (keyOrName?.trim()?.lowercase()) {
        "groceries", "cat_groceries", "market", "food" -> Icons.Rounded.ShoppingCart
        "dining", "cat_dining", "restaurant", "dining & coffee" -> Icons.Rounded.Restaurant
        "coffee", "cafe" -> Icons.Rounded.LocalCafe
        "housing", "cat_housing", "rent", "rent & housing" -> Icons.Rounded.Home
        "transport", "cat_transport", "transit & fuel", "car", "fuel" -> Icons.Rounded.DirectionsCar
        "utilities", "cat_utilities", "bills", "utilities & bills", "electric" -> Icons.Rounded.Bolt
        "shopping", "cat_shopping", "retail" -> Icons.Rounded.ShoppingBag
        "health", "cat_health", "wellness", "health & wellness", "medical" -> Icons.Rounded.LocalHospital
        "travel", "cat_travel", "flight" -> Icons.Rounded.Flight
        "subscriptions", "streaming" -> Icons.Rounded.Subscriptions
        "entertainment", "movies" -> Icons.Rounded.Movie
        "fitness", "gym" -> Icons.Rounded.FitnessCenter
        "pets", "pet" -> Icons.Rounded.Pets
        "education", "school" -> Icons.Rounded.School
        "gift", "gifts" -> Icons.Rounded.CardGiftcard
        "salary", "cat_salary", "payroll", "work" -> Icons.Rounded.Work
        "freelance", "cat_freelance", "consulting" -> Icons.Rounded.LaptopMac
        "interest", "cat_interest", "interest & dividends", "dividends" -> Icons.AutoMirrored.Rounded.ShowChart
        else -> Icons.Rounded.PieChart
    }
}
