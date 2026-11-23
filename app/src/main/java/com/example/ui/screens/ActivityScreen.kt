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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.MonthPeriod
import com.example.model.SortOption
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.theme.MoneyHeroStyle
import com.example.ui.theme.SpendlyTheme
import com.example.util.ConversionResult
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState

@Composable
fun ActivityScreen(
    uiState: SpendlyUiState,
    onSearchChange: (String) -> Unit,
    onUpdateFilters: (
        period: MonthPeriod?,
        type: TransactionType?,
        accountId: String?,
        categoryId: String?,
        sort: SortOption,
    ) -> Unit,
    onClearFilters: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onDeleteTransaction: (TransactionRecord) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var selectedTxForDetail by remember { mutableStateOf<TransactionRecord?>(null) }
    var txPendingDelete by remember { mutableStateOf<TransactionRecord?>(null) }

    val filtered = remember(
        uiState.transactions,
        uiState.activitySearchQuery,
        uiState.activityFilterPeriod,
        uiState.activityFilterType,
        uiState.activityFilterAccountId,
        uiState.activityFilterCategoryId,
        uiState.activitySortOption,
    ) {
        val q = uiState.activitySearchQuery.trim().lowercase()
        val list = uiState.transactions.filter { tx ->
            val matchesQuery = q.isEmpty() ||
                tx.description.lowercase().contains(q) ||
                tx.merchant.lowercase().contains(q) ||
                tx.notes.lowercase().contains(q) ||
                (uiState.categoryById(tx.categoryId)?.name?.lowercase()?.contains(q) == true)
            val matchesPeriod = uiState.activityFilterPeriod == null ||
                DateFormatters.monthPeriodOf(tx.dateMillis) == uiState.activityFilterPeriod
            val matchesType = uiState.activityFilterType == null || tx.type == uiState.activityFilterType
            val matchesAccount = uiState.activityFilterAccountId == null ||
                tx.accountId == uiState.activityFilterAccountId ||
                tx.destinationAccountId == uiState.activityFilterAccountId
            val matchesCategory = uiState.activityFilterCategoryId == null ||
                tx.categoryId == uiState.activityFilterCategoryId
            matchesQuery && matchesPeriod && matchesType && matchesAccount && matchesCategory
        }

        when (uiState.activitySortOption) {
            SortOption.NEWEST -> list.sortedByDescending { it.dateMillis }
            SortOption.OLDEST -> list.sortedBy { it.dateMillis }
            SortOption.HIGHEST -> list.sortedByDescending { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: it.amountMinor }
            SortOption.LOWEST -> list.sortedBy { uiState.convertToBase(it.amountMinor, it.currencyCode) ?: it.amountMinor }
        }
    }

    val groupedByDate = remember(filtered, uiState.activitySortOption) {
        if (uiState.activitySortOption == SortOption.NEWEST || uiState.activitySortOption == SortOption.OLDEST) {
            filtered.groupBy { DateFormatters.formatDateGroupHeader(it.dateMillis) }
        } else {
            mapOf("Sorted Results (${filtered.size})" to filtered)
        }
    }

    val hasActiveFilter = uiState.activityFilterPeriod != null ||
        uiState.activityFilterType != null ||
        uiState.activityFilterAccountId != null ||
        uiState.activityFilterCategoryId != null ||
        uiState.activitySearchQuery.isNotBlank()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .testTag("activity_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Activity", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "${filtered.size} matching transaction${if (filtered.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (hasActiveFilter) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.testTag("activity_clear_filters"),
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = uiState.activitySearchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("activity_search_input"),
                    placeholder = { Text("Search merchant, note, category...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.activitySearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }

            // Filter & Sort Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Month filter
                    Box {
                        FilterChip(
                            selected = uiState.activityFilterPeriod != null,
                            onClick = { showMonthMenu = true },
                            label = {
                                Text(
                                    uiState.activityFilterPeriod?.let { DateFormatters.formatShortMonth(it) }
                                        ?: "All Months"
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null) },
                            modifier = Modifier.testTag("filter_month_chip"),
                        )
                        DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("All Months") },
                                onClick = {
                                    onUpdateFilters(null, uiState.activityFilterType, uiState.activityFilterAccountId, uiState.activityFilterCategoryId, uiState.activitySortOption)
                                    showMonthMenu = false
                                },
                            )
                            uiState.availablePeriods.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(DateFormatters.formatMonthPeriod(p)) },
                                    onClick = {
                                        onUpdateFilters(p, uiState.activityFilterType, uiState.activityFilterAccountId, uiState.activityFilterCategoryId, uiState.activitySortOption)
                                        showMonthMenu = false
                                    },
                                )
                            }
                        }
                    }

                    // Type filter chips
                    TransactionType.entries.forEach { type ->
                        val active = uiState.activityFilterType == type
                        FilterChip(
                            selected = active,
                            onClick = {
                                onUpdateFilters(
                                    uiState.activityFilterPeriod,
                                    if (active) null else type,
                                    uiState.activityFilterAccountId,
                                    uiState.activityFilterCategoryId,
                                    uiState.activitySortOption,
                                )
                            },
                            label = { Text(type.label) },
                            modifier = Modifier.testTag("filter_type_${type.name.lowercase()}"),
                        )
                    }

                    // Account filter
                    Box {
                        val selectedAcc = uiState.activityFilterAccountId?.let { uiState.accountById(it) }
                        FilterChip(
                            selected = selectedAcc != null,
                            onClick = { showAccountMenu = true },
                            label = { Text(selectedAcc?.name ?: "Account") },
                            modifier = Modifier.testTag("filter_account_chip"),
                        )
                        DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("All Accounts") },
                                onClick = {
                                    onUpdateFilters(uiState.activityFilterPeriod, uiState.activityFilterType, null, uiState.activityFilterCategoryId, uiState.activitySortOption)
                                    showAccountMenu = false
                                },
                            )
                            uiState.accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        onUpdateFilters(uiState.activityFilterPeriod, uiState.activityFilterType, acc.id, uiState.activityFilterCategoryId, uiState.activitySortOption)
                                        showAccountMenu = false
                                    },
                                )
                            }
                        }
                    }

                    // Category filter
                    Box {
                        val selectedCat = uiState.categoryById(uiState.activityFilterCategoryId)
                        FilterChip(
                            selected = selectedCat != null,
                            onClick = { showCategoryMenu = true },
                            label = { Text(selectedCat?.name ?: "Category") },
                            modifier = Modifier.testTag("filter_category_chip"),
                        )
                        DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = {
                                    onUpdateFilters(uiState.activityFilterPeriod, uiState.activityFilterType, uiState.activityFilterAccountId, null, uiState.activitySortOption)
                                    showCategoryMenu = false
                                },
                            )
                            uiState.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        onUpdateFilters(uiState.activityFilterPeriod, uiState.activityFilterType, uiState.activityFilterAccountId, cat.id, uiState.activitySortOption)
                                        showCategoryMenu = false
                                    },
                                )
                            }
                        }
                    }

                    // Sort control
                    Box {
                        FilterChip(
                            selected = uiState.activitySortOption != SortOption.NEWEST,
                            onClick = { showSortMenu = true },
                            label = { Text("Sort: ${uiState.activitySortOption.label}") },
                            leadingIcon = { Icon(Icons.Rounded.SwapVert, contentDescription = null) },
                            modifier = Modifier.testTag("filter_sort_chip"),
                        )
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        onUpdateFilters(uiState.activityFilterPeriod, uiState.activityFilterType, uiState.activityFilterAccountId, uiState.activityFilterCategoryId, opt)
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyLedgerState(
                        title = if (hasActiveFilter) "No matching transactions" else "Your activity ledger is empty",
                        subtitle = if (hasActiveFilter) {
                            "Try clearing one or more filters or broadening your search query."
                        } else {
                            "Record an expense, income, or transfer to begin tracking cash flow."
                        },
                        actionLabel = if (hasActiveFilter) "Clear Filters" else "Add Transaction",
                        onActionClick = { if (hasActiveFilter) onClearFilters() else onAddTransaction() },
                    )
                }
            } else {
                groupedByDate.forEach { (header, groupItems) ->
                    item(key = "hdr_$header") {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        )
                    }
                    items(groupItems, key = { it.id }) { tx ->
                        val accName = uiState.accountById(tx.accountId)?.name ?: "Account"
                        val destName = tx.destinationAccountId?.let { uiState.accountById(it)?.name }
                        val cat = uiState.categoryById(tx.categoryId)
                        TransactionLedgerRow(
                            transaction = tx,
                            category = cat,
                            accountName = accName,
                            destinationAccountName = destName,
                            defaultCurrencyCode = uiState.defaultCurrencyCode,
                            convertedBaseMinor = uiState.convertToBase(tx.amountMinor, tx.currencyCode),
                            hideBalances = uiState.privacyHideBalances,
                            onClick = { selectedTxForDetail = tx },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    selectedTxForDetail?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            uiState = uiState,
            onDismiss = { selectedTxForDetail = null },
            onEdit = {
                selectedTxForDetail = null
                onEditTransaction(tx)
            },
            onRequestDelete = {
                selectedTxForDetail = null
                txPendingDelete = tx
            },
        )
    }

    txPendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txPendingDelete = null },
            title = { Text("Delete Transaction?") },
            text = {
                Text(
                    "Remove \"${tx.description}\" (${MoneyFormatter.formatMinor(tx.amountMinor, tx.currencyCode)})? Account balances and budgets will recalculate immediately."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(tx)
                        txPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_tx_button"),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { txPendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionRecord,
    uiState: SpendlyUiState,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    val semantic = SpendlyTheme.semantic
    val acc = uiState.accountById(transaction.accountId)
    val dest = transaction.destinationAccountId?.let { uiState.accountById(it) }
    val cat = uiState.categoryById(transaction.categoryId)
    val conversion = MoneyFormatter.convertMinor(
        transaction.amountMinor,
        transaction.currencyCode,
        uiState.defaultCurrencyCode,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = transaction.type.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Text(
                    text = DateFormatters.formatDateGroupHeader(transaction.dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = MoneyFormatter.formatMinor(transaction.amountMinor, transaction.currencyCode),
                    style = MoneyHeroStyle,
                    color = when (transaction.type) {
                        TransactionType.EXPENSE -> semantic.expense
                        TransactionType.INCOME -> semantic.income
                        TransactionType.TRANSFER -> semantic.transfer
                    },
                )
                if (transaction.currencyCode != uiState.defaultCurrencyCode) {
                    val convText = when (conversion) {
                        is ConversionResult.Available ->
                            "≈ ${MoneyFormatter.formatMinor(conversion.convertedMinor, conversion.baseCurrencyCode)} (1 ${transaction.currencyCode} = ${conversion.rateText} ${conversion.baseCurrencyCode} • ${conversion.source})"
                        is ConversionResult.Unavailable -> conversion.reason
                    }
                    Text(
                        text = convText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DetailKeyValue("Description", transaction.description.ifBlank { "—" })
            if (transaction.merchant.isNotBlank()) {
                DetailKeyValue("Merchant", transaction.merchant)
            }
            if (transaction.type == TransactionType.TRANSFER) {
                DetailKeyValue("From Account", acc?.name ?: "Account")
                DetailKeyValue("To Account", dest?.name ?: "Account")
                if (transaction.isCreditCardRepayment) {
                    DetailKeyValue("Accounting", "Credit card repayment (excluded from shopping spend)")
                }
            } else {
                DetailKeyValue("Category", cat?.name ?: "Uncategorized")
                DetailKeyValue("Account", acc?.name ?: "Account")
            }
            if (transaction.notes.isNotBlank()) {
                DetailKeyValue("Notes", transaction.notes)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("tx_detail_delete_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Delete")
                }

                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("tx_detail_edit_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Edit")
                }
            }
        }
    }
}

@Composable
private fun DetailKeyValue(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f),
        )
    }
}
