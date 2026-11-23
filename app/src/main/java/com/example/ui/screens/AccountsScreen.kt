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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.TransactionRecord
import com.example.ui.components.AccountLedgerCard
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.SearchableCurrencySheet
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.util.CurrencyCatalog
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    uiState: SpendlyUiState,
    onNavigateBack: () -> Unit,
    onOpenAccountDetail: (String) -> Unit,
    onSaveAccount: (
        existingId: String?,
        name: String,
        type: AccountType,
        institutionName: String,
        cardNetwork: String,
        lastFour: String,
        currencyCode: String,
        openingBalanceMinor: Long,
        creditLimitMinor: Long?,
        description: String,
    ) -> Unit,
) {
    var showArchived by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountRecord?>(null) }
    var showAccountForm by remember { mutableStateOf(false) }

    val semantic = SpendlyTheme.semantic
    val baseCode = uiState.defaultCurrencyCode
    val hide = uiState.privacyHideBalances

    val displayedSummaries = remember(uiState.accountSummaries, showArchived) {
        uiState.accountSummaries.filter { it.account.isArchived == showArchived }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Accounts & Cards") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("accounts_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            editingAccount = null
                            showAccountForm = true
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("add_account_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.padding(2.dp))
                        Text("Add")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxSize()
                    .testTag("accounts_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Summary Strip
                item {
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CALCULATED ASSETS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.totalAssetsBaseMinor, baseCode),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "CREDIT OUTSTANDING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semantic.warning,
                                    )
                                    Text(
                                        text = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.totalCreditOutstandingBaseMinor, baseCode),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (uiState.totalCreditOutstandingBaseMinor > 0) semantic.warning else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Balances are calculated from manual entries & opening balances — not bank-verified.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Active / Archived toggle
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showArchived,
                            onClick = { showArchived = false },
                            label = { Text("Active (${uiState.activeAccounts.size})") },
                            modifier = Modifier.testTag("accounts_tab_active"),
                        )
                        FilterChip(
                            selected = showArchived,
                            onClick = { showArchived = true },
                            label = { Text("Archived (${uiState.archivedAccounts.size})") },
                            modifier = Modifier.testTag("accounts_tab_archived"),
                        )
                    }
                }

                if (displayedSummaries.isEmpty()) {
                    item {
                        EmptyLedgerState(
                            title = if (showArchived) "No archived accounts" else "No accounts yet",
                            subtitle = if (showArchived) {
                                "Archived accounts hide from new transaction pickers while keeping historical records intact."
                            } else {
                                "Add a bank account, debit/credit card, cash wallet, or digital wallet manually."
                            },
                            actionLabel = if (showArchived) "View Active Accounts" else "Add Account",
                            onActionClick = {
                                if (showArchived) showArchived = false
                                else {
                                    editingAccount = null
                                    showAccountForm = true
                                }
                            },
                        )
                    }
                } else {
                    items(displayedSummaries, key = { it.account.id }) { summary ->
                        AccountLedgerCard(
                            summary = summary,
                            defaultCurrencyCode = baseCode,
                            hideBalances = hide,
                            onClick = { onOpenAccountDetail(summary.account.id) },
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showAccountForm) {
        AccountFormSheet(
            initialCurrency = uiState.defaultCurrencyCode,
            editingAccount = editingAccount,
            onDismiss = { showAccountForm = false },
            onSave = { name, type, institution, network, lastFour, currency, openingMinor, creditLimitMinor, desc ->
                onSaveAccount(
                    editingAccount?.id,
                    name,
                    type,
                    institution,
                    network,
                    lastFour,
                    currency,
                    openingMinor,
                    creditLimitMinor,
                    desc,
                )
                showAccountForm = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    uiState: SpendlyUiState,
    onNavigateBack: () -> Unit,
    onRecordTransactionForAccount: (String) -> Unit,
    onSaveAccount: (
        existingId: String?,
        name: String,
        type: AccountType,
        institutionName: String,
        cardNetwork: String,
        lastFour: String,
        currencyCode: String,
        openingBalanceMinor: Long,
        creditLimitMinor: Long?,
        description: String,
    ) -> Unit,
    onToggleArchive: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onOpenTransactionDetail: (TransactionRecord) -> Unit,
) {
    val summary = uiState.accountSummaries.firstOrNull { it.account.id == accountId }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (summary == null) {
        LaunchedEffectFallback(onNavigateBack)
        return
    }

    val account = summary.account
    val accountTransactions = remember(uiState.transactions, accountId) {
        uiState.transactions
            .filter { it.accountId == accountId || it.destinationAccountId == accountId }
            .sortedByDescending { it.dateMillis }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(account.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("account_detail_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }, modifier = Modifier.testTag("account_detail_edit")) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit account")
                    }
                    IconButton(
                        onClick = { onToggleArchive(account.id) },
                        modifier = Modifier.testTag("account_detail_archive"),
                    ) {
                        Icon(
                            imageVector = if (account.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = if (account.isArchived) "Unarchive account" else "Archive account",
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("account_detail_delete"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete account",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AccountLedgerCard(
                        summary = summary,
                        defaultCurrencyCode = uiState.defaultCurrencyCode,
                        hideBalances = uiState.privacyHideBalances,
                        onClick = {},
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onRecordTransactionForAccount(account.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("account_detail_add_tx"),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !account.isArchived,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.padding(3.dp))
                            Text(if (account.isArchived) "Account Archived" else "Record Transaction")
                        }
                    }
                }

                if (account.description.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = account.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Account History (${accountTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                if (accountTransactions.isEmpty()) {
                    item {
                        EmptyLedgerState(
                            title = "No activity for this account",
                            subtitle = "Transactions recorded against ${account.name} will appear here.",
                            actionLabel = "Add Transaction",
                            onActionClick = { onRecordTransactionForAccount(account.id) },
                        )
                    }
                } else {
                    items(accountTransactions, key = { it.id }) { tx ->
                        TransactionLedgerRow(
                            transaction = tx,
                            category = uiState.categoryById(tx.categoryId),
                            accountName = uiState.accountById(tx.accountId)?.name ?: "Account",
                            destinationAccountName = tx.destinationAccountId?.let { uiState.accountById(it)?.name },
                            defaultCurrencyCode = uiState.defaultCurrencyCode,
                            convertedBaseMinor = uiState.convertToBase(tx.amountMinor, tx.currencyCode),
                            hideBalances = uiState.privacyHideBalances,
                            onClick = { onOpenTransactionDetail(tx) },
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        AccountFormSheet(
            initialCurrency = account.currencyCode,
            editingAccount = account,
            onDismiss = { showEditSheet = false },
            onSave = { name, type, institution, network, lastFour, currency, openingMinor, creditLimitMinor, desc ->
                onSaveAccount(
                    account.id,
                    name,
                    type,
                    institution,
                    network,
                    lastFour,
                    currency,
                    openingMinor,
                    creditLimitMinor,
                    desc,
                )
                showEditSheet = false
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${account.name}?") },
            text = {
                Text(
                    "Deleting this account also removes its ${accountTransactions.size} linked transaction(s). If you want to preserve historical reports, choose Archive instead."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount(account.id)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteConfirm = false
                        onToggleArchive(account.id)
                    },
                ) {
                    Text("Archive Instead")
                }
            },
        )
    }
}

@Composable
private fun LaunchedEffectFallback(onNavigateBack: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { onNavigateBack() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    initialCurrency: String,
    editingAccount: AccountRecord?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: AccountType,
        institution: String,
        network: String,
        lastFour: String,
        currencyCode: String,
        openingBalanceMinor: Long,
        creditLimitMinor: Long?,
        description: String,
    ) -> Unit,
) {
    var nickname by remember { mutableStateOf(editingAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(editingAccount?.type ?: AccountType.BANK_ACCOUNT) }
    var institution by remember { mutableStateOf(editingAccount?.institutionName ?: "") }
    var cardNetwork by remember { mutableStateOf(editingAccount?.cardNetwork ?: "") }
    var lastFour by remember { mutableStateOf(editingAccount?.lastFour ?: "") }
    var currencyCode by remember { mutableStateOf(editingAccount?.currencyCode ?: initialCurrency) }
    var openingBalanceText by remember {
        mutableStateOf(
            if (editingAccount != null && editingAccount.openingBalanceMinor != 0L) {
                val spec = CurrencyCatalog.findByCode(editingAccount.currencyCode)
                val div = Math.pow(10.0, spec.minorDecimals.toDouble())
                String.format(java.util.Locale.US, "%.${spec.minorDecimals}f", editingAccount.openingBalanceMinor / div)
            } else "0.00"
        )
    }
    var creditLimitText by remember {
        mutableStateOf(
            editingAccount?.creditLimitMinor?.let {
                val spec = CurrencyCatalog.findByCode(editingAccount.currencyCode)
                val div = Math.pow(10.0, spec.minorDecimals.toDouble())
                String.format(java.util.Locale.US, "%.${spec.minorDecimals}f", it / div)
            } ?: ""
        )
    }
    var description by remember { mutableStateOf(editingAccount?.description ?: "") }

    var typeDropdownOpen by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var lastFourError by remember { mutableStateOf<String?>(null) }
    var balanceError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editingAccount != null) "Edit Account" else "Add Manual Account",
                style = MaterialTheme.typography.headlineSmall,
            )

            // Mandatory credential safety notice
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Manual record only. Never enter full card numbers, CVV/CVC, PINs, or online banking passwords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    nameError = null
                },
                label = { Text("Account Nickname *") },
                placeholder = { Text("e.g., Everyday Checking, Cash Wallet") },
                modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                isError = nameError != null,
                supportingText = nameError?.let { err -> { Text(err) } },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            ExposedDropdownMenuBox(
                expanded = typeDropdownOpen,
                onExpandedChange = { typeDropdownOpen = !typeDropdownOpen },
            ) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("account_type_dropdown"),
                    shape = RoundedCornerShape(14.dp),
                )
                ExposedDropdownMenu(
                    expanded = typeDropdownOpen,
                    onDismissRequest = { typeDropdownOpen = false },
                ) {
                    AccountType.entries.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.displayName) },
                            onClick = {
                                selectedType = t
                                typeDropdownOpen = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Institution or Provider (Optional)") },
                placeholder = { Text("e.g., Summit National, Wise, PayPal") },
                modifier = Modifier.fillMaxWidth().testTag("account_institution_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            if (selectedType.supportsCardDetails) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = cardNetwork,
                        onValueChange = { cardNetwork = it },
                        label = { Text("Network") },
                        placeholder = { Text("Visa / MC") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = lastFour,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                lastFour = input
                                lastFourError = null
                            }
                        },
                        label = { Text("Last 4 Digits") },
                        placeholder = { Text("4821") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("account_last_four_input"),
                        isError = lastFourError != null,
                        supportingText = lastFourError?.let { err -> { Text(err) } },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedButton(
                    onClick = { showCurrencyPicker = true },
                    modifier = Modifier
                        .weight(0.42f)
                        .height(56.dp)
                        .padding(top = 6.dp)
                        .testTag("account_currency_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(currencyCode)
                }
                OutlinedTextField(
                    value = openingBalanceText,
                    onValueChange = {
                        openingBalanceText = it
                        balanceError = null
                    },
                    label = { Text(if (selectedType.isLiability) "Opening Outstanding" else "Opening Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.58f).testTag("account_opening_balance_input"),
                    isError = balanceError != null,
                    supportingText = balanceError?.let { err -> { Text(err) } },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }

            if (selectedType == AccountType.CREDIT_CARD) {
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("Credit Limit (Optional)") },
                    placeholder = { Text("e.g., 5000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )

            Button(
                onClick = {
                    if (nickname.isBlank()) nameError = "Enter an account nickname"
                    if (lastFour.isNotEmpty() && lastFour.length != 4) lastFourError = "Enter 4 digits or leave blank"
                    val openingMinor = if (openingBalanceText.isBlank()) 0L
                    else MoneyFormatter.parseMajorToMinor(openingBalanceText, currencyCode)
                    if (openingMinor == null) balanceError = "Invalid opening amount"
                    val limitMinor = if (creditLimitText.isBlank()) null
                    else MoneyFormatter.parseMajorToMinor(creditLimitText, currencyCode)

                    if (nameError == null && lastFourError == null && balanceError == null && openingMinor != null) {
                        onSave(
                            nickname,
                            selectedType,
                            institution,
                            cardNetwork,
                            lastFour,
                            currencyCode,
                            openingMinor,
                            limitMinor,
                            description,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_account_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (editingAccount != null) "Save Changes" else "Create Account")
            }
        }
    }

    if (showCurrencyPicker) {
        SearchableCurrencySheet(
            currentCurrencyCode = currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onConfirmCurrency = {
                currencyCode = it.code
                showCurrencyPicker = false
            },
        )
    }
}
