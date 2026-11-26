package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AccountType
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.SearchableCurrencySheet
import com.example.ui.components.accountAccentPair
import com.example.ui.components.accountTypeIcon
import com.example.ui.components.categoryIconFor
import com.example.ui.components.pressBounce
import com.example.ui.theme.MoneyInputStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.categoryColorPair
import com.example.util.CurrencyCatalog
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    uiState: SpendlyUiState,
    initialType: TransactionType,
    initialAccountId: String?,
    editingTransaction: TransactionRecord?,
    onNavigateBack: () -> Unit,
    onCreateAccountFirst: () -> Unit,
    onSaveTransaction: (
        existingId: String?,
        type: TransactionType,
        amountMinor: Long,
        currencyCode: String,
        accountId: String,
        destinationAccountId: String?,
        categoryId: String?,
        description: String,
        merchant: String,
        dateMillis: Long,
        notes: String,
    ) -> Unit,
) {
    val activeAccounts = uiState.activeAccounts
    var selectedType by remember { mutableStateOf(editingTransaction?.type ?: initialType) }
    var amountText by remember {
        mutableStateOf(
            if (editingTransaction != null) {
                val spec = CurrencyCatalog.findByCode(editingTransaction.currencyCode)
                val divisor = Math.pow(10.0, spec.minorDecimals.toDouble())
                String.format(Locale.US, "%.${spec.minorDecimals}f", editingTransaction.amountMinor / divisor)
            } else ""
        )
    }
    var selectedAccountId by remember {
        mutableStateOf(
            editingTransaction?.accountId
                ?: initialAccountId
                ?: activeAccounts.firstOrNull()?.id
                ?: ""
        )
    }
    var destinationAccountId by remember {
        mutableStateOf(
            editingTransaction?.destinationAccountId
                ?: activeAccounts.getOrNull(1)?.id
                ?: ""
        )
    }
    var selectedCurrencyCode by remember {
        mutableStateOf(
            editingTransaction?.currencyCode
                ?: uiState.accountById(selectedAccountId)?.currencyCode
                ?: uiState.defaultCurrencyCode
        )
    }
    var selectedCategoryId by remember {
        mutableStateOf(
            editingTransaction?.categoryId
                ?: uiState.categories.firstOrNull { it.type == (editingTransaction?.type ?: initialType) }?.id
                ?: ""
        )
    }
    var description by remember { mutableStateOf(editingTransaction?.description ?: "") }
    var merchant by remember { mutableStateOf(editingTransaction?.merchant ?: "") }
    var notes by remember { mutableStateOf(editingTransaction?.notes ?: "") }
    var dateMillis by remember { mutableStateOf(editingTransaction?.dateMillis ?: System.currentTimeMillis()) }

    var amountError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountSheetForDest by remember { mutableStateOf<Boolean?>(null) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (editingTransaction == null) {
            delay(150)
            runCatching { amountFocus.requestFocus() }
        }
    }

    val filteredCategories = remember(uiState.categories, selectedType) {
        uiState.categories.filter { it.isActive && it.type == selectedType }
    }
    val selectedAccount = uiState.accountById(selectedAccountId)
    val destinationAccount = uiState.accountById(destinationAccountId)
    val selectedCategory = uiState.categoryById(selectedCategoryId)
    val currencySpec = CurrencyCatalog.findByCode(selectedCurrencyCode)
    val semantic = SpendlyTheme.semantic

    val (activeAccent, activeTintBg) = when (selectedType) {
        TransactionType.EXPENSE -> semantic.expense to semantic.expenseContainer
        TransactionType.INCOME -> semantic.income to semantic.incomeContainer
        TransactionType.TRANSFER -> semantic.transfer to semantic.transferContainer
    }
    val onActiveAccent = if (semantic.isDark) Color(0xFF091411) else Color.White

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        text = if (editingTransaction != null) "Edit Transaction" else "New Transaction",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pressBounce().testTag("add_tx_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (activeAccounts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(
                                onClick = {
                                    val parsedMinor = MoneyFormatter.parseMajorToMinor(amountText, selectedCurrencyCode)
                                    amountError = when {
                                        amountText.isBlank() -> "Enter an amount"
                                        parsedMinor == null -> "Check decimal precision (max ${currencySpec.minorDecimals} decimals for ${currencySpec.code})"
                                        parsedMinor <= 0L -> "Amount must be greater than zero"
                                        else -> null
                                    }
                                    accountError = when {
                                        selectedAccount == null -> "Select an account"
                                        selectedType == TransactionType.TRANSFER && destinationAccount == null -> "Select a destination account"
                                        selectedType == TransactionType.TRANSFER && selectedAccountId == destinationAccountId -> "Source and destination accounts must differ"
                                        else -> null
                                    }
                                    categoryError = if (selectedType != TransactionType.TRANSFER && selectedCategory == null) {
                                        "Select a category"
                                    } else null
                                    descriptionError = if (description.isBlank() && merchant.isBlank()) {
                                        "Add a short description or merchant name"
                                    } else null

                                    if (amountError == null && accountError == null && categoryError == null && descriptionError == null && parsedMinor != null) {
                                        isSaving = true
                                        scope.launch {
                                            delay(120)
                                            onSaveTransaction(
                                                editingTransaction?.id,
                                                selectedType,
                                                parsedMinor,
                                                selectedCurrencyCode,
                                                selectedAccountId,
                                                if (selectedType == TransactionType.TRANSFER) destinationAccountId else null,
                                                if (selectedType == TransactionType.TRANSFER) null else selectedCategoryId,
                                                description.ifBlank { merchant },
                                                merchant,
                                                dateMillis,
                                                notes,
                                            )
                                            isSaving = false
                                        }
                                    }
                                },
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeAccent,
                                    contentColor = onActiveAccent,
                                ),
                                modifier = Modifier
                                    .widthIn(max = 600.dp)
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .pressBounce(enabled = !isSaving, pressScale = 0.96f)
                                    .testTag("save_transaction_button"),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = onActiveAccent,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Saving...")
                                } else {
                                    Text(
                                        text = if (editingTransaction != null) "Update Transaction" else "Save ${selectedType.label}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (activeAccounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 460.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text("Create an account first", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Transactions are always recorded against a manual account (such as Cash Wallet, Bank Account, or Credit Card).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onCreateAccountFirst,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .pressBounce(pressScale = 0.96f)
                            .testTag("add_tx_create_account_cta"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Create Account")
                    }
                }
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Segmented Type Picker
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, semantic.surfaceBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TransactionType.entries.forEach { type ->
                            val selected = selectedType == type
                            val pillColor = when (type) {
                                TransactionType.EXPENSE -> semantic.expense
                                TransactionType.INCOME -> semantic.income
                                TransactionType.TRANSFER -> semantic.transfer
                            }
                            Surface(
                                onClick = {
                                    selectedType = type
                                    accountError = null
                                    categoryError = null
                                    if (type != TransactionType.TRANSFER) {
                                        val validCat = uiState.categories.firstOrNull { it.type == type }
                                        if (validCat != null && uiState.categoryById(selectedCategoryId)?.type != type) {
                                            selectedCategoryId = validCat.id
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .pressBounce(pressScale = 0.94f)
                                    .testTag("tx_type_${type.name.lowercase()}"),
                                shape = RoundedCornerShape(9.dp),
                                color = if (selected) pillColor else Color.Transparent,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = type.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) onActiveAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount Entry Card
                Surface(
                    shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (amountError != null) 1.5.dp else 1.dp,
                        if (amountError != null) MaterialTheme.colorScheme.error else semantic.surfaceBorder,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${selectedType.label} Amount",
                                style = MaterialTheme.typography.labelLarge,
                                color = activeAccent,
                            )
                            Surface(
                                onClick = { showCurrencySheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .pressBounce(pressScale = 0.93f)
                                    .testTag("tx_currency_chip"),
                                shape = RoundedCornerShape(999.dp),
                                color = activeTintBg,
                            ) {
                                Text(
                                    text = "${currencySpec.code} (${currencySpec.symbol})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = activeAccent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                amountError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(amountFocus)
                                .testTag("tx_amount_input"),
                            textStyle = MoneyInputStyle.copy(color = activeAccent),
                            placeholder = {
                                Text(
                                    text = if (currencySpec.minorDecimals == 0) "0" else "0.00",
                                    style = MoneyInputStyle,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                )
                            },
                            prefix = {
                                Text(
                                    text = currencySpec.symbol,
                                    style = MoneyInputStyle,
                                    color = activeAccent.copy(alpha = 0.75f),
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = amountError != null,
                            shape = RoundedCornerShape(SpendlyTokens.InputRadius),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = semantic.surfaceBorder,
                                focusedBorderColor = activeAccent,
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
                                        val current = amountText.trim().replace(",", "").toDoubleOrNull() ?: 0.0
                                        val next = current + addVal
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
                                        .testTag("tx_quick_chip_$addVal"),
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, semantic.surfaceBorder),
                                ) {
                                    Text(
                                        text = "+${currencySpec.symbol}$addVal",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    )
                                }
                            }
                            if (amountText.isNotEmpty()) {
                                Surface(
                                    onClick = { amountText = "" },
                                    modifier = Modifier.minimumInteractiveComponentSize().pressBounce(pressScale = 0.92f),
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                ) {
                                    Text(
                                        text = "Clear",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    )
                                }
                            }
                        }

                        if (amountError != null) {
                            Text(
                                text = amountError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                // 1-Tap Quick Category Chips for Expense & Income
                if (selectedType != TransactionType.TRANSFER && filteredCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filteredCategories.take(6).forEach { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            val (catTint, catBg) = categoryColorPair(cat.iconKey, semantic.isDark)
                            Surface(
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryError = null
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .pressBounce(pressScale = 0.94f)
                                    .testTag("quick_cat_${cat.id}"),
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) catBg else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) catTint else semantic.surfaceBorder,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = categoryIconFor(cat.iconKey),
                                        contentDescription = null,
                                        tint = if (isSelected) catTint else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        ),
                                        color = if (isSelected) catTint else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                // Grouped Category, Account & Date Inset Sheet
                LedgerGroupCard {
                    if (selectedType != TransactionType.TRANSFER) {
                        val (catTint, catBg) = categoryColorPair(selectedCategory?.iconKey, semantic.isDark)
                        PickerFieldRow(
                            label = "Category",
                            value = selectedCategory?.name ?: "Choose category",
                            leadingIcon = categoryIconFor(selectedCategory?.iconKey),
                            iconTint = catTint,
                            iconBg = catBg,
                            errorText = categoryError,
                            onClick = { showCategorySheet = true },
                            testTag = "tx_category_picker",
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    PickerFieldRow(
                        label = if (selectedType == TransactionType.TRANSFER) "From Account" else "Account",
                        value = selectedAccount?.let { "${it.name} (${it.currencyCode})" } ?: "Choose account",
                        leadingIcon = selectedAccount?.let { accountTypeIcon(it.type) } ?: Icons.Rounded.AccountBalanceWallet,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        errorText = if (selectedType != TransactionType.TRANSFER) accountError else null,
                        onClick = { showAccountSheetForDest = false },
                        testTag = "tx_account_picker",
                    )

                    if (selectedType == TransactionType.TRANSFER) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        PickerFieldRow(
                            label = "To Account",
                            value = destinationAccount?.let { "${it.name} (${it.currencyCode})" } ?: "Choose destination account",
                            leadingIcon = destinationAccount?.let { accountTypeIcon(it.type) } ?: Icons.Rounded.SwapVert,
                            iconTint = semantic.transfer,
                            iconBg = semantic.transferContainer,
                            errorText = accountError,
                            onClick = { showAccountSheetForDest = true },
                            testTag = "tx_dest_account_picker",
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    PickerFieldRow(
                        label = "Date",
                        value = DateFormatters.formatDateGroupHeader(dateMillis),
                        leadingIcon = Icons.Rounded.CalendarToday,
                        iconTint = semantic.warning,
                        iconBg = semantic.warningContainer,
                        errorText = null,
                        onClick = { showDatePicker = true },
                        testTag = "tx_date_picker",
                    )
                }

                if (selectedType == TransactionType.TRANSFER &&
                    destinationAccount?.type == AccountType.CREDIT_CARD &&
                    selectedAccountId != destinationAccountId
                ) {
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "Credit card repayment: reduces card balance without counting as a second shopping expense.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                // Details Card
                LedgerGroupCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                if (it.length <= 160) {
                                    description = it
                                    descriptionError = null
                                }
                            },
                            label = { Text("Description *") },
                            placeholder = { Text("e.g., Weekly groceries, Coffee, Rent") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_description_input"),
                            singleLine = true,
                            isError = descriptionError != null,
                            supportingText = descriptionError?.let { err -> { Text(err) } },
                            shape = RoundedCornerShape(SpendlyTokens.InputRadius),
                        )

                        OutlinedTextField(
                            value = merchant,
                            onValueChange = {
                                if (it.length <= 100) {
                                    merchant = it
                                    descriptionError = null
                                }
                            },
                            label = { Text("Merchant or Payee (Optional)") },
                            placeholder = { Text("e.g., Trader Joe's, Landlord") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_merchant_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(SpendlyTokens.InputRadius),
                        )

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { if (it.length <= 500) notes = it },
                            label = { Text("Note (Optional)") },
                            placeholder = { Text("Reference details, split info, receipt note...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tx_notes_input"),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(SpendlyTokens.InputRadius),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Select ${selectedType.label} Category", style = MaterialTheme.typography.headlineSmall)
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredCategories, key = { it.id }) { cat ->
                        val active = cat.id == selectedCategoryId
                        val (catTint, catBg) = categoryColorPair(cat.iconKey, semantic.isDark)
                        Surface(
                            onClick = {
                                selectedCategoryId = cat.id
                                categoryError = null
                                showCategorySheet = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressBounce(pressScale = 0.97f)
                                .testTag("pick_cat_${cat.id}"),
                            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = if (active) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
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
                                            .background(catBg),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(categoryIconFor(cat.iconKey), contentDescription = null, tint = catTint, modifier = Modifier.size(18.dp))
                                    }
                                    Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                }
                                if (active) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAccountSheetForDest != null) {
        val isDest = showAccountSheetForDest == true
        ModalBottomSheet(
            onDismissRequest = { showAccountSheetForDest = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = if (isDest) "Select Destination Account" else "Select Account",
                    style = MaterialTheme.typography.headlineSmall,
                )
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(activeAccounts, key = { it.id }) { acc ->
                        val currentId = if (isDest) destinationAccountId else selectedAccountId
                        val active = acc.id == currentId
                        val (accTint, accBg) = accountAccentPair(acc.type, semantic.isDark)
                        Surface(
                            onClick = {
                                if (isDest) {
                                    destinationAccountId = acc.id
                                } else {
                                    selectedAccountId = acc.id
                                    selectedCurrencyCode = acc.currencyCode
                                }
                                accountError = null
                                showAccountSheetForDest = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressBounce(pressScale = 0.97f)
                                .testTag("pick_acc_${acc.id}"),
                            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = if (active) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
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
                                            .background(accBg),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(accountTypeIcon(acc.type), contentDescription = null, tint = accTint, modifier = Modifier.size(18.dp))
                                    }
                                    Column {
                                        Text(acc.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${acc.type.displayName} • ${acc.currencyCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (active) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCurrencySheet) {
        SearchableCurrencySheet(
            currentCurrencyCode = selectedCurrencyCode,
            onDismiss = { showCurrencySheet = false },
            onConfirmCurrency = {
                selectedCurrencyCode = it.code
                showCurrencySheet = false
            },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    },
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun PickerFieldRow(
    label: String,
    value: String,
    leadingIcon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    errorText: String?,
    onClick: () -> Unit,
    testTag: String,
) {
    Column {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .pressBounce(pressScale = 0.97f)
                .testTag(testTag),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Column {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 68.dp, bottom = 8.dp),
            )
        }
    }
}
