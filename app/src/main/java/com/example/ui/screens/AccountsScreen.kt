package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.TransactionRecord
import com.example.ui.components.AccountLedgerCard
import com.example.ui.components.CardFinishPreset
import com.example.ui.components.EmptyLedgerState
import com.example.ui.components.Interactive3DWalletCard
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.SearchableCurrencySheet
import com.example.ui.components.TransactionLedgerRow
import com.example.ui.components.WalletDeckStage
import com.example.ui.components.pressBounce
import com.example.ui.components.touchSpringCard
import com.example.ui.theme.MoneyHeroStyle
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.util.CardArtStore
import com.example.util.CurrencyCatalog
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlinx.coroutines.launch

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
    ) -> String,
) {
    var showArchived by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountRecord?>(null) }
    var showAccountForm by remember { mutableStateOf(false) }

    val semantic = SpendlyTheme.semantic
    val baseCode = uiState.defaultCurrencyCode
    val hide = uiState.privacyHideBalances
    val netPositionMinor = uiState.totalAssetsBaseMinor - uiState.totalCreditOutstandingBaseMinor

    val displayedSummaries = remember(uiState.accountSummaries, showArchived) {
        uiState.accountSummaries.filter { it.account.isArchived == showArchived }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        "Wallet & Accounts",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pressBounce().testTag("accounts_back"),
                    ) {
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
                            .pressBounce(pressScale = 0.94f)
                            .testTag("add_account_button"),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.padding(2.dp))
                        Text("Add Card")
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Surface(
                        modifier = Modifier.touchSpringCard(pressScale = 0.982f, maxTiltDeg = 5f, hapticOnPress = true),
                        shape = RoundedCornerShape(SpendlyTokens.HeroRadius),
                        color = semantic.heroSurface,
                        border = BorderStroke(1.dp, semantic.heroGold.copy(alpha = 0.28f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Net manual position",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = semantic.onHeroSurfaceMuted,
                                )
                                Text(
                                    text = if (hide) "••••••" else MoneyFormatter.formatMinor(netPositionMinor, baseCode),
                                    style = MoneyHeroStyle,
                                    color = semantic.onHeroSurface,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = semantic.onHeroWell,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Assets", style = MaterialTheme.typography.labelSmall, color = semantic.heroAccent)
                                        Text(
                                            text = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.totalAssetsBaseMinor, baseCode),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = semantic.onHeroSurface,
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = semantic.onHeroWell,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Credit due", style = MaterialTheme.typography.labelSmall, color = semantic.heroWarn)
                                        Text(
                                            text = if (hide) "••••" else MoneyFormatter.formatMinor(uiState.totalCreditOutstandingBaseMinor, baseCode),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = semantic.onHeroSurface,
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = semantic.onHeroSurfaceMuted,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = "Customize each card with real photos, bank logos & network styles.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.onHeroSurfaceMuted,
                                )
                            }
                        }
                    }
                }

                item {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showArchived,
                            onClick = { showArchived = false },
                            label = { Text("Active (${uiState.activeAccounts.size})") },
                            shape = RoundedCornerShape(999.dp),
                            colors = chipColors,
                            modifier = Modifier.pressBounce(pressScale = 0.94f).testTag("accounts_tab_active"),
                        )
                        FilterChip(
                            selected = showArchived,
                            onClick = { showArchived = true },
                            label = { Text("Archived (${uiState.archivedAccounts.size})") },
                            shape = RoundedCornerShape(999.dp),
                            colors = chipColors,
                            modifier = Modifier.pressBounce(pressScale = 0.94f).testTag("accounts_tab_archived"),
                        )
                    }
                }

                if (displayedSummaries.isEmpty()) {
                    item {
                        EmptyLedgerState(
                            title = if (showArchived) "No archived accounts" else "No cards or accounts yet",
                            subtitle = if (showArchived) {
                                "Archived accounts hide from new transaction pickers while keeping historical records intact."
                            } else {
                                "Add a Visa, Mastercard, Amex, debit card, bank account, or cash wallet with your own photo and logo."
                            },
                            actionLabel = if (showArchived) "View Active Accounts" else "Add Card or Account",
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
                val savedId = onSaveAccount(
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
                savedId
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
    ) -> String,
    onToggleArchive: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onOpenTransactionDetail: (TransactionRecord) -> Unit,
) {
    val summary = uiState.accountSummaries.firstOrNull { it.account.id == accountId }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var hasNavigatedBack by remember { mutableStateOf(false) }
    val safeNavigateBack: () -> Unit = {
        if (!hasNavigatedBack) {
            hasNavigatedBack = true
            onNavigateBack()
        }
    }
    val semantic = SpendlyTheme.semantic
    val context = LocalContext.current
    val artVersion by CardArtStore.version.collectAsStateWithLifecycle()

    if (summary == null) {
        LaunchedEffectFallback(safeNavigateBack)
        return
    }

    val account = summary.account
    val artPrefs = remember(account.id, artVersion) {
        CardArtStore.getCardArt(context, account.id)
    }
    val accountTransactions = remember(uiState.transactions, accountId) {
        uiState.transactions
            .filter { it.accountId == accountId || it.destinationAccountId == accountId }
            .sortedByDescending { it.dateMillis }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        account.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = safeNavigateBack,
                        modifier = Modifier.pressBounce().testTag("account_detail_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.pressBounce().testTag("account_detail_edit"),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit account")
                    }
                    IconButton(
                        onClick = { onToggleArchive(account.id) },
                        modifier = Modifier.pressBounce().testTag("account_detail_archive"),
                    ) {
                        Icon(
                            imageVector = if (account.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = if (account.isArchived) "Unarchive account" else "Archive account",
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.pressBounce().testTag("account_detail_delete"),
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    WalletDeckStage {
                        Interactive3DWalletCard(
                            account = account,
                            balanceMinor = summary.calculatedBalanceMinor,
                            hideBalances = uiState.privacyHideBalances,
                            artPrefs = artPrefs,
                            enableDragTilt = true,
                            secondaryHint = "Drag to tilt • Tap to flip",
                            testTag = "account_detail_3d_card",
                        )
                    }
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
                                .height(50.dp)
                                .pressBounce(enabled = !account.isArchived, pressScale = 0.95f)
                                .testTag("account_detail_add_tx"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !account.isArchived,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.padding(3.dp))
                            Text(if (account.isArchived) "Archived" else "Record Entry")
                        }
                        OutlinedButton(
                            onClick = { showEditSheet = true },
                            modifier = Modifier.height(50.dp).pressBounce(pressScale = 0.95f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.padding(3.dp))
                            Text("Customize Card")
                        }
                    }
                }

                if (account.description.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, semantic.surfaceBorder),
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
                            subtitle = "Transactions recorded against ${account.name} appear here.",
                            actionLabel = "Add Transaction",
                            onActionClick = { onRecordTransactionForAccount(account.id) },
                        )
                    }
                } else {
                    item {
                        LedgerGroupCard {
                            accountTransactions.forEachIndexed { idx, tx ->
                                TransactionLedgerRow(
                                    transaction = tx,
                                    category = uiState.categoryById(tx.categoryId),
                                    accountName = uiState.accountById(tx.accountId)?.name ?: "Account",
                                    destinationAccountName = tx.destinationAccountId?.let { uiState.accountById(it)?.name },
                                    defaultCurrencyCode = uiState.defaultCurrencyCode,
                                    convertedBaseMinor = uiState.convertToBase(tx.amountMinor, tx.currencyCode),
                                    hideBalances = uiState.privacyHideBalances,
                                    onClick = { onOpenTransactionDetail(tx) },
                                    grouped = true,
                                )
                                if (idx < accountTransactions.lastIndex) {
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

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showEditSheet) {
        AccountFormSheet(
            initialCurrency = account.currencyCode,
            editingAccount = account,
            onDismiss = { showEditSheet = false },
            onSave = { name, type, institution, network, lastFour, currency, openingMinor, creditLimitMinor, desc ->
                val savedId = onSaveAccount(
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
                savedId
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${account.name}?") },
            text = {
                Text(
                    "Deleting this account also removes its ${accountTransactions.size} linked transaction(s). Choose Archive if you want to keep historical reports."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount(account.id)
                        safeNavigateBack()
                    },
                    modifier = Modifier
                        .pressBounce(pressScale = 0.94f)
                        .testTag("account_delete_confirm_button"),
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
                    modifier = Modifier
                        .pressBounce(pressScale = 0.94f)
                        .testTag("account_archive_instead_button"),
                ) {
                    Text("Archive Instead")
                }
            },
        )
    }
}

@Composable
private fun LaunchedEffectFallback(onNavigateBack: () -> Unit) {
    LaunchedEffect(Unit) { onNavigateBack() }
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
    ) -> String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val artKey = remember(editingAccount?.id) {
        editingAccount?.id ?: "draft_${UUID.randomUUID().toString().take(8)}"
    }
    val isCreatingNew = editingAccount == null

    var nickname by remember { mutableStateOf(editingAccount?.name ?: "") }
    var selectedType by remember { mutableStateOf(editingAccount?.type ?: AccountType.DEBIT_CARD) }
    var institution by remember { mutableStateOf(editingAccount?.institutionName ?: "") }
    var cardNetwork by remember { mutableStateOf(editingAccount?.cardNetwork ?: "Visa") }
    var lastFour by remember { mutableStateOf(editingAccount?.lastFour ?: "") }
    var currencyCode by remember { mutableStateOf(editingAccount?.currencyCode ?: initialCurrency) }
    var openingBalanceText by remember {
        val initialCode = editingAccount?.currencyCode ?: initialCurrency
        val initialSpec = CurrencyCatalog.findByCode(initialCode)
        mutableStateOf(
            if (editingAccount != null && editingAccount.openingBalanceMinor != 0L) {
                val div = 10.0.pow(initialSpec.minorDecimals.toDouble())
                String.format(Locale.US, "%.${initialSpec.minorDecimals}f", editingAccount.openingBalanceMinor / div)
            } else if (initialSpec.minorDecimals == 0) {
                "0"
            } else {
                "0.00"
            }
        )
    }
    var creditLimitText by remember {
        mutableStateOf(
            editingAccount?.creditLimitMinor?.let {
                val spec = CurrencyCatalog.findByCode(editingAccount.currencyCode)
                val div = 10.0.pow(spec.minorDecimals.toDouble())
                String.format(Locale.US, "%.${spec.minorDecimals}f", it / div)
            } ?: ""
        )
    }
    var description by remember { mutableStateOf(editingAccount?.description ?: "") }

    val artVersion by CardArtStore.version.collectAsStateWithLifecycle()
    val artPrefs = remember(artKey, artVersion) {
        CardArtStore.getCardArt(context, artKey)
    }

    val cardPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                CardArtStore.saveCardPhotoFromUri(context, artKey, uri)
            }
        }
    }

    val cardLogoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                CardArtStore.saveCardLogoFromUri(context, artKey, uri)
            }
        }
    }

    var typeDropdownOpen by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var lastFourError by remember { mutableStateOf<String?>(null) }
    var balanceError by remember { mutableStateOf<String?>(null) }
    var creditLimitError by remember { mutableStateOf<String?>(null) }

    val previewAccount = remember(
        artKey, nickname, selectedType, institution, cardNetwork, lastFour, currencyCode, creditLimitText
    ) {
        AccountRecord(
            id = artKey,
            name = nickname.ifBlank { "My Card" },
            type = selectedType,
            institutionName = institution.ifBlank { "Bank / Provider" },
            cardNetwork = if (selectedType.supportsCardDetails) cardNetwork else "",
            lastFour = if (selectedType.supportsCardDetails) lastFour else "",
            currencyCode = currencyCode,
            openingBalanceMinor = MoneyFormatter.parseMajorToMinor(openingBalanceText, currencyCode) ?: 0L,
            creditLimitMinor = MoneyFormatter.parseMajorToMinor(creditLimitText, currencyCode),
        )
    }

    val dismissWithCleanup = {
        if (isCreatingNew) {
            CardArtStore.clearAllArt(context, artKey)
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = dismissWithCleanup,
        sheetState = sheetState,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (editingAccount != null) "Edit Card & Account" else "Add Card or Account",
                    style = MaterialTheme.typography.headlineSmall,
                )
                IconButton(
                    onClick = dismissWithCleanup,
                    modifier = Modifier.pressBounce(),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Live Google Wallet 3D Card Preview
            Interactive3DWalletCard(
                account = previewAccount,
                balanceMinor = previewAccount.openingBalanceMinor,
                hideBalances = false,
                artPrefs = artPrefs,
                enableDragTilt = true,
                secondaryHint = "Live preview • Tap flip",
                testTag = "account_form_live_card_preview",
            )

            // Zero-permission Photo Picker buttons for real Card Photo & Bank Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        cardPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).pressBounce(pressScale = 0.95f).testTag("pick_card_photo_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.padding(3.dp))
                    Text(if (artPrefs.photoPath != null) "Change Photo" else "Card Photo")
                }
                OutlinedButton(
                    onClick = {
                        cardLogoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f).pressBounce(pressScale = 0.95f).testTag("pick_card_logo_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.padding(3.dp))
                    Text(if (artPrefs.logoPath != null) "Change Logo" else "Bank Logo")
                }
            }

            if (artPrefs.photoPath != null || artPrefs.logoPath != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (artPrefs.photoPath != null) {
                        FilterChip(
                            selected = true,
                            onClick = { CardArtStore.clearCardPhoto(context, artKey) },
                            label = { Text("Remove Photo") },
                            trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.pressBounce(pressScale = 0.94f),
                        )
                    }
                    if (artPrefs.logoPath != null) {
                        FilterChip(
                            selected = true,
                            onClick = { CardArtStore.clearCardLogo(context, artKey) },
                            label = { Text("Remove Logo") },
                            trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.pressBounce(pressScale = 0.94f),
                        )
                    }
                }
            }

            // Card Material Finish Swatches
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Card material & color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isAuto = artPrefs.finishId == null
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .pressBounce(pressScale = 0.88f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = if (isAuto) 2.5.dp else 1.dp,
                                color = if (isAuto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable { CardArtStore.setFinishId(context, artKey, null) }
                            .testTag("card_finish_auto"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Auto finish",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    CardFinishPreset.entries.forEach { preset ->
                        val selected = artPrefs.finishId == preset.id
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .pressBounce(pressScale = 0.88f)
                                .clip(CircleShape)
                                .background(preset.swatchColor)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.16f),
                                    shape = CircleShape,
                                )
                                .clickable { CardArtStore.setFinishId(context, artKey, preset.id) }
                                .testTag("card_finish_${preset.id.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = preset.label,
                                    tint = if (preset.isLightSurface) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
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
                        text = "Manual record only. Never enter full card numbers, CVV codes, PINs, or banking passwords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    if (it.length <= 80) {
                        nickname = it
                        nameError = null
                    }
                },
                label = { Text("Card or account nickname *") },
                placeholder = { Text("e.g., Chase Sapphire, Everyday Debit") },
                modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                isError = nameError != null,
                supportingText = nameError?.let { err -> { Text(err) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            ExposedDropdownMenuBox(
                expanded = typeDropdownOpen,
                onExpandedChange = { typeDropdownOpen = !typeDropdownOpen },
            ) {
                OutlinedTextField(
                    value = selectedType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("account_type_dropdown"),
                    shape = RoundedCornerShape(12.dp),
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
                onValueChange = { if (it.length <= 80) institution = it },
                label = { Text("Bank or card issuer (optional)") },
                placeholder = { Text("e.g., Chase, Citi, Monzo, Wise, Amex") },
                modifier = Modifier.fillMaxWidth().testTag("account_institution_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            if (selectedType.supportsCardDetails) {
                // Quick 1-tap Real Card Network Logo Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Visa", "Mastercard", "Amex", "Discover", "RuPay", "UnionPay").forEach { net ->
                        FilterChip(
                            selected = cardNetwork.equals(net, ignoreCase = true),
                            onClick = { cardNetwork = net },
                            label = { Text(net) },
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.pressBounce(pressScale = 0.94f),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = cardNetwork,
                        onValueChange = { cardNetwork = it },
                        label = { Text("Network") },
                        placeholder = { Text("Visa / Mastercard") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    OutlinedTextField(
                        value = lastFour,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                lastFour = input
                                lastFourError = null
                            }
                        },
                        label = { Text("Last 4 digits") },
                        placeholder = { Text("4821") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("account_last_four_input"),
                        isError = lastFourError != null,
                        supportingText = lastFourError?.let { err -> { Text(err) } },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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
                        .pressBounce(pressScale = 0.95f)
                        .testTag("account_currency_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(currencyCode)
                }
                OutlinedTextField(
                    value = openingBalanceText,
                    onValueChange = {
                        openingBalanceText = it
                        balanceError = null
                    },
                    label = { Text(if (selectedType.isLiability) "Opening due" else "Opening balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.58f).testTag("account_opening_balance_input"),
                    isError = balanceError != null,
                    supportingText = balanceError?.let { err -> { Text(err) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            if (selectedType == AccountType.CREDIT_CARD) {
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = {
                        creditLimitText = it
                        creditLimitError = null
                    },
                    label = { Text("Credit limit (optional)") },
                    placeholder = { Text("e.g., 5000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("account_limit_input"),
                    isError = creditLimitError != null,
                    supportingText = creditLimitError?.let { err -> { Text(err) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 240) description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth().testTag("account_description_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            Button(
                onClick = {
                    val trimmedName = nickname.trim()
                    nameError = when {
                        trimmedName.isBlank() -> "Enter a card or account nickname"
                        trimmedName.length > 80 -> "Nickname must be 80 characters or fewer"
                        else -> null
                    }
                    lastFourError = if (selectedType.supportsCardDetails && lastFour.isNotEmpty() && lastFour.length != 4) {
                        "Enter 4 digits or leave blank"
                    } else null
                    val openingMinor = if (openingBalanceText.isBlank()) 0L
                    else MoneyFormatter.parseMajorToMinor(openingBalanceText, currencyCode)
                    balanceError = if (openingMinor == null) "Invalid opening amount" else null

                    val limitMinor = if (selectedType == AccountType.CREDIT_CARD && creditLimitText.isNotBlank()) {
                        MoneyFormatter.parseMajorToMinor(creditLimitText, currencyCode)
                    } else null
                    creditLimitError = if (selectedType == AccountType.CREDIT_CARD && creditLimitText.isNotBlank() && (limitMinor == null || limitMinor <= 0L)) {
                        "Enter a valid credit limit greater than zero"
                    } else null

                    if (nameError == null &&
                        lastFourError == null &&
                        balanceError == null &&
                        creditLimitError == null &&
                        openingMinor != null
                    ) {
                        val savedId = onSave(
                            trimmedName,
                            selectedType,
                            institution.trim().take(80),
                            cardNetwork.trim().take(40),
                            lastFour,
                            currencyCode,
                            openingMinor,
                            limitMinor,
                            description.trim().take(240),
                        )
                        if (isCreatingNew) {
                            CardArtStore.copyDraftArtToAccount(context, artKey, savedId)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .pressBounce(pressScale = 0.96f)
                    .testTag("save_account_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (editingAccount != null) "Save Card Changes" else "Save Card & Account")
            }
        }
    }

    if (showCurrencyPicker) {
        SearchableCurrencySheet(
            currentCurrencyCode = currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onConfirmCurrency = { spec ->
                currencyCode = spec.code
                if (spec.minorDecimals == 0 && openingBalanceText == "0.00") {
                    openingBalanceText = "0"
                } else if (spec.minorDecimals == 2 && openingBalanceText == "0") {
                    openingBalanceText = "0.00"
                }
                balanceError = null
                creditLimitError = null
                showCurrencyPicker = false
            },
        )
    }
}
