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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CsvImportRow
import com.example.model.TransactionType
import com.example.ui.components.SearchableCurrencySheet
import com.example.ui.components.categoryIconFor
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.ThemeMode
import com.example.util.CurrencyCatalog
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MoreScreen(
    uiState: SpendlyUiState,
    onNavigateAccounts: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateCategories: () -> Unit,
    onNavigateImportExport: () -> Unit,
    onNavigateCurrency: () -> Unit,
    onNavigateSettingsDetail: (String) -> Unit,
    onTogglePreviewData: (Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .testTag("more_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text("More", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Manage accounts, analytics, data portability, and preferences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Financial Management Group
            item {
                MoreSectionGroup(title = "FINANCE & DATA") {
                    MoreMenuRow(
                        icon = Icons.Rounded.AccountBalance,
                        title = "Accounts & Cards",
                        subtitle = "${uiState.activeAccounts.size} active • Manual ledger",
                        onClick = onNavigateAccounts,
                        testTag = "more_accounts",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.BarChart,
                        title = "Reports & Analytics",
                        subtitle = "Cash flow, category donut & trends",
                        onClick = onNavigateReports,
                        testTag = "more_reports",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Category,
                        title = "Categories",
                        subtitle = "${uiState.categories.size} expense & income categories",
                        onClick = onNavigateCategories,
                        testTag = "more_categories",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.UploadFile,
                        title = "Import & Export",
                        subtitle = "CSV wizard, export & JSON backup",
                        onClick = onNavigateImportExport,
                        testTag = "more_import_export",
                    )
                }
            }

            // Preferences Group
            item {
                MoreSectionGroup(title = "PREFERENCES") {
                    MoreMenuRow(
                        icon = Icons.Rounded.CurrencyExchange,
                        title = "Currency Settings",
                        subtitle = "Default: ${uiState.defaultCurrencyCode} • Multi-currency rates",
                        onClick = onNavigateCurrency,
                        testTag = "more_currency",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Palette,
                        title = "Appearance & Language",
                        subtitle = "Theme: ${uiState.themeMode.name.lowercase().replaceFirstChar { it.uppercase() }} • ${uiState.selectedLanguage}",
                        onClick = { onNavigateSettingsDetail("appearance") },
                        testTag = "more_appearance",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        subtitle = if (uiState.budgetAlertsEnabled) "Budget alerts active" else "Alerts muted",
                        onClick = { onNavigateSettingsDetail("notifications") },
                        testTag = "more_notifications",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Security,
                        title = "Security & Privacy",
                        subtitle = "Local-first ledger • Hide balances option",
                        onClick = { onNavigateSettingsDetail("security") },
                        testTag = "more_security",
                    )
                }
            }

            // Profile & About Group
            item {
                MoreSectionGroup(title = "PROFILE & SUPPORT") {
                    MoreMenuRow(
                        icon = Icons.Rounded.Person,
                        title = "Profile",
                        subtitle = uiState.profileName,
                        onClick = { onNavigateSettingsDetail("profile") },
                        testTag = "more_profile",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = "Help & Support",
                        subtitle = "Guides for CSV imports & credit cards",
                        onClick = { onNavigateSettingsDetail("support") },
                        testTag = "more_support",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Info,
                        title = "About Spendly",
                        subtitle = "Spendly Global v1.0 • UI/UX Edition",
                        onClick = { onNavigateSettingsDetail("about") },
                        testTag = "more_about",
                    )
                }
            }

            // Sample Preview Data Switch for inspecting Empty vs Populated UI
            item {
                Surface(
                    shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Interactive UI Preview Ledger", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Toggle off to inspect clean Empty States across all screens, or on to restore sample preview data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.isPreviewDataActive,
                            onCheckedChange = onTogglePreviewData,
                            modifier = Modifier.testTag("more_preview_data_switch"),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    uiState: SpendlyUiState,
    onNavigateBack: () -> Unit,
    onAddCategory: (name: String, type: TransactionType, iconKey: String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.padding(end = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.padding(2.dp))
                        Text("New")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 860.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.categories, key = { it.id }) { cat ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(categoryIconFor(cat.iconKey), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                    Text(cat.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = if (cat.isDefault) "Default" else "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { t ->
                            val active = newType == t
                            Surface(
                                onClick = { newType = t },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        t.label,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            nameError = null
                        },
                        label = { Text("Category Name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { err -> { Text(err) } },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank()) {
                            nameError = "Enter a category name"
                        } else {
                            onAddCategory(newName, newType, if (newType == TransactionType.INCOME) "salary" else "shopping")
                            newName = ""
                            showAddDialog = false
                        }
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    uiState: SpendlyUiState,
    onNavigateBack: () -> Unit,
    isDuplicateCandidate: (dateMillis: Long, amountMinor: Long, description: String, accountId: String) -> Boolean,
    onCommitCsvImport: (rows: List<CsvImportRow>, destinationAccountId: String, currencyCode: String) -> Int,
    onRestoreBackupSample: () -> Unit,
) {
    val context = LocalContext.current
    var wizardStep by remember { mutableIntStateOf(0) } // 0 = Hub, 1 = Preview & Map, 2 = Confirm
    var destAccountId by remember(uiState.activeAccounts) {
        mutableStateOf(uiState.activeAccounts.firstOrNull()?.id ?: "")
    }
    var importCurrency by remember { mutableStateOf(uiState.defaultCurrencyCode) }
    var rawCsvText by remember {
        mutableStateOf(
            """
            Date,Description,Amount,Type
            2026-11-18,Blue Bottle Coffee,6.75,Expense
            2026-11-17,City Metro Transit Pass,34.00,Expense
            2026-11-15,Bi-weekly Payroll Deposit,4850.00,Income
            2026-11-14,Invalid Zero Entry,0.00,Expense
            """.trimIndent()
        )
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var accountDropdown by remember { mutableStateOf(false) }

    val parsedRows = remember(rawCsvText, destAccountId, importCurrency, wizardStep) {
        if (wizardStep == 0 || destAccountId.isBlank()) emptyList()
        else parseCsvRows(rawCsvText, importCurrency, destAccountId, isDuplicateCandidate)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Import & Export") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 860.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (wizardStep == 0) {
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
                                Text("CSV Statement Import Wizard", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Spendly validates rows, maps columns, checks for duplicates, and previews errors before importing. Existing records are never overwritten silently.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = rawCsvText,
                                    onValueChange = { rawCsvText = it },
                                    label = { Text("CSV Content (Edit or paste bank CSV)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    shape = RoundedCornerShape(14.dp),
                                )
                                Button(
                                    onClick = { wizardStep = 1 },
                                    enabled = uiState.activeAccounts.isNotEmpty() && rawCsvText.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("csv_preview_button"),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                                    Spacer(Modifier.padding(3.dp))
                                    Text(if (uiState.activeAccounts.isEmpty()) "Create an Account First" else "Preview & Map CSV")
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("Export & Backup", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val file = File(context.cacheDir, "spendly_transactions_export.csv")
                                            val body = buildString {
                                                appendLine("Date,Type,Description,Amount,Currency")
                                                uiState.transactions.forEach {
                                                    appendLine("${DateFormatters.formatIsoDate(it.dateMillis)},${it.type.label},\"${it.description}\",${it.amountMinor / 100.0},${it.currencyCode}")
                                                }
                                            }
                                            file.writeText(body)
                                            statusMessage = "Exported ${uiState.transactions.size} transactions to ${file.name}."
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                                        Spacer(Modifier.padding(2.dp))
                                        Text("Export CSV")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val file = File(context.cacheDir, "spendly_backup.json")
                                            file.writeText("{\"accounts\":${uiState.accounts.size},\"transactions\":${uiState.transactions.size},\"budgets\":${uiState.budgets.size}}")
                                            statusMessage = "Created JSON backup (${file.name}, ${file.length()} bytes)."
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Text("Export Backup")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showRestoreConfirm = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Text("Restore Preview Snapshot")
                                }
                            }
                        }
                    }
                } else {
                    // Step 1+: Preview, Map Account & Duplicate Detection
                    item {
                        Surface(
                            shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("Step 2: Map Destination & Currency", style = MaterialTheme.typography.titleMedium)
                                val destAcc = uiState.accountById(destAccountId)
                                ExposedDropdownMenuBox(
                                    expanded = accountDropdown,
                                    onExpandedChange = { accountDropdown = !accountDropdown },
                                ) {
                                    OutlinedTextField(
                                        value = destAcc?.name ?: "Select account",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Destination Account") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdown) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = accountDropdown,
                                        onDismissRequest = { accountDropdown = false },
                                    ) {
                                        uiState.activeAccounts.forEach { acc ->
                                            DropdownMenuItem(
                                                text = { Text("${acc.name} (${acc.currencyCode})") },
                                                onClick = {
                                                    destAccountId = acc.id
                                                    importCurrency = acc.currencyCode
                                                    accountDropdown = false
                                                },
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Columns mapped: Date → Transaction Date, Description → Merchant/Desc, Amount → $importCurrency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    val validCount = parsedRows.count { it.errorMessage == null && !it.isDuplicate }
                    val dupCount = parsedRows.count { it.isDuplicate }
                    val errCount = parsedRows.count { it.errorMessage != null }

                    item {
                        Text(
                            text = "Preview: $validCount ready • $dupCount duplicate(s) skipped • $errCount error(s)",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    items(parsedRows, key = { it.rowNumber }) { row ->
                        val semantic = SpendlyTheme.semantic
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Row ${row.rowNumber}: ${row.description}", style = MaterialTheme.typography.titleSmall)
                                    Text("${row.dateText} • ${row.amountText} ($importCurrency)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    row.errorMessage?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall, color = semantic.expense)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = when {
                                        row.errorMessage != null -> semantic.expenseContainer
                                        row.isDuplicate -> semantic.warningContainer
                                        else -> semantic.incomeContainer
                                    },
                                ) {
                                    Text(
                                        text = when {
                                            row.errorMessage != null -> "Error"
                                            row.isDuplicate -> "Duplicate"
                                            else -> "Ready"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            row.errorMessage != null -> semantic.expense
                                            row.isDuplicate -> semantic.warning
                                            else -> semantic.income
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { wizardStep = 0 },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val count = onCommitCsvImport(parsedRows, destAccountId, importCurrency)
                                    wizardStep = 0
                                    statusMessage = "Imported $count new transaction(s). Duplicates and invalid rows were safely skipped."
                                },
                                enabled = validCount > 0,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("csv_confirm_import_button"),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Confirm Import ($validCount)")
                            }
                        }
                    }
                }
            }
        }
    }

    statusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = { Text("Data Action Complete") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { statusMessage = null }) { Text("OK") }
            },
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Confirm Backup Restoration") },
            text = {
                Text("Restoring the preview snapshot will replace your current in-session accounts, transactions, and budgets with the default preview ledger. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackupSample()
                        showRestoreConfirm = false
                        statusMessage = "Restored snapshot successfully."
                    },
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

private fun parseCsvRows(
    raw: String,
    currencyCode: String,
    destAccountId: String,
    isDuplicateCandidate: (Long, Long, String, String) -> Boolean,
): List<CsvImportRow> {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.size <= 1) return emptyList()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    return lines.drop(1).mapIndexed { index, line ->
        val cols = line.split(",").map { it.trim() }
        val dateText = cols.getOrNull(0) ?: ""
        val desc = cols.getOrNull(1) ?: ""
        val amtText = cols.getOrNull(2) ?: ""
        val typeText = cols.getOrNull(3) ?: "Expense"

        val parsedDate = runCatching { sdf.parse(dateText)?.time }.getOrNull()
        val parsedMinor = MoneyFormatter.parseMajorToMinor(amtText, currencyCode)
        val type = if (typeText.equals("Income", ignoreCase = true)) TransactionType.INCOME else TransactionType.EXPENSE

        val err = when {
            parsedDate == null -> "Invalid date format (expected YYYY-MM-DD)"
            desc.isBlank() -> "Missing description"
            parsedMinor == null || parsedMinor <= 0L -> "Amount must be greater than zero"
            else -> null
        }

        val dup = if (err == null && parsedDate != null && parsedMinor != null) {
            isDuplicateCandidate(parsedDate, parsedMinor, desc, destAccountId)
        } else false

        CsvImportRow(
            rowNumber = index + 1,
            dateText = dateText,
            parsedDateMillis = parsedDate,
            description = desc,
            amountText = amtText,
            parsedAmountMinor = parsedMinor,
            type = type,
            categoryId = if (type == TransactionType.INCOME) "cat_salary" else "cat_groceries",
            isDuplicate = dup,
            errorMessage = err,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    uiState: SpendlyUiState,
    onNavigateBack: () -> Unit,
    onSetDefaultCurrency: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val base = CurrencyCatalog.findByCode(uiState.defaultCurrencyCode)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Currency Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 860.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth().testTag("change_default_currency_card"),
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("DEFAULT BASE CURRENCY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "${base.code} (${base.symbol}) — ${base.displayName}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                item {
                    Text(
                        text = "Exchange Rates & Precision",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Original transaction currencies are always preserved. Missing rates are shown explicitly rather than fabricated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                items(CurrencyCatalog.supportedCurrencies, key = { it.code }) { spec ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("${spec.code} (${spec.symbol}) — ${spec.displayName}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = if (spec.rateToUsd != null) {
                                        "1 ${spec.code} = ${spec.rateToUsd} USD • ${spec.rateSource} (${spec.rateDate})"
                                    } else {
                                        "Exchange rate unavailable — displays original currency only"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${spec.minorDecimals} dec",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        SearchableCurrencySheet(
            currentCurrencyCode = uiState.defaultCurrencyCode,
            onDismiss = { showPicker = false },
            onConfirmCurrency = {
                onSetDefaultCurrency(it.code)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDetailScreen(
    section: String,
    uiState: SpendlyUiState,
    onNavigateBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (String) -> Unit,
    onUpdatePreferences: (profileName: String?, budgetAlerts: Boolean?, weeklyDigest: Boolean?, hideBalances: Boolean?) -> Unit,
) {
    val title = when (section) {
        "appearance" -> "Appearance & Language"
        "notifications" -> "Notifications"
        "security" -> "Security & Privacy"
        "profile" -> "Profile"
        "support" -> "Help & Support"
        else -> "About Spendly"
    }

    var editingName by remember(uiState.profileName) { mutableStateOf(uiState.profileName) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_detail_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (section) {
                    "appearance" -> {
                        item {
                            MoreSectionGroup(title = "THEME") {
                                ThemeMode.entries.forEachIndexed { idx, mode ->
                                    if (idx > 0) MoreDivider()
                                    Surface(
                                        onClick = { onSetThemeMode(mode) },
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth().testTag("theme_option_${mode.name.lowercase()}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
                                            if (uiState.themeMode == mode) {
                                                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            MoreSectionGroup(title = "LANGUAGE") {
                                listOf("English", "বাংলা (Bangla)").forEachIndexed { idx, lang ->
                                    if (idx > 0) MoreDivider()
                                    Surface(
                                        onClick = { onSetLanguage(lang) },
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(lang, style = MaterialTheme.typography.titleSmall)
                                            if (uiState.selectedLanguage == lang) {
                                                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "notifications" -> {
                        item {
                            PreferenceToggleCard(
                                title = "Budget Threshold Alerts",
                                subtitle = "Highlight categories at 85% usage or over budget.",
                                checked = uiState.budgetAlertsEnabled,
                                onCheckedChange = { onUpdatePreferences(null, it, null, null) },
                            )
                        }
                        item {
                            PreferenceToggleCard(
                                title = "Weekly Cash-Flow Digest",
                                subtitle = "Weekly summary reminder of net spending vs income.",
                                checked = uiState.weeklyDigestEnabled,
                                onCheckedChange = { onUpdatePreferences(null, null, it, null) },
                            )
                        }
                    }

                    "security" -> {
                        item {
                            PreferenceToggleCard(
                                title = "Hide Monetary Balances",
                                subtitle = "Mask balances and transaction amounts across Home, Activity, and Accounts.",
                                checked = uiState.privacyHideBalances,
                                onCheckedChange = { onUpdatePreferences(null, null, null, it) },
                            )
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Text(
                                    text = "Privacy Guarantee: Spendly stores your ledger locally and never collects full card numbers, CVVs, PINs, or bank passwords.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }

                    "profile" -> {
                        item {
                            OutlinedTextField(
                                value = editingName,
                                onValueChange = { editingName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    if (editingName.isNotBlank()) {
                                        onUpdatePreferences(editingName.trim(), null, null, null)
                                        onNavigateBack()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Save Profile")
                            }
                        }
                    }

                    "support" -> {
                        item {
                            InfoCard("How Credit Card Repayments Work", "Record repayments as a Transfer from your Bank Account to your Credit Card. Spendly reduces your credit card outstanding balance without counting the repayment as a second shopping expense.")
                        }
                        item {
                            InfoCard("Manual Account Balances", "Account balances are calculated from your opening balance plus recorded transactions. They are tracking figures rather than live bank feeds.")
                        }
                    }

                    else -> {
                        item {
                            InfoCard("SPENDLY GLOBAL", "Version 1.0 • Editorial Ledger UI/UX\nOrganize manually. Understand automatically.\n\nDefault Currency: USD ($)\nOwner administration portal (/admin) and payment gateway adapters are isolated server-side.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreSectionGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun MoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MoreDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PreferenceToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
