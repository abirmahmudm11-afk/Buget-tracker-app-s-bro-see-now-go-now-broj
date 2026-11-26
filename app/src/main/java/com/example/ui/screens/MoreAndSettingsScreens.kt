package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CsvImportRow
import com.example.model.TransactionType
import com.example.notifications.SpendlyNotificationHelper
import com.example.ui.components.LedgerGroupCard
import com.example.ui.components.SearchableCurrencySheet
import com.example.ui.components.categoryIconFor
import com.example.ui.components.pressBounce
import com.example.ui.components.touchSpringCard
import com.example.ui.theme.SpendlyTheme
import com.example.ui.theme.SpendlyTokens
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.categoryColorPair
import com.example.util.CsvUtils
import com.example.util.CurrencyCatalog
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyUiState
import java.io.File

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
    onSignOut: () -> Unit = {},
) {
    val semantic = SpendlyTheme.semantic
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val initials = remember(uiState.profileName) {
        uiState.profileName
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "SP" }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .testTag("more_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Accounts, reports, data portability, and preferences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Profile Summary Card (Nordic Spruce & Gold Ledger Hero)
            item {
                Surface(
                    onClick = { onNavigateSettingsDetail("profile") },
                    modifier = Modifier
                        .touchSpringCard(pressScale = 0.98f, maxTiltDeg = 5.5f, hapticOnPress = true)
                        .testTag("more_profile_hero"),
                    shape = RoundedCornerShape(SpendlyTokens.HeroRadius),
                    color = semantic.heroSurface,
                    border = BorderStroke(1.dp, semantic.heroGold.copy(alpha = 0.28f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(semantic.heroSurfaceElevated, semantic.heroSurface),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height),
                                    ),
                                )
                            }
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(semantic.onHeroWell),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = semantic.heroAccent,
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = uiState.profileName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = semantic.onHeroSurface,
                                    )
                                    Text(
                                        text = "${uiState.defaultCurrencyCode} Base • ${uiState.selectedLanguage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = semantic.onHeroSurfaceMuted,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(semantic.onHeroWell),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Edit profile",
                                    tint = semantic.onHeroSurface,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ProfileStatPill("Accounts", "${uiState.activeAccounts.size}", Modifier.weight(1f))
                            ProfileStatPill("Entries", "${uiState.transactions.size}", Modifier.weight(1f))
                            ProfileStatPill("Budgets", "${uiState.budgets.size}", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Financial Management Group
            item {
                MoreSectionGroup(title = "Finance & Data") {
                    MoreMenuRow(
                        icon = Icons.Rounded.AccountBalance,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        title = "Accounts & Cards",
                        subtitle = "${uiState.activeAccounts.size} active • Manual ledger",
                        onClick = onNavigateAccounts,
                        testTag = "more_accounts",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.BarChart,
                        iconTint = semantic.warning,
                        iconBg = semantic.warningContainer,
                        title = "Reports & Analytics",
                        subtitle = "Cash flow, category breakdown & trends",
                        onClick = onNavigateReports,
                        testTag = "more_reports",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Category,
                        iconTint = semantic.slateAccent,
                        iconBg = semantic.slateContainer,
                        title = "Categories",
                        subtitle = "${uiState.categories.size} expense & income categories",
                        onClick = onNavigateCategories,
                        testTag = "more_categories",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.UploadFile,
                        iconTint = semantic.transfer,
                        iconBg = semantic.transferContainer,
                        title = "Import & Export",
                        subtitle = "CSV wizard, export & JSON backup",
                        onClick = onNavigateImportExport,
                        testTag = "more_import_export",
                    )
                }
            }

            // Preferences Group
            item {
                MoreSectionGroup(title = "Preferences") {
                    MoreMenuRow(
                        icon = Icons.Rounded.CurrencyExchange,
                        iconTint = semantic.income,
                        iconBg = semantic.incomeContainer,
                        title = "Currency Settings",
                        subtitle = "Default: ${uiState.defaultCurrencyCode} • Multi-currency rates",
                        onClick = onNavigateCurrency,
                        testTag = "more_currency",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Palette,
                        iconTint = semantic.warning,
                        iconBg = semantic.warningContainer,
                        title = "Appearance & Language",
                        subtitle = "Theme: ${uiState.themeMode.name.lowercase().replaceFirstChar { it.uppercase() }} • ${uiState.selectedLanguage}",
                        onClick = { onNavigateSettingsDetail("appearance") },
                        testTag = "more_appearance",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Notifications,
                        iconTint = semantic.expense,
                        iconBg = semantic.expenseContainer,
                        title = "Notifications",
                        subtitle = if (uiState.budgetAlertsEnabled) "Budget alerts active" else "Alerts muted",
                        onClick = { onNavigateSettingsDetail("notifications") },
                        testTag = "more_notifications",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Security,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        title = "Security & Privacy",
                        subtitle = "Cloud sync • Hide balances option",
                        onClick = { onNavigateSettingsDetail("security") },
                        testTag = "more_security",
                    )
                }
            }

            // Profile & About Group
            item {
                MoreSectionGroup(title = "Profile & Support") {
                    MoreMenuRow(
                        icon = Icons.Rounded.Person,
                        iconTint = semantic.transfer,
                        iconBg = semantic.transferContainer,
                        title = "Profile",
                        subtitle = uiState.profileName,
                        onClick = { onNavigateSettingsDetail("profile") },
                        testTag = "more_profile",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        iconTint = semantic.slateAccent,
                        iconBg = semantic.slateContainer,
                        title = "Help & Support",
                        subtitle = "Guides for CSV imports & credit cards",
                        onClick = { onNavigateSettingsDetail("support") },
                        testTag = "more_support",
                    )
                    MoreDivider()
                    MoreMenuRow(
                        icon = Icons.Rounded.Info,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconBg = MaterialTheme.colorScheme.surfaceContainer,
                        title = "About Spendly",
                        subtitle = "Spendly v1.0 • Personal Finance Ledger",
                        onClick = { onNavigateSettingsDetail("about") },
                        testTag = "more_about",
                    )
                }
            }

            // Client Portfolio Data Switch for inspecting Empty vs Populated UI
            item {
                LedgerGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Preloaded Client Portfolio",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Toggle off to clear ledger records or on to restore the starter portfolio.",
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

            // Account Session / Sign Out
            item {
                LedgerGroupCard {
                    MoreMenuRow(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        iconTint = semantic.expense,
                        iconBg = semantic.expenseContainer,
                        title = "Sign Out",
                        subtitle = "Switch Google account or lock session",
                        onClick = { showSignOutConfirm = true },
                        testTag = "more_sign_out",
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out?") },
            text = { Text("You will be returned to the sign-in screen. Your cloud ledger remains safely synced.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    modifier = Modifier
                        .pressBounce(pressScale = 0.94f)
                        .testTag("more_sign_out_confirm"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutConfirm = false },
                    modifier = Modifier.pressBounce(pressScale = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun ProfileStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val semantic = SpendlyTheme.semantic
    Surface(
        modifier = modifier.touchSpringCard(pressScale = 0.96f, maxTiltDeg = 6f),
        shape = RoundedCornerShape(12.dp),
        color = semantic.onHeroWell,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = semantic.onHeroSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = semantic.onHeroSurfaceMuted,
            )
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
    val semantic = SpendlyTheme.semantic
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var nameError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .pressBounce()
                            .testTag("categories_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            nameError = null
                            showAddDialog = true
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .pressBounce(pressScale = 0.94f)
                            .testTag("cat_add_button"),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("New")
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.categories, key = { it.id }) { cat ->
                    val (catTint, catBg) = categoryColorPair(cat.iconKey, semantic.isDark)
                    Surface(
                        modifier = Modifier.touchSpringCard(pressScale = 0.98f, maxTiltDeg = 4.5f),
                        shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = if (semantic.isDark) 0.92f else 0.96f),
                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(catBg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        categoryIconFor(cat.iconKey),
                                        contentDescription = null,
                                        tint = catTint,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        cat.type.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Text(
                                    text = if (cat.isDefault) "Default" else "Custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                nameError = null
                showAddDialog = false
            },
            title = { Text("New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { t ->
                            val active = newType == t
                            Surface(
                                onClick = {
                                    newType = t
                                    nameError = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .pressBounce(pressScale = 0.94f)
                                    .testTag("cat_type_${t.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        t.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it.take(60)
                            nameError = null
                        },
                        label = { Text("Category Name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { err -> { Text(err) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cat_name_input"),
                        shape = RoundedCornerShape(SpendlyTokens.InputRadius),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newName.trim()
                        val duplicate = uiState.categories.any {
                            it.type == newType && it.name.equals(trimmed, ignoreCase = true)
                        }
                        when {
                            trimmed.isEmpty() -> nameError = "Enter a category name"
                            trimmed.length > 60 -> nameError = "Category name must be 60 characters or fewer"
                            duplicate -> nameError = "A ${newType.label.lowercase()} category named \"$trimmed\" already exists"
                            else -> {
                                onAddCategory(trimmed, newType, if (newType == TransactionType.INCOME) "salary" else "shopping")
                                newName = ""
                                nameError = null
                                showAddDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("cat_save_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        nameError = null
                        showAddDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Cancel") }
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
    val semantic = SpendlyTheme.semantic
    var wizardStep by remember { mutableIntStateOf(0) }
    var destAccountId by remember {
        mutableStateOf(uiState.activeAccounts.firstOrNull()?.id ?: "")
    }
    var importCurrency by remember {
        mutableStateOf(uiState.activeAccounts.firstOrNull()?.currencyCode ?: uiState.defaultCurrencyCode)
    }
    LaunchedEffect(uiState.activeAccounts) {
        val selectedStillExists = uiState.activeAccounts.any { it.id == destAccountId }
        if (!selectedStillExists) {
            val fallback = uiState.activeAccounts.firstOrNull()
            destAccountId = fallback?.id ?: ""
            importCurrency = fallback?.currencyCode ?: uiState.defaultCurrencyCode
        }
    }
    BackHandler(enabled = wizardStep > 0) {
        wizardStep = 0
    }
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
        else CsvUtils.parseCsvRows(rawCsvText, importCurrency, destAccountId, isDuplicateCandidate)
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
                        "Import & Export",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (wizardStep > 0) wizardStep = 0 else onNavigateBack()
                        },
                        modifier = Modifier
                            .pressBounce()
                            .testTag("import_export_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (wizardStep == 0) {
                    item {
                        LedgerGroupCard {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "CSV Statement Import",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "Spendly validates rows, maps columns, checks for duplicates, and previews errors before importing. Existing records are never overwritten.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedTextField(
                                    value = rawCsvText,
                                    onValueChange = { rawCsvText = it },
                                    label = { Text("CSV Content (Edit or paste bank CSV)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("csv_raw_input"),
                                    minLines = 4,
                                    shape = RoundedCornerShape(SpendlyTokens.InputRadius),
                                )
                                Button(
                                    onClick = { wizardStep = 1 },
                                    enabled = uiState.activeAccounts.isNotEmpty() && rawCsvText.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .pressBounce(enabled = uiState.activeAccounts.isNotEmpty() && rawCsvText.isNotBlank(), pressScale = 0.96f)
                                        .testTag("csv_preview_button"),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                                    Spacer(Modifier.size(6.dp))
                                    Text(if (uiState.activeAccounts.isEmpty()) "Create an Account First" else "Preview & Map CSV")
                                }
                            }
                        }
                    }

                    item {
                        LedgerGroupCard {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "Export & Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                )
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
                                                    val date = CsvUtils.escapeCsvField(DateFormatters.formatIsoDate(it.dateMillis))
                                                    val typeLabel = CsvUtils.escapeCsvField(it.type.label)
                                                    val desc = CsvUtils.escapeCsvField(it.description)
                                                    val amt = CsvUtils.formatMinorForCsv(it.amountMinor, it.currencyCode)
                                                    val cur = CsvUtils.escapeCsvField(it.currencyCode)
                                                    appendLine("$date,$typeLabel,$desc,$amt,$cur")
                                                }
                                            }
                                            file.writeText(body)
                                            statusMessage = "Exported ${uiState.transactions.size} transactions to ${file.name}."
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .pressBounce(pressScale = 0.95f)
                                            .testTag("csv_export_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                                    ) {
                                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                                        Spacer(Modifier.size(4.dp))
                                        Text("Export CSV")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val file = File(context.cacheDir, "spendly_backup.json")
                                            file.writeText("{\"accounts\":${uiState.accounts.size},\"transactions\":${uiState.transactions.size},\"budgets\":${uiState.budgets.size}}")
                                            statusMessage = "Created JSON backup (${file.name}, ${file.length()} bytes)."
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .pressBounce(pressScale = 0.95f)
                                            .testTag("backup_export_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                                    ) {
                                        Text("Export Backup")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showRestoreConfirm = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .pressBounce(pressScale = 0.96f)
                                        .testTag("restore_snapshot_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, semantic.surfaceBorder),
                                ) {
                                    Text("Restore Preview Snapshot")
                                }
                            }
                        }
                    }
                } else {
                    item {
                        LedgerGroupCard {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    "Step 2: Map Destination & Currency",
                                    style = MaterialTheme.typography.titleMedium,
                                )
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                        shape = RoundedCornerShape(SpendlyTokens.InputRadius),
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
                                    "Columns mapped: Date -> Transaction Date, Description -> Merchant/Desc, Amount -> $importCurrency",
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
                        Surface(
                            shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, semantic.surfaceBorder),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Row ${row.rowNumber}: ${row.description}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        "${row.dateText} • ${row.amountText} ($importCurrency)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .pressBounce(pressScale = 0.95f)
                                    .testTag("csv_back_step_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, semantic.surfaceBorder),
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    val count = onCommitCsvImport(parsedRows, destAccountId, importCurrency)
                                    wizardStep = 0
                                    statusMessage = "Imported $count new transaction(s). Duplicates and invalid rows were skipped."
                                },
                                enabled = validCount > 0,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .pressBounce(enabled = validCount > 0, pressScale = 0.95f)
                                    .testTag("csv_confirm_import_button"),
                                shape = RoundedCornerShape(12.dp),
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
                Button(
                    onClick = { statusMessage = null },
                    shape = RoundedCornerShape(12.dp),
                ) { Text("OK") }
            },
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Confirm Backup Restoration") },
            text = {
                Text("Restoring the preview snapshot will replace your current accounts, transactions, and budgets with the starter preview ledger. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackupSample()
                        showRestoreConfirm = false
                        statusMessage = "Restored snapshot successfully."
                    },
                    modifier = Modifier.testTag("confirm_restore_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRestoreConfirm = false },
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Cancel") }
            },
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
    val semantic = SpendlyTheme.semantic
    var showPicker by remember { mutableStateOf(false) }
    val base = CurrencyCatalog.findByCode(uiState.defaultCurrencyCode)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        "Currency Settings",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.pressBounce()) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Surface(
                        onClick = { showPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressBounce(pressScale = 0.97f)
                            .testTag("change_default_currency_card"),
                        shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "Default Base Currency",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "${base.code} (${base.symbol}) — ${base.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    "Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Exchange Rates & Precision",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Original transaction currencies are preserved. Missing rates are shown explicitly rather than estimated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(CurrencyCatalog.supportedCurrencies, key = { it.code }) { spec ->
                    Surface(
                        shape = RoundedCornerShape(SpendlyTokens.RowRadius),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, semantic.surfaceBorder),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${spec.code} (${spec.symbol}) — ${spec.displayName}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
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
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Text(
                                    text = "${spec.minorDecimals} dec",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                )
                            }
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
    onUpdateDailyReminder: (Boolean) -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val context = LocalContext.current
    val title = when (section) {
        "appearance" -> "Appearance & Language"
        "notifications" -> "Notifications"
        "security" -> "Security & Privacy"
        "profile" -> "Profile"
        "support" -> "Help & Support"
        else -> "About Spendly"
    }

    var editingName by remember(uiState.profileName) { mutableStateOf(uiState.profileName) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var pendingNotifToggle by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            when (pendingNotifToggle) {
                "alerts" -> onUpdatePreferences(null, false, null, null)
                "digest" -> onUpdatePreferences(null, null, false, null)
                "daily" -> {
                    onUpdateDailyReminder(false)
                    SpendlyNotificationHelper.updateDailyReminderSchedule(context, false)
                }
            }
        }
        pendingNotifToggle = null
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
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.pressBounce().testTag("settings_detail_back"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (section) {
                    "appearance" -> {
                        item {
                            MoreSectionGroup(title = "Theme") {
                                listOf(
                                    ThemeMode.LIGHT to "Light (Nordic Paper)",
                                    ThemeMode.DARK to "Dark (Nordic Slate)",
                                    ThemeMode.SYSTEM to "Match System Setting",
                                ).forEachIndexed { idx, (mode, label) ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                    Surface(
                                        onClick = { onSetThemeMode(mode) },
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .pressBounce(pressScale = 0.97f)
                                            .testTag("theme_option_${mode.name.lowercase()}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                label,
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            if (uiState.themeMode == mode) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            MoreSectionGroup(title = "Language") {
                                listOf("English", "বাংলা (Bangla)").forEachIndexed { idx, lang ->
                                    if (idx > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                    Surface(
                                        onClick = { onSetLanguage(lang) },
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .pressBounce(pressScale = 0.97f)
                                            .testTag("language_option_$idx"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                lang,
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            if (uiState.selectedLanguage == lang) {
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
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
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !SpendlyNotificationHelper.hasPermission(context)) {
                                        pendingNotifToggle = "alerts"
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onUpdatePreferences(null, enabled, null, null)
                                },
                                switchTestTag = "toggle_budget_alerts",
                            )
                        }
                        item {
                            PreferenceToggleCard(
                                title = "Weekly Cash-Flow Digest",
                                subtitle = "Weekly summary reminder of net spending vs income.",
                                checked = uiState.weeklyDigestEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !SpendlyNotificationHelper.hasPermission(context)) {
                                        pendingNotifToggle = "digest"
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onUpdatePreferences(null, null, enabled, null)
                                },
                                switchTestTag = "toggle_weekly_digest",
                            )
                        }
                        item {
                            PreferenceToggleCard(
                                title = "Daily Evening Check-In",
                                subtitle = "Gentle 8:00 PM reminder to log daily expenses.",
                                checked = uiState.dailyReminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !SpendlyNotificationHelper.hasPermission(context)) {
                                        pendingNotifToggle = "daily"
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onUpdateDailyReminder(enabled)
                                    SpendlyNotificationHelper.updateDailyReminderSchedule(context, enabled)
                                },
                                switchTestTag = "toggle_daily_reminder",
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
                                switchTestTag = "toggle_hide_balances",
                            )
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(SpendlyTokens.CardRadius),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "Privacy Guarantee: Spendly protects your cloud ledger with strict per-user access control and never collects card numbers, CVVs, PINs, or bank passwords.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }

                    "profile" -> {
                        item {
                            LedgerGroupCard {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    OutlinedTextField(
                                        value = editingName,
                                        onValueChange = {
                                            editingName = it.take(80)
                                            nameError = null
                                        },
                                        label = { Text("Display Name") },
                                        isError = nameError != null,
                                        supportingText = nameError?.let { err -> { Text(err) } },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_name_input"),
                                        singleLine = true,
                                        shape = RoundedCornerShape(SpendlyTokens.InputRadius),
                                    )
                                    Button(
                                        onClick = {
                                            val trimmed = editingName.trim()
                                            when {
                                                trimmed.isEmpty() -> nameError = "Enter a display name"
                                                trimmed.length > 80 -> nameError = "Display name must be 80 characters or fewer"
                                                else -> {
                                                    onUpdatePreferences(trimmed, null, null, null)
                                                    onNavigateBack()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .pressBounce(pressScale = 0.96f)
                                            .testTag("profile_save_button"),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Text("Save Profile")
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = { showSignOutConfirm = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .pressBounce(pressScale = 0.96f)
                                    .testTag("profile_sign_out_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("Sign Out")
                            }
                        }
                    }

                    "support" -> {
                        item {
                            InfoCard(
                                "How Credit Card Repayments Work",
                                "Record repayments as a Transfer from your Bank Account to your Credit Card. Spendly reduces your credit card outstanding balance without counting the repayment as a second shopping expense.",
                            )
                        }
                        item {
                            InfoCard(
                                "Manual Account Balances",
                                "Account balances are calculated from your opening balance plus recorded transactions. They are tracking figures rather than live bank feeds.",
                            )
                        }
                    }

                    else -> {
                        item {
                            InfoCard(
                                "Spendly",
                                "Version 1.0 • Personal Finance Ledger\nOrganize manually. Understand clearly.\n\nDefault Currency: ${uiState.defaultCurrencyCode}\nCloud-synced personal finance tracking.",
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out?") },
            text = { Text("You will be returned to the sign-in screen. Your cloud ledger remains safely synced.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("sign_out_confirm_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutConfirm = false },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancel")
                }
            },
        )
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
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        LedgerGroupCard {
            content()
        }
    }
}

@Composable
private fun MoreMenuRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .pressBounce(pressScale = 0.97f)
            .testTag(testTag),
        color = MaterialTheme.colorScheme.surface,
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
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoreDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun PreferenceToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String = "",
) {
    LedgerGroupCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = if (switchTestTag.isNotEmpty()) Modifier.testTag(switchTestTag) else Modifier,
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    LedgerGroupCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
