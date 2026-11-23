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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.MonthPickerSheet
import com.example.ui.components.MonthSelectorChip
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.theme.SpendlyTokens
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState

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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Budgets", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Track spending limits without double-counting repayments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier.testTag("budget_history_button"),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = "Budget history")
                            Spacer(Modifier.padding(2.dp))
                            Text("History")
                        }
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
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.padding(2.dp))
                        Text("Add Budget")
                    }
                }
            }

            // Overall Monthly Budget
            item {
                Text(
                    text = "Overall Monthly Budget",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (overall != null) {
                item {
                    BudgetProgressCard(
                        summary = overall,
                        hideBalances = hide,
                        onClick = {
                            editingBudget = overall.budget
                            defaultScope = BudgetScope.OVERALL
                            showBudgetForm = true
                        },
                    )
                }
            } else {
                item {
                    EmptyLedgerState(
                        title = "No overall budget for ${DateFormatters.formatShortMonth(uiState.selectedPeriod)}",
                        subtitle = "Spendly never fakes 0% progress for missing budgets. Create a monthly spending target when you're ready.",
                        actionLabel = "Create Monthly Budget",
                        onActionClick = {
                            editingBudget = null
                            defaultScope = BudgetScope.OVERALL
                            showBudgetForm = true
                        },
                    )
                }
            }

            // Category Budgets
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
                        Text("+ Category Limit")
                    }
                }
            }

            if (categorySummaries.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("No category budgets set", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Set limits for Dining, Groceries, or Shopping to receive proactive threshold alerts.",
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
                    ) {
                        Text("Edit Limit")
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
                    catTx.forEach { tx ->
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
                        )
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
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                String.format(java.util.Locale.US, "%.2f", editingBudget.limitMinor / 100.0)
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetScope.entries.forEach { s ->
                    val active = scope == s
                    Surface(
                        onClick = { scope = s },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = s.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                        label = { Text("Expense Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(14.dp),
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
                label = { Text("Spending Limit (${uiState.defaultCurrencyCode}) *") },
                placeholder = { Text("e.g., 500.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                isError = limitError != null,
                supportingText = limitError?.let { err -> { Text(err) } },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            Button(
                onClick = {
                    val minor = MoneyFormatter.parseMajorToMinor(limitText, uiState.defaultCurrencyCode)
                    if (minor == null || minor <= 0L) {
                        limitError = "Enter a valid limit greater than zero"
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
                    .height(52.dp)
                    .testTag("save_budget_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (editingBudget != null) "Update Budget" else "Save Budget")
            }

            if (onDelete != null) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
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
