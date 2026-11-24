package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BudgetRecord
import com.example.model.BudgetScope
import com.example.model.BudgetSummary
import com.example.model.MonthPeriod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.BudgetRingGauge
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.MonthPickerSheet
import com.example.ui.components.MonthSelectorChip
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    uiState: SpendlyUiState,
    onSelectPeriod: (MonthPeriod) -> Unit,
    onSaveBudget: (
        existingId: String?,
        name: String,
        scope: BudgetScope,
        categoryId: String?,
        limitMinor: Long,
        currencyCode: String,
        period: MonthPeriod,
    ) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onOpenTransactionDetail: (TransactionRecord) -> Unit,
) {
    val semantic = SpendlyTheme.semantic
    var showMonthPicker by remember { mutableStateOf(false) }
    var showBudgetForm by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetRecord?>(null) }
    var defaultScope by remember { mutableStateOf(BudgetScope.OVERALL) }
    var selectedSummaryForBreakdown by remember { mutableStateOf<BudgetSummary?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val overall = uiState.overallBudgetSummary
    val categorySummaries = uiState.categoryBudgetSummaries
    val hide = uiState.privacyHideBalances

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .testTag("budgets_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Budgets", style = MaterialTheme.typography.headlineMedium)
                    OutlinedButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("budget_history_button"),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = "Budget history",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.padding(3.dp))
                        Text("History")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonthSelectorChip(
                        period = uiState.selectedPeriod,
                        onClick = { showMonthPicker = true },
                    )
                    Button(
                        onClick = {
                            editingBudget = null
                            defaultScope = if (overall == null) BudgetScope.OVERALL else BudgetScope.CATEGORY
                            showBudgetForm = true
                        },
                        modifier = Modifier.testTag("add_budget_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.padding(3.dp))
                        Text("Add Budget")
                    }
                }
            }

            item {
                Text(
                    text = "Overall Monthly Budget",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (overall != null) {
                val ringColor = when {
                    overall.isOverspent -> semantic.expense
                    overall.isNearLimit -> semantic.warning
                    else -> semantic.income
                }
                item {
                    Surface(
                        onClick = {
                            editingBudget = overall.budget
                            defaultScope = BudgetScope.OVERALL
                            showBudgetForm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_card_${overall.budget.id}"),
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            BudgetRingGauge(
                                progress = overall.progress,
                                ringColor = ringColor,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                centerValue = "${(overall.progress * 100).toInt()}%",
                                centerLabel = if (overall.isOverspent) "Over" else "Used",
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = DateFormatters.formatMonthPeriod(uiState.selectedPeriod),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit overall budget",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    text = if (hide) "••••••" else if (overall.remainingMinor < 0) {
                                        "Over by ${MoneyFormatter.formatMinor(-overall.remainingMinor, overall.budget.currencyCode)}"
                                    } else {
                                        "${MoneyFormatter.formatMinor(overall.remainingMinor, overall.budget.currencyCode)} left"
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (overall.isOverspent) semantic.expense else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (hide) "Limit hidden" else "${MoneyFormatter.formatMinor(overall.spentMinor, overall.budget.currencyCode)} spent of ${MoneyFormatter.formatMinor(overall.budget.limitMinor, overall.budget.currencyCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    EmptyLedgerState(
                        title = "No overall budget for ${DateFormatters.formatShortMonth(uiState.selectedPeriod)}",
                        subtitle = "Create a monthly spending limit to track remaining headroom and get threshold alerts.",
                        actionLabel = "Create Monthly Budget",
                        onActionClick = {
                            editingBudget = null
                            defaultScope = BudgetScope.OVERALL
                            showBudgetForm = true
                        },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Category Budgets (${categorySummaries.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(
                        onClick = {
                            editingBudget = null
                            defaultScope = BudgetScope.CATEGORY
                            showBudgetForm = true
                        },
                    ) {
                        Text("+ Add limit")
                    }
                }
            }

            if (categorySummaries.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("No category budgets set", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Set limits for Dining, Groceries, or Shopping to get notified before you overspend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(categorySummaries, key = { it.budget.id }) { summary ->
                    BudgetProgressCard(
                        summary = summary,
                        hideBalances = hide,
                        onClick = { selectedSummaryForBreakdown = summary },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            periods = uiState.availablePeriods,
            selectedPeriod = uiState.selectedPeriod,
            onSelect = onSelectPeriod,
            onDismiss = { showMonthPicker = false },
        )
    }

    if (showBudgetForm) {
        BudgetFormSheet(
            uiState = uiState,
            initialScope = defaultScope,
            editingBudget = editingBudget,
            onDismiss = { showBudgetForm = false },
            onSave = { name, scope, catId, limitMinor ->
                onSaveBudget(
                    editingBudget?.id,
                    name,
                    scope,
                    catId,
                    limitMinor,
                    uiState.defaultCurrencyCode,
                    uiState.selectedPeriod,
                )
                showBudgetForm = false
            },
            onDelete = editingBudget?.let { b ->
                {
                    onDeleteBudget(b.id)
                    showBudgetForm = false
                }
            },
        )
    }

    selectedSummaryForBreakdown?.let { summary ->
        val catTx = uiState.monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId == summary.budget.categoryId }
            .sortedByDescending { it.dateMillis }

        ModalBottomSheet(
            onDismissRequest = { selectedSummaryForBreakdown = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = summary.category?.name ?: summary.budget.name,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    OutlinedButton(
                        onClick = {
                            editingBudget = summary.budget
                            defaultScope = summary.budget.scope
                            selectedSummaryForBreakdown = null
                            showBudgetForm = true
                        },
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text("Edit limit")
                    }
                }

                BudgetProgressCard(summary = summary, hideBalances = hide, onClick = {})

                Text("Transactions in this category", style = MaterialTheme.typography.titleSmall)
                if (catTx.isEmpty()) {
                    Text(
                        "No expenses recorded for this category in ${DateFormatters.formatShortMonth(uiState.selectedPeriod)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LedgerGroupCard {
                        catTx.forEachIndexed { idx, tx ->
                            TransactionLedgerRow(
                                transaction = tx,
                                category = summary.category,
                                accountName = uiState.accountById(tx.accountId)?.name ?: "Account",
                                destinationAccountName = null,
                                defaultCurrencyCode = uiState.defaultCurrencyCode,
                                convertedBaseMinor = uiState.convertToBase(tx.amountMinor, tx.currencyCode),
                                hideBalances = hide,
                                onClick = {
                                    selectedSummaryForBreakdown = null
                                    onOpenTransactionDetail(tx)
                                },
                                grouped = true,
                            )
                            if (idx < catTx.lastIndex) {
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

    if (showHistoryDialog) {
        val pastBudgets = uiState.budgets.sortedByDescending { it.year * 100 + it.month }
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Budget History") },
            text = {
                if (pastBudgets.isEmpty()) {
                    Text("No historical budgets recorded yet.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pastBudgets.forEach { b ->
                            val period = MonthPeriod(b.year, b.month)
                            Surface(
                                onClick = {
                                    onSelectPeriod(period)
                                    showHistoryDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(b.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            DateFormatters.formatMonthPeriod(period),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        MoneyFormatter.formatMinor(b.limitMinor, b.currencyCode),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormSheet(
    uiState: SpendlyUiState,
    initialScope: BudgetScope,
    editingBudget: BudgetRecord?,
    onDismiss: () -> Unit,
    onSave: (name: String, scope: BudgetScope, categoryId: String?, limitMinor: Long) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val expenseCategories = remember(uiState.categories) {
        uiState.categories.filter { it.isActive && it.type == TransactionType.EXPENSE }
    }

    var scope by remember { mutableStateOf(editingBudget?.scope ?: initialScope) }
    var selectedCatId by remember {
        mutableStateOf(editingBudget?.categoryId ?: expenseCategories.firstOrNull()?.id ?: "")
    }
    var limitText by remember {
        mutableStateOf(
            if (editingBudget != null) {
                String.format(Locale.US, "%.2f", editingBudget.limitMinor / 100.0)
            } else ""
        )
    }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var limitError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editingBudget != null) "Edit Budget" else "Create Budget",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Period: ${DateFormatters.formatMonthPeriod(uiState.selectedPeriod)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BudgetScope.entries.forEach { s ->
                        val active = scope == s
                        Surface(
                            onClick = { scope = s },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = if (active) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = s.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (scope == BudgetScope.CATEGORY) {
                val selectedCat = uiState.categoryById(selectedCatId)
                ExposedDropdownMenuBox(
                    expanded = catMenuExpanded,
                    onExpandedChange = { catMenuExpanded = !catMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedCat?.name ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expense category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false },
                    ) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCatId = cat.id
                                    catMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = limitText,
                onValueChange = {
                    limitText = it
                    limitError = null
                },
                label = { Text("Spending limit (${uiState.defaultCurrencyCode}) *") },
                placeholder = { Text("e.g., 500.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                isError = limitError != null,
                supportingText = limitError?.let { err -> { Text(err) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = {
                    val minor = MoneyFormatter.parseMajorToMinor(limitText, uiState.defaultCurrencyCode)
                    if (minor == null || minor <= 0L) {
                        limitError = "Enter a limit greater than zero"
                    } else {
                        val name = if (scope == BudgetScope.OVERALL) {
                            "${DateFormatters.formatShortMonth(uiState.selectedPeriod)} Overall Budget"
                        } else {
                            uiState.categoryById(selectedCatId)?.name ?: "Category Budget"
                        }
                        onSave(name, scope, if (scope == BudgetScope.CATEGORY) selectedCatId else null, minor)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_budget_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (editingBudget != null) "Update Budget" else "Save Budget")
            }

            if (onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.padding(3.dp))
                    Text("Remove Budget")
                }
            }
        }
    }
}
