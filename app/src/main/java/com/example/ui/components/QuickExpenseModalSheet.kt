package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.TransactionType
import com.example.ui.theme.MoneyInputStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.categoryColorPair
import com.example.util.CurrencyCatalog
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickExpenseModalSheet(
    uiState: SpendlyUiState,
    onDismiss: () -> Unit,
    onSaveExpense: (
        amountMinor: Long,
        categoryId: String,
        dateMillis: Long,
        accountId: String?,
        note: String,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseCategories = remember(uiState.categories) {
        uiState.categories.filter { it.isActive && it.type == TransactionType.EXPENSE }
    }
    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember(expenseCategories) {
        mutableStateOf(expenseCategories.firstOrNull()?.id ?: "cat_groceries")
    }
    var selectedAccountId by remember(uiState.activeAccounts) {
        mutableStateOf(uiState.activeAccounts.firstOrNull()?.id)
    }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var noteText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val selectedAccount = selectedAccountId?.let { uiState.accountById(it) }
    val currencyCode = selectedAccount?.currencyCode ?: uiState.defaultCurrencyCode
    val currencySpec = CurrencyCatalog.findByCode(currencyCode)
    val semantic = SpendlyTheme.semantic

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Log Expense",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Enter amount, category, date, and account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.pressBounce().testTag("quick_expense_close_button"),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, if (amountError != null) MaterialTheme.colorScheme.error else semantic.surfaceBorder),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Amount (${currencySpec.code})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            amountError = null
                        },
                        placeholder = {
                            Text(
                                text = if (currencySpec.minorDecimals == 0) "0" else "0.00",
                                style = MoneyInputStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        },
                        prefix = {
                            Text(
                                text = "${currencySpec.symbol} ",
                                style = MoneyInputStyle,
                                color = semantic.expense,
                            )
                        },
                        textStyle = MoneyInputStyle.copy(color = semantic.expense),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = amountError != null,
                        supportingText = amountError?.let { err -> { Text(err) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_expense_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = semantic.surfaceBorder,
                            focusedBorderColor = semantic.expense,
                        ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(10, 25, 50, 100).forEach { addVal ->
                            Surface(
                                onClick = {
                                    val cur = amountText.trim().replace(",", "").toDoubleOrNull() ?: 0.0
                                    val next = cur + addVal
                                    amountText = if (currencySpec.minorDecimals == 0) {
                                        next.toLong().toString()
                                    } else {
                                        String.format(Locale.US, "%.${currencySpec.minorDecimals}f", next)
                                    }
                                    amountError = null
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .pressBounce(pressScale = 0.92f)
                                    .testTag("quick_expense_chip_$addVal"),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, semantic.surfaceBorder),
                            ) {
                                Text(
                                    text = "+${currencySpec.symbol}$addVal",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = semantic.expense,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                        if (amountText.isNotEmpty()) {
                            Surface(
                                onClick = { amountText = "" },
                                modifier = Modifier.minimumInteractiveComponentSize().pressBounce(pressScale = 0.92f),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, semantic.surfaceBorder),
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    expenseCategories.forEach { cat ->
                        val selected = selectedCategoryId == cat.id
                        val (catTint, catBg) = categoryColorPair(cat.iconKey, semantic.isDark)
                        val containerColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            label = "catChipBg",
                        )
                        Surface(
                            onClick = { selectedCategoryId = cat.id },
                            shape = RoundedCornerShape(999.dp),
                            color = containerColor,
                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.pressBounce(pressScale = 0.94f).testTag("quick_expense_cat_${cat.id}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(catBg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = categoryIconFor(cat.iconKey),
                                        contentDescription = null,
                                        tint = catTint,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .pressBounce(pressScale = 0.96f)
                        .testTag("quick_expense_date_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Select date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = DateFormatters.formatShortDate(dateMillis),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { if (uiState.activeAccounts.isNotEmpty()) showAccountMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressBounce(pressScale = 0.96f)
                            .testTag("quick_expense_account_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = "Account",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = selectedAccount?.name ?: "Cash Wallet",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false },
                    ) {
                        uiState.activeAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.currencyCode})") },
                                trailingIcon = {
                                    if (acc.id == selectedAccountId) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    selectedAccountId = acc.id
                                    showAccountMenu = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 160) noteText = it },
                label = { Text("Merchant or note (optional)") },
                placeholder = { Text("e.g., Trader Joe's, Lunch") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_expense_note_input"),
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = {
                    val minor = MoneyFormatter.parseMajorToMinor(amountText, currencyCode)
                    if (amountText.isBlank() || minor == null || minor <= 0L) {
                        amountError = "Enter an amount greater than zero"
                    } else {
                        onSaveExpense(minor, selectedCategoryId, dateMillis, selectedAccountId, noteText)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = semantic.expense,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressBounce(pressScale = 0.96f)
                    .testTag("quick_expense_save_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save Expense",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    },
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
