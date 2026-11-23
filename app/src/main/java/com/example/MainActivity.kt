package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.SpendlyBottomNavigationBar
import com.example.ui.components.SpendlySideNavigationRail
import com.example.ui.components.SpendlyTopTab
import com.example.ui.screens.AccountDetailScreen
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CurrencySettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsDetailScreen
import com.example.ui.screens.TransactionDetailSheet
import com.example.ui.theme.SpendlyTheme
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val spendlyViewModel: SpendlyViewModel = viewModel()
            SpendlyApp(viewModel = spendlyViewModel)
        }
    }
}

@Composable
fun SpendlyApp(viewModel: SpendlyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var inspectedTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: SpendlyTopTab.HOME.route

    val topLevelRoutes = remember { SpendlyTopTab.entries.map { it.route }.toSet() }
    val showTopLevelChrome = uiState.onboardingComplete && currentRoute in topLevelRoutes

    fun navigateTopTab(tab: SpendlyTopTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openAddTransaction(
        type: TransactionType = TransactionType.EXPENSE,
        accountId: String? = null,
        txId: String? = null,
    ) {
        val accParam = accountId ?: ""
        val txParam = txId ?: ""
        navController.navigate("add_transaction?type=${type.name}&accountId=$accParam&txId=$txParam")
    }

    fun performDeleteWithUndo(tx: TransactionRecord) {
        viewModel.deleteTransaction(tx.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted \"${tx.description}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteTransaction()
            }
        }
    }

    SpendlyTheme(themeMode = uiState.themeMode) {
        if (!uiState.onboardingComplete) {
            OnboardingScreen(
                initialCurrencyCode = uiState.defaultCurrencyCode,
                initialLanguage = uiState.selectedLanguage,
                onGetStarted = { currencyCode, language, loadSample ->
                    viewModel.completeOnboarding(currencyCode, language, loadSample)
                },
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useSideRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (showTopLevelChrome && !useSideRail) {
                            SpendlyBottomNavigationBar(
                                currentRoute = currentRoute,
                                onSelectTab = ::navigateTopTab,
                                onAddClick = { openAddTransaction(TransactionType.EXPENSE) },
                            )
                        }
                    },
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        if (showTopLevelChrome && useSideRail) {
                            SpendlySideNavigationRail(
                                currentRoute = currentRoute,
                                onSelectTab = ::navigateTopTab,
                                onAddClick = { openAddTransaction(TransactionType.EXPENSE) },
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = SpendlyTopTab.HOME.route,
                            ) {
                                composable(SpendlyTopTab.HOME.route) {
                                    HomeScreen(
                                        uiState = uiState,
                                        onSelectPeriod = viewModel::setSelectedPeriod,
                                        onToggleTheme = viewModel::setThemeMode,
                                        onOpenSettings = { navigateTopTab(SpendlyTopTab.MORE) },
                                        onAddExpense = { openAddTransaction(TransactionType.EXPENSE) },
                                        onAddIncome = { openAddTransaction(TransactionType.INCOME) },
                                        onAddTransfer = { openAddTransaction(TransactionType.TRANSFER) },
                                        onViewAllTransactions = {
                                            viewModel.setActivityFilters(
                                                period = uiState.selectedPeriod,
                                                type = null,
                                                accountId = null,
                                                categoryId = null,
                                                sortOption = uiState.activitySortOption,
                                            )
                                            navigateTopTab(SpendlyTopTab.ACTIVITY)
                                        },
                                        onManageAccounts = { navController.navigate("accounts") },
                                        onOpenReports = { navController.navigate("reports") },
                                        onOpenAccountDetail = { id -> navController.navigate("account_detail/$id") },
                                        onOpenBudgets = { navigateTopTab(SpendlyTopTab.BUDGETS) },
                                        onOpenTransactionDetail = { inspectedTransaction = it },
                                    )
                                }

                                composable(SpendlyTopTab.ACTIVITY.route) {
                                    ActivityScreen(
                                        uiState = uiState,
                                        onSearchChange = viewModel::setActivitySearch,
                                        onUpdateFilters = viewModel::setActivityFilters,
                                        onClearFilters = viewModel::clearActivityFilters,
                                        onAddTransaction = { openAddTransaction(TransactionType.EXPENSE) },
                                        onEditTransaction = { tx ->
                                            openAddTransaction(tx.type, tx.accountId, tx.id)
                                        },
                                        onDeleteTransaction = ::performDeleteWithUndo,
                                    )
                                }

                                composable(SpendlyTopTab.BUDGETS.route) {
                                    BudgetsScreen(
                                        uiState = uiState,
                                        onSelectPeriod = viewModel::setSelectedPeriod,
                                        onSaveBudget = { existingId, name, scopeVal, catId, limitMinor, currencyCode, period ->
                                            viewModel.saveBudget(existingId, name, scopeVal, catId, limitMinor, currencyCode, period)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Budget saved")
                                            }
                                        },
                                        onDeleteBudget = { id ->
                                            viewModel.deleteBudget(id)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Budget removed")
                                            }
                                        },
                                        onOpenTransactionDetail = { inspectedTransaction = it },
                                    )
                                }

                                composable(SpendlyTopTab.MORE.route) {
                                    MoreScreen(
                                        uiState = uiState,
                                        onNavigateAccounts = { navController.navigate("accounts") },
                                        onNavigateReports = { navController.navigate("reports") },
                                        onNavigateCategories = { navController.navigate("categories") },
                                        onNavigateImportExport = { navController.navigate("import_export") },
                                        onNavigateCurrency = { navController.navigate("currency_settings") },
                                        onNavigateSettingsDetail = { sec -> navController.navigate("settings_detail/$sec") },
                                        onTogglePreviewData = { enabled ->
                                            viewModel.togglePreviewSampleData(enabled)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (enabled) "Interactive preview ledger loaded"
                                                    else "Preview data cleared — inspecting empty states"
                                                )
                                            }
                                        },
                                    )
                                }

                                composable(
                                    route = "add_transaction?type={type}&accountId={accountId}&txId={txId}",
                                    arguments = listOf(
                                        navArgument("type") { type = NavType.StringType; defaultValue = TransactionType.EXPENSE.name },
                                        navArgument("accountId") { type = NavType.StringType; defaultValue = "" },
                                        navArgument("txId") { type = NavType.StringType; defaultValue = "" },
                                    ),
                                ) { entry ->
                                    val typeArg = entry.arguments?.getString("type") ?: TransactionType.EXPENSE.name
                                    val initialType = runCatching { TransactionType.valueOf(typeArg) }.getOrDefault(TransactionType.EXPENSE)
                                    val accArg = entry.arguments?.getString("accountId")?.takeIf { it.isNotBlank() }
                                    val txIdArg = entry.arguments?.getString("txId")?.takeIf { it.isNotBlank() }
                                    val editingTx = txIdArg?.let { id -> uiState.transactions.firstOrNull { it.id == id } }

                                    AddTransactionScreen(
                                        uiState = uiState,
                                        initialType = initialType,
                                        initialAccountId = accArg,
                                        editingTransaction = editingTx,
                                        onNavigateBack = { navController.popBackStack() },
                                        onCreateAccountFirst = {
                                            navController.popBackStack()
                                            navController.navigate("accounts")
                                        },
                                        onSaveTransaction = { existingId, type, amountMinor, currencyCode, accountId, destAccountId, categoryId, desc, merchant, dateMillis, notes ->
                                            viewModel.saveTransaction(
                                                existingId, type, amountMinor, currencyCode, accountId, destAccountId, categoryId, desc, merchant, dateMillis, notes
                                            )
                                            navController.popBackStack()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (existingId != null) "Transaction updated"
                                                    else "${type.label} saved (${MoneyFormatter.formatMinor(amountMinor, currencyCode)})"
                                                )
                                            }
                                        },
                                    )
                                }

                                composable("accounts") {
                                    AccountsScreen(
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onOpenAccountDetail = { id -> navController.navigate("account_detail/$id") },
                                        onSaveAccount = { existingId, name, type, inst, net, last4, curr, openMinor, limitMinor, desc ->
                                            viewModel.saveAccount(existingId, name, type, inst, net, last4, curr, openMinor, limitMinor, desc)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Account \"$name\" saved")
                                            }
                                        },
                                    )
                                }

                                composable(
                                    route = "account_detail/{accountId}",
                                    arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
                                ) { entry ->
                                    val accId = entry.arguments?.getString("accountId") ?: ""
                                    AccountDetailScreen(
                                        accountId = accId,
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onRecordTransactionForAccount = { id ->
                                            openAddTransaction(TransactionType.EXPENSE, accountId = id)
                                        },
                                        onSaveAccount = { existingId, name, type, inst, net, last4, curr, openMinor, limitMinor, desc ->
                                            viewModel.saveAccount(existingId, name, type, inst, net, last4, curr, openMinor, limitMinor, desc)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Account updated")
                                            }
                                        },
                                        onToggleArchive = { id ->
                                            viewModel.toggleArchiveAccount(id)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Account archive status updated")
                                            }
                                        },
                                        onDeleteAccount = { id ->
                                            viewModel.deleteAccount(id)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Account deleted")
                                            }
                                        },
                                        onOpenTransactionDetail = { inspectedTransaction = it },
                                    )
                                }

                                composable("reports") {
                                    ReportsScreen(
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onSelectReportPeriod = viewModel::setReportPeriod,
                                        onOpenCategoryInActivity = { catId ->
                                            viewModel.setActivityFilters(
                                                period = null,
                                                type = TransactionType.EXPENSE,
                                                accountId = null,
                                                categoryId = catId,
                                                sortOption = uiState.activitySortOption,
                                            )
                                            navigateTopTab(SpendlyTopTab.ACTIVITY)
                                        },
                                        onAddTransaction = { openAddTransaction(TransactionType.EXPENSE) },
                                    )
                                }

                                composable("categories") {
                                    CategoriesScreen(
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onAddCategory = { name, type, icon ->
                                            viewModel.addCategory(name, type, icon)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Category \"$name\" added")
                                            }
                                        },
                                    )
                                }

                                composable("import_export") {
                                    ImportExportScreen(
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        isDuplicateCandidate = viewModel::isDuplicateCandidate,
                                        onCommitCsvImport = viewModel::commitCsvImport,
                                        onRestoreBackupSample = { viewModel.togglePreviewSampleData(true) },
                                    )
                                }

                                composable("currency_settings") {
                                    CurrencySettingsScreen(
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onSetDefaultCurrency = { code ->
                                            viewModel.setDefaultCurrency(code)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Default currency updated to $code")
                                            }
                                        },
                                    )
                                }

                                composable(
                                    route = "settings_detail/{section}",
                                    arguments = listOf(navArgument("section") { type = NavType.StringType }),
                                ) { entry ->
                                    val section = entry.arguments?.getString("section") ?: "about"
                                    SettingsDetailScreen(
                                        section = section,
                                        uiState = uiState,
                                        onNavigateBack = { navController.popBackStack() },
                                        onSetThemeMode = viewModel::setThemeMode,
                                        onSetLanguage = viewModel::setLanguage,
                                        onUpdatePreferences = viewModel::updateProfileAndPreferences,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            uiState = uiState,
            onDismiss = { inspectedTransaction = null },
            onEdit = {
                inspectedTransaction = null
                openAddTransaction(tx.type, tx.accountId, tx.id)
            },
            onRequestDelete = {
                inspectedTransaction = null
                pendingDeleteTransaction = tx
            },
        )
    }

    pendingDeleteTransaction?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text("Delete Transaction?") },
            text = {
                Text("Remove \"${tx.description}\" (${MoneyFormatter.formatMinor(tx.amountMinor, tx.currencyCode)})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        performDeleteWithUndo(tx)
                        pendingDeleteTransaction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeleteTransaction = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
