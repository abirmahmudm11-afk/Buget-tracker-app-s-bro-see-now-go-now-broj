package com.example.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SpendlyRepository
import com.example.model.AccountRecord
import com.example.model.AccountSummary
import com.example.model.AccountType
import com.example.model.BudgetRecord
import com.example.model.BudgetScope
import com.example.model.BudgetSummary
import com.example.model.CategoryRecord
import com.example.model.CsvImportRow
import com.example.model.HomeTransactionFilter
import com.example.model.MonthPeriod
import com.example.model.ReportPeriod
import com.example.model.SortOption
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UserProfileRecord
import com.example.ui.theme.ThemeMode
import com.example.util.ConversionResult
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SpendlyEvent {
    data class BudgetAlert(val title: String, val message: String, val notificationId: Int) : SpendlyEvent
    data class SnackbarMessage(val message: String) : SpendlyEvent
}

private data class LocalViewFilters(
    val selectedPeriod: MonthPeriod = MonthPeriod.current(),
    val homeFilter: HomeTransactionFilter = HomeTransactionFilter.ALL,
    val lastDeletedTransaction: TransactionRecord? = null,
    val activityFilterPeriod: MonthPeriod? = null,
    val activityFilterCategoryId: String? = null,
    val activityFilterAccountId: String? = null,
    val activityFilterType: TransactionType? = null,
    val activitySearchQuery: String = "",
    val activitySortOption: SortOption = SortOption.NEWEST,
    val reportPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
)

private sealed interface StreamState<out T> {
    data object Loading : StreamState<Nothing>
    data class Ready<T>(val data: T) : StreamState<T>
    data class Failed(val message: String) : StreamState<Nothing>
}

@Immutable
data class SpendlyUiState(
    val isLoading: Boolean = true,
    val syncError: String? = null,
    val isSyncing: Boolean = false,
    val onboardingComplete: Boolean = false,
    val defaultCurrencyCode: String = "USD",
    val selectedLanguage: String = "English",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val profileName: String = "Alex Morgan",
    val budgetAlertsEnabled: Boolean = true,
    val weeklyDigestEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val privacyHideBalances: Boolean = false,
    val isPreviewDataActive: Boolean = false,
    val selectedPeriod: MonthPeriod = MonthPeriod.current(),
    val homeFilter: HomeTransactionFilter = HomeTransactionFilter.ALL,
    val accounts: List<AccountRecord> = emptyList(),
    val categories: List<CategoryRecord> = defaultCategories(),
    val transactions: List<TransactionRecord> = emptyList(),
    val budgets: List<BudgetRecord> = emptyList(),
    val lastDeletedTransaction: TransactionRecord? = null,
    val activityFilterPeriod: MonthPeriod? = null,
    val activityFilterCategoryId: String? = null,
    val activityFilterAccountId: String? = null,
    val activityFilterType: TransactionType? = null,
    val activitySearchQuery: String = "",
    val activitySortOption: SortOption = SortOption.NEWEST,
    val reportPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    // Precomputed fields (calculated once off the main thread in combine + flowOn(Dispatchers.Default))
    val accountsMap: Map<String, AccountRecord> = emptyMap(),
    val categoriesMap: Map<String, CategoryRecord> = defaultCategories().associateBy { it.id },
    val availablePeriods: List<MonthPeriod> = emptyList(),
    val activeAccounts: List<AccountRecord> = emptyList(),
    val archivedAccounts: List<AccountRecord> = emptyList(),
    val accountSummaries: List<AccountSummary> = emptyList(),
    val totalAssetsBaseMinor: Long = 0L,
    val totalCreditOutstandingBaseMinor: Long = 0L,
    val totalNetBalanceBaseMinor: Long = 0L,
    val monthlyTransactions: List<TransactionRecord> = emptyList(),
    val monthlyExpensesBaseMinor: Long = 0L,
    val monthlyIncomeBaseMinor: Long = 0L,
    val monthlyNetCashFlowBaseMinor: Long = 0L,
    val overallBudgetSummary: BudgetSummary? = null,
    val categoryBudgetSummaries: List<BudgetSummary> = emptyList(),
    val remainingOverallBudgetBaseMinor: Long? = null,
) {
    fun accountById(id: String): AccountRecord? = accountsMap[id] ?: accounts.firstOrNull { it.id == id }
    fun categoryById(id: String?): CategoryRecord? = if (id == null) null else (categoriesMap[id] ?: categories.firstOrNull { it.id == id })

    fun convertToBase(amountMinor: Long, currencyCode: String): Long? {
        return when (val res = MoneyFormatter.convertMinor(amountMinor, currencyCode, defaultCurrencyCode)) {
            is ConversionResult.Available -> res.convertedMinor
            is ConversionResult.Unavailable -> null
        }
    }
}

class SpendlyViewModel(
    private val repository: SpendlyRepository,
    private val currentUserId: String,
    initialDisplayName: String = "Spendly User",
) : ViewModel() {

    private val localFilters = MutableStateFlow(LocalViewFilters())
    private val _events = MutableSharedFlow<SpendlyEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SpendlyEvent> = _events.asSharedFlow()

    private fun <T> Flow<T>.asStreamState(label: String): Flow<StreamState<T>> {
        return kotlinx.coroutines.flow.flow<StreamState<T>> {
            collect { emit(StreamState.Ready(it)) }
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing $label", rootError)
            emit(StreamState.Failed(rootError.localizedMessage ?: "Failed to sync $label"))
        }
    }

    private val profileFlow = repository.observeUserProfile(currentUserId).asStreamState("profile")
    private val accountsFlow = repository.observeAccounts(currentUserId).asStreamState("accounts")
    private val transactionsFlow = repository.observeTransactions(currentUserId).asStreamState("transactions")
    private val budgetsFlow = repository.observeBudgets(currentUserId).asStreamState("budgets")
    private val categoriesFlow = repository.observeCategories(currentUserId).asStreamState("categories")

    private val remoteStateFlow = combine(
        profileFlow,
        accountsFlow,
        transactionsFlow,
        budgetsFlow,
        categoriesFlow,
    ) { profileState, accState, txState, budState, catState ->
        RemoteBundle(profileState, accState, txState, budState, catState)
    }

    val uiState: StateFlow<SpendlyUiState> = combine(
        remoteStateFlow,
        localFilters,
    ) { remote, local ->
        val anyLoading = remote.profile is StreamState.Loading ||
            remote.accounts is StreamState.Loading ||
            remote.transactions is StreamState.Loading ||
            remote.budgets is StreamState.Loading ||
            remote.categories is StreamState.Loading

        val streamError = listOfNotNull(
            (remote.profile as? StreamState.Failed)?.message,
            (remote.accounts as? StreamState.Failed)?.message,
            (remote.transactions as? StreamState.Failed)?.message,
            (remote.budgets as? StreamState.Failed)?.message,
            (remote.categories as? StreamState.Failed)?.message,
        ).firstOrNull()

        val profile = (remote.profile as? StreamState.Ready)?.data
        val accounts = (remote.accounts as? StreamState.Ready)?.data.orEmpty()
        val transactions = (remote.transactions as? StreamState.Ready)?.data.orEmpty()
        val budgets = (remote.budgets as? StreamState.Ready)?.data.orEmpty()
        val remoteCategories = (remote.categories as? StreamState.Ready)?.data.orEmpty()

        computeSpendlyUiState(
            profile = profile,
            accounts = accounts,
            transactions = transactions,
            budgets = budgets,
            remoteCategories = remoteCategories,
            selectedPeriod = local.selectedPeriod,
            homeFilter = local.homeFilter,
            lastDeletedTransaction = local.lastDeletedTransaction,
            activityFilterPeriod = local.activityFilterPeriod,
            activityFilterCategoryId = local.activityFilterCategoryId,
            activityFilterAccountId = local.activityFilterAccountId,
            activityFilterType = local.activityFilterType,
            activitySearchQuery = local.activitySearchQuery,
            activitySortOption = local.activitySortOption,
            reportPeriod = local.reportPeriod,
            isLoading = anyLoading && profile == null,
            syncError = local.syncError ?: streamError,
            isSyncing = local.isSyncing,
            initialDisplayName = initialDisplayName,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SpendlyUiState(isLoading = true, profileName = initialDisplayName),
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                UserProfileRecord(
                    userId = currentUserId,
                    profileName = initialDisplayName.ifBlank { "Spendly User" },
                    onboardingComplete = false,
                    isPreviewDataActive = false,
                )
            )
        }
    }

    fun clearSyncError() {
        localFilters.update { it.copy(syncError = null) }
    }

    fun completeOnboarding(
        currencyCode: String,
        language: String,
        loadPreviewSample: Boolean,
    ) {
        viewModelScope.launch {
            localFilters.update { it.copy(isSyncing = true, syncError = null) }
            val ensureRes = repository.ensureUserProfile(
                UserProfileRecord(
                    userId = currentUserId,
                    profileName = uiState.value.profileName.ifBlank { "Spendly User" },
                    defaultCurrencyCode = currencyCode,
                    selectedLanguage = language,
                    onboardingComplete = true,
                    isPreviewDataActive = loadPreviewSample,
                )
            )
            if (ensureRes.isFailure) {
                localFilters.update { it.copy(isSyncing = false, syncError = "Could not initialize profile") }
                return@launch
            }
            repository.updateUserProfileFields(
                mapOf(
                    "defaultCurrencyCode" to currencyCode,
                    "selectedLanguage" to language,
                    "onboardingComplete" to true,
                    "previewDataActive" to loadPreviewSample,
                )
            )
            val seedRes = if (loadPreviewSample) {
                repository.seedLedgerData(
                    accounts = sampleAccounts(),
                    transactions = sampleTransactions(),
                    budgets = sampleBudgets(),
                    categories = defaultCategories(),
                    previewDataActive = true,
                )
            } else {
                repository.seedLedgerData(
                    accounts = emptyList(),
                    transactions = emptyList(),
                    budgets = emptyList(),
                    categories = defaultCategories(),
                    previewDataActive = false,
                )
            }
            localFilters.update {
                it.copy(
                    isSyncing = false,
                    syncError = seedRes.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun togglePreviewSampleData(enableSample: Boolean) {
        viewModelScope.launch {
            localFilters.update { it.copy(isSyncing = true, syncError = null, lastDeletedTransaction = null) }
            val result = if (enableSample) {
                repository.seedLedgerData(
                    accounts = sampleAccounts(),
                    transactions = sampleTransactions(),
                    budgets = sampleBudgets(),
                    categories = defaultCategories(),
                    previewDataActive = true,
                )
            } else {
                val snap = uiState.value
                repository.clearAllLedgerRecords(
                    accountIds = snap.accounts.map { it.id },
                    transactionIds = snap.transactions.map { it.id },
                    budgetIds = snap.budgets.map { it.id },
                )
            }
            localFilters.update {
                it.copy(
                    isSyncing = false,
                    syncError = result.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun restorePreviewSnapshot() {
        viewModelScope.launch {
            localFilters.update { it.copy(isSyncing = true, syncError = null, lastDeletedTransaction = null) }
            val snap = uiState.value
            val clearRes = repository.clearAllLedgerRecords(
                accountIds = snap.accounts.map { it.id },
                transactionIds = snap.transactions.map { it.id },
                budgetIds = snap.budgets.map { it.id },
            )
            if (clearRes.isFailure) {
                localFilters.update {
                    it.copy(isSyncing = false, syncError = clearRes.exceptionOrNull()?.localizedMessage)
                }
                return@launch
            }
            val seedRes = repository.seedLedgerData(
                accounts = sampleAccounts(),
                transactions = sampleTransactions(),
                budgets = sampleBudgets(),
                categories = defaultCategories(),
                previewDataActive = true,
            )
            localFilters.update {
                it.copy(
                    isSyncing = false,
                    syncError = seedRes.exceptionOrNull()?.localizedMessage,
                )
            }
        }
    }

    fun setDefaultCurrency(code: String) {
        viewModelScope.launch {
            repository.updateUserProfileFields(mapOf("defaultCurrencyCode" to code))
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            repository.updateUserProfileFields(mapOf("selectedLanguage" to language))
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateUserProfileFields(mapOf("themeMode" to mode.name))
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    fun setSelectedPeriod(period: MonthPeriod) {
        localFilters.update { it.copy(selectedPeriod = period) }
    }

    fun setHomeFilter(filter: HomeTransactionFilter) {
        localFilters.update { it.copy(homeFilter = filter) }
    }

    fun updateProfileAndPreferences(
        profileName: String? = null,
        budgetAlertsEnabled: Boolean? = null,
        weeklyDigestEnabled: Boolean? = null,
        dailyReminderEnabled: Boolean? = null,
        privacyHideBalances: Boolean? = null,
    ) {
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>()
            profileName?.trim()?.takeIf { it.isNotBlank() }?.let { updates["profileName"] = it.take(80) }
            budgetAlertsEnabled?.let { updates["budgetAlertsEnabled"] = it }
            weeklyDigestEnabled?.let { updates["weeklyDigestEnabled"] = it }
            dailyReminderEnabled?.let { updates["dailyReminderEnabled"] = it }
            privacyHideBalances?.let { updates["privacyHideBalances"] = it }
            if (updates.isNotEmpty()) {
                repository.updateUserProfileFields(updates)
                    .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
            }
        }
    }

    // Activity Filters
    fun setActivitySearch(query: String) {
        localFilters.update { it.copy(activitySearchQuery = query) }
    }

    fun setActivityFilters(
        period: MonthPeriod? = localFilters.value.activityFilterPeriod,
        type: TransactionType? = localFilters.value.activityFilterType,
        accountId: String? = localFilters.value.activityFilterAccountId,
        categoryId: String? = localFilters.value.activityFilterCategoryId,
        sortOption: SortOption = localFilters.value.activitySortOption,
    ) {
        localFilters.update {
            it.copy(
                activityFilterPeriod = period,
                activityFilterType = type,
                activityFilterAccountId = accountId,
                activityFilterCategoryId = categoryId,
                activitySortOption = sortOption,
            )
        }
    }

    fun clearActivityFilters() {
        localFilters.update {
            it.copy(
                activityFilterPeriod = null,
                activityFilterType = null,
                activityFilterAccountId = null,
                activityFilterCategoryId = null,
                activitySearchQuery = "",
                activitySortOption = SortOption.NEWEST,
            )
        }
    }

    fun setReportPeriod(period: ReportPeriod) {
        localFilters.update { it.copy(reportPeriod = period) }
    }

    // Transactions
    fun saveTransaction(
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
    ) {
        viewModelScope.launch {
            val current = uiState.value
            val destAcc = if (type == TransactionType.TRANSFER && destinationAccountId != null) {
                current.accountById(destinationAccountId)
            } else null
            val isCcRepayment = type == TransactionType.TRANSFER && destAcc?.type == AccountType.CREDIT_CARD

            val record = TransactionRecord(
                id = existingId ?: "tx_${UUID.randomUUID().toString().take(8)}",
                type = type,
                amountMinor = amountMinor,
                currencyCode = currencyCode,
                accountId = accountId,
                destinationAccountId = if (type == TransactionType.TRANSFER) destinationAccountId else null,
                categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
                description = description.trim().ifBlank { merchant.trim().ifBlank { type.label } }.take(160),
                merchant = merchant.trim().take(100),
                dateMillis = dateMillis,
                notes = notes.trim().take(500),
                isCreditCardRepayment = isCcRepayment,
            )

            repository.saveTransaction(record, isUpdate = existingId != null)
                .onSuccess {
                    if (type == TransactionType.EXPENSE && current.budgetAlertsEnabled) {
                        checkAndEmitBudgetAlert(current, record)
                    }
                }
                .onFailure { err ->
                    localFilters.update { it.copy(syncError = err.localizedMessage ?: "Failed to save transaction") }
                }
        }
    }

    fun quickLogExpense(
        amountMinor: Long,
        categoryId: String,
        dateMillis: Long,
        accountId: String?,
        note: String,
    ) {
        viewModelScope.launch {
            val current = uiState.value
            val targetAccount = accountId?.let { current.accountById(it) }
                ?: current.activeAccounts.firstOrNull()

            val resolvedAccountId = if (targetAccount != null) {
                targetAccount.id
            } else {
                val freshId = if (current.accountById("acc_cash_default") == null) {
                    "acc_cash_default"
                } else {
                    "acc_cash_${UUID.randomUUID().toString().take(8)}"
                }
                val defaultWallet = AccountRecord(
                    id = freshId,
                    name = "Cash Wallet",
                    type = AccountType.CASH_WALLET,
                    institutionName = "Personal",
                    currencyCode = current.defaultCurrencyCode,
                    openingBalanceMinor = 0L,
                    description = "Auto-created wallet for quick expenses",
                )
                val accRes = repository.saveAccount(defaultWallet, isUpdate = false)
                if (accRes.isFailure) {
                    localFilters.update { it.copy(syncError = "Could not create default wallet") }
                    return@launch
                }
                defaultWallet.id
            }

            val resolvedCurrency = targetAccount?.currencyCode ?: current.defaultCurrencyCode
            val catName = current.categoryById(categoryId)?.name ?: "Expense"
            val trimmedNote = note.trim()
            val tx = TransactionRecord(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                type = TransactionType.EXPENSE,
                amountMinor = amountMinor,
                currencyCode = resolvedCurrency,
                accountId = resolvedAccountId,
                categoryId = categoryId,
                description = trimmedNote.ifBlank { catName }.take(160),
                merchant = trimmedNote.take(100),
                dateMillis = dateMillis,
                notes = trimmedNote.take(500),
            )
            repository.saveTransaction(tx, isUpdate = false)
                .onSuccess {
                    _events.tryEmit(
                        SpendlyEvent.SnackbarMessage(
                            "Expense logged (${MoneyFormatter.formatMinor(amountMinor, resolvedCurrency)})"
                        )
                    )
                    if (current.budgetAlertsEnabled) {
                        checkAndEmitBudgetAlert(current, tx)
                    }
                }
                .onFailure { err ->
                    localFilters.update { it.copy(syncError = err.localizedMessage ?: "Failed to save expense") }
                }
        }
    }

    private fun checkAndEmitBudgetAlert(state: SpendlyUiState, newExpense: TransactionRecord) {
        val period = DateFormatters.monthPeriodOf(newExpense.dateMillis)
        val monthTxs = state.transactions.filter {
            it.id != newExpense.id && DateFormatters.monthPeriodOf(it.dateMillis) == period
        } + newExpense

        val matchingBudgets = state.budgets.filter { b ->
            b.year == period.year && b.month == period.month && (
                b.scope == BudgetScope.OVERALL ||
                    (b.scope == BudgetScope.CATEGORY && b.categoryId == newExpense.categoryId)
                )
        }

        matchingBudgets.forEach { b ->
            val relevantTxs = if (b.scope == BudgetScope.OVERALL) {
                monthTxs.filter { it.type == TransactionType.EXPENSE }
            } else {
                monthTxs.filter { it.type == TransactionType.EXPENSE && it.categoryId == b.categoryId }
            }
            val spent = relevantTxs.sumOf { tx ->
                when (val c = MoneyFormatter.convertMinor(tx.amountMinor, tx.currencyCode, b.currencyCode)) {
                    is ConversionResult.Available -> c.convertedMinor
                    is ConversionResult.Unavailable -> 0L
                }
            }
            val ratio = if (b.limitMinor > 0L) spent.toFloat() / b.limitMinor.toFloat() else 0f
            if (ratio >= 1.0f) {
                val over = MoneyFormatter.formatMinor(spent - b.limitMinor, b.currencyCode)
                _events.tryEmit(
                    SpendlyEvent.BudgetAlert(
                        title = "Over Budget: ${b.name}",
                        message = "You've exceeded your ${b.name} limit by $over.",
                        notificationId = b.id.hashCode(),
                    )
                )
            } else if (ratio >= 0.85f) {
                val pct = (ratio * 100).toInt()
                val left = MoneyFormatter.formatMinor(b.limitMinor - spent, b.currencyCode)
                _events.tryEmit(
                    SpendlyEvent.BudgetAlert(
                        title = "Approaching Limit: ${b.name}",
                        message = "$pct% used — $left remaining for ${DateFormatters.formatShortMonth(period)}.",
                        notificationId = b.id.hashCode(),
                    )
                )
            }
        }
    }

    fun deleteTransaction(id: String) {
        val target = uiState.value.transactions.firstOrNull { it.id == id }
        localFilters.update { it.copy(lastDeletedTransaction = target) }
        viewModelScope.launch {
            repository.deleteTransaction(id)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    fun undoDeleteTransaction() {
        val deleted = localFilters.value.lastDeletedTransaction ?: return
        localFilters.update { it.copy(lastDeletedTransaction = null) }
        viewModelScope.launch {
            repository.saveTransaction(deleted, isUpdate = false)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    // Accounts
    fun saveAccount(
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
    ): String {
        val existing = existingId?.let { uiState.value.accountById(it) }
        val resolvedId = existingId ?: "acc_${UUID.randomUUID().toString().take(8)}"
        val record = AccountRecord(
            id = resolvedId,
            name = name.trim().take(80),
            type = type,
            institutionName = institutionName.trim().take(80),
            cardNetwork = if (type.supportsCardDetails) cardNetwork.trim().take(40) else "",
            lastFour = if (type.supportsCardDetails) lastFour.filter { it.isDigit() }.take(4) else "",
            currencyCode = currencyCode,
            openingBalanceMinor = openingBalanceMinor,
            creditLimitMinor = if (type == AccountType.CREDIT_CARD) creditLimitMinor else null,
            description = description.trim().take(240),
            isArchived = existing?.isArchived ?: false,
        )
        viewModelScope.launch {
            repository.saveAccount(record, isUpdate = existingId != null)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
        return resolvedId
    }

    fun toggleArchiveAccount(accountId: String) {
        val acc = uiState.value.accountById(accountId) ?: return
        viewModelScope.launch {
            repository.toggleArchiveAccount(accountId, !acc.isArchived)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    fun deleteAccount(accountId: String) {
        val linkedTxIds = uiState.value.transactions
            .filter { it.accountId == accountId || it.destinationAccountId == accountId }
            .map { it.id }
        viewModelScope.launch {
            repository.deleteAccountCascade(accountId, linkedTxIds)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    // Budgets
    fun saveBudget(
        existingId: String?,
        name: String,
        scope: BudgetScope,
        categoryId: String?,
        limitMinor: Long,
        currencyCode: String,
        period: MonthPeriod,
    ) {
        val currentBudgets = uiState.value.budgets
        val replacing = currentBudgets.filter { existing ->
            if (existingId != null) false
            else if (scope == BudgetScope.OVERALL) {
                existing.scope == BudgetScope.OVERALL &&
                    existing.year == period.year &&
                    existing.month == period.month
            } else {
                existing.scope == BudgetScope.CATEGORY &&
                    existing.categoryId == categoryId &&
                    existing.year == period.year &&
                    existing.month == period.month
            }
        }.map { it.id }

        val record = BudgetRecord(
            id = existingId ?: "bud_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            scope = scope,
            categoryId = if (scope == BudgetScope.CATEGORY) categoryId else null,
            limitMinor = limitMinor,
            currencyCode = currencyCode,
            year = period.year,
            month = period.month,
        )
        viewModelScope.launch {
            repository.saveBudget(
                budget = record,
                isUpdate = existingId != null,
                replacingBudgetIds = replacing,
            ).onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(budgetId)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    // Categories
    fun addCategory(name: String, type: TransactionType, iconKey: String) {
        val record = CategoryRecord(
            id = "cat_${UUID.randomUUID().toString().take(8)}",
            name = name.trim().take(60),
            type = type,
            iconKey = iconKey,
            isDefault = false,
            isActive = true,
        )
        viewModelScope.launch {
            repository.saveCategory(record)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
    }

    // CSV Import Preview Commit
    fun commitCsvImport(
        rows: List<CsvImportRow>,
        destinationAccountId: String,
        currencyCode: String,
    ): Int {
        val validRows = rows.filter {
            it.errorMessage == null && !it.isDuplicate && it.parsedAmountMinor != null && it.parsedDateMillis != null
        }
        val newTx = validRows.map { row ->
            TransactionRecord(
                id = "imp_${UUID.randomUUID().toString().take(8)}",
                type = row.type,
                amountMinor = row.parsedAmountMinor!!,
                currencyCode = currencyCode,
                accountId = destinationAccountId,
                categoryId = row.categoryId,
                description = row.description,
                merchant = row.description,
                dateMillis = row.parsedDateMillis!!,
                notes = "Imported via CSV Wizard",
            )
        }
        if (newTx.isNotEmpty()) {
            viewModelScope.launch {
                repository.commitBatchTransactions(newTx)
                    .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
            }
        }
        return newTx.size
    }

    fun isDuplicateCandidate(dateMillis: Long, amountMinor: Long, description: String, accountId: String): Boolean {
        val targetDay = DateFormatters.toDayKey(dateMillis)
        return uiState.value.transactions.any { tx ->
            tx.accountId == accountId &&
                tx.amountMinor == amountMinor &&
                DateFormatters.toDayKey(tx.dateMillis) == targetDay &&
                tx.description.equals(description.trim(), ignoreCase = true)
        }
    }

    private data class RemoteBundle(
        val profile: StreamState<UserProfileRecord?>,
        val accounts: StreamState<List<AccountRecord>>,
        val transactions: StreamState<List<TransactionRecord>>,
        val budgets: StreamState<List<BudgetRecord>>,
        val categories: StreamState<List<CategoryRecord>>,
    )

    private companion object {
        const val TAG = "SpendlyVM"
    }
}

fun defaultCategories(): List<CategoryRecord> = listOf(
    CategoryRecord("cat_groceries", "Groceries", TransactionType.EXPENSE, "groceries"),
    CategoryRecord("cat_dining", "Dining & Coffee", TransactionType.EXPENSE, "dining"),
    CategoryRecord("cat_housing", "Rent & Housing", TransactionType.EXPENSE, "housing"),
    CategoryRecord("cat_transport", "Transit & Fuel", TransactionType.EXPENSE, "transport"),
    CategoryRecord("cat_utilities", "Utilities & Bills", TransactionType.EXPENSE, "utilities"),
    CategoryRecord("cat_shopping", "Shopping", TransactionType.EXPENSE, "shopping"),
    CategoryRecord("cat_health", "Health & Wellness", TransactionType.EXPENSE, "health"),
    CategoryRecord("cat_travel", "Travel", TransactionType.EXPENSE, "travel"),
    CategoryRecord("cat_salary", "Salary", TransactionType.INCOME, "salary"),
    CategoryRecord("cat_freelance", "Freelance", TransactionType.INCOME, "freelance"),
    CategoryRecord("cat_interest", "Interest & Dividends", TransactionType.INCOME, "interest"),
)

private fun dateAt(year: Int, month: Int, day: Int, hour: Int = 12): Long {
    val now = Calendar.getInstance()
    val curYear = now.get(Calendar.YEAR)
    val curMonth = now.get(Calendar.MONTH) + 1
    val curDay = now.get(Calendar.DAY_OF_MONTH)
    return Calendar.getInstance().apply {
        set(year, month - 1, 1, hour, 0, 0)
        set(Calendar.MILLISECOND, 0)
        val maxInMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
        val upperBound = if (year == curYear && month == curMonth) minOf(curDay, maxInMonth) else maxInMonth
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, upperBound))
    }.timeInMillis
}

private fun sampleAccounts(): List<AccountRecord> = listOf(
    AccountRecord(
        id = "acc_checking",
        name = "Private Checking",
        type = AccountType.BANK_ACCOUNT,
        institutionName = "Chase Private Client",
        currencyCode = "USD",
        openingBalanceMinor = 1485000L,
        description = "Primary household & operating checking ledger",
    ),
    AccountRecord(
        id = "acc_sapphire",
        name = "Sapphire Reserve",
        type = AccountType.CREDIT_CARD,
        institutionName = "Chase Bank",
        cardNetwork = "Visa",
        lastFour = "4821",
        currencyCode = "USD",
        openingBalanceMinor = 42000L,
        creditLimitMinor = 1500000L,
        description = "Travel & dining rewards card — auto-pay enabled",
    ),
    AccountRecord(
        id = "acc_amex",
        name = "Gold Reserve",
        type = AccountType.CREDIT_CARD,
        institutionName = "American Express",
        cardNetwork = "Amex",
        lastFour = "3008",
        currencyCode = "USD",
        openingBalanceMinor = 18500L,
        creditLimitMinor = 1000000L,
        description = "Dining & everyday statement card",
    ),
    AccountRecord(
        id = "acc_euro_travel",
        name = "Euro Multi-Currency",
        type = AccountType.PREPAID_CARD,
        institutionName = "Wise",
        cardNetwork = "Mastercard",
        lastFour = "9014",
        currencyCode = "EUR",
        openingBalanceMinor = 245000L,
        description = "International settlement & European travel wallet",
    ),
    AccountRecord(
        id = "acc_savings",
        name = "Treasury Reserve",
        type = AccountType.SAVINGS_ACCOUNT,
        institutionName = "Marcus Reserve",
        currencyCode = "USD",
        openingBalanceMinor = 3850000L,
        description = "High-yield liquidity reserve",
    ),
    AccountRecord(
        id = "acc_cash",
        name = "Cash Pocket",
        type = AccountType.CASH_WALLET,
        institutionName = "Physical Wallet",
        currencyCode = "USD",
        openingBalanceMinor = 34000L,
        description = "Petty cash & incidentals",
    ),
)

private fun sampleTransactions(): List<TransactionRecord> {
    val cur = MonthPeriod.current()
    val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    val prevY = prevCal.get(Calendar.YEAR)
    val prevM = prevCal.get(Calendar.MONTH) + 1
    val twoCal = Calendar.getInstance().apply { add(Calendar.MONTH, -2) }
    val twoY = twoCal.get(Calendar.YEAR)
    val twoM = twoCal.get(Calendar.MONTH) + 1
    val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    fun curDay(daysAgo: Int): Int = (todayDay - daysAgo).coerceAtLeast(1)

    return listOf(
        TransactionRecord(
            id = "tx_1",
            type = TransactionType.EXPENSE,
            amountMinor = 14860L,
            currencyCode = "USD",
            accountId = "acc_amex",
            categoryId = "cat_groceries",
            description = "Whole Foods Market",
            merchant = "Whole Foods Market",
            dateMillis = dateAt(cur.year, cur.month, curDay(1), 11),
            notes = "Weekly organic produce & household pantry",
        ),
        TransactionRecord(
            id = "tx_2",
            type = TransactionType.EXPENSE,
            amountMinor = 1840L,
            currencyCode = "USD",
            accountId = "acc_cash",
            categoryId = "cat_dining",
            description = "Blue Bottle Espresso",
            merchant = "Blue Bottle Coffee",
            dateMillis = dateAt(cur.year, cur.month, curDay(2), 9),
        ),
        TransactionRecord(
            id = "tx_3",
            type = TransactionType.TRANSFER,
            amountMinor = 65000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            destinationAccountId = "acc_sapphire",
            description = "Sapphire Reserve Statement Pay",
            merchant = "Internal Repayment",
            dateMillis = dateAt(cur.year, cur.month, curDay(3), 16),
            notes = "Monthly statement balance settlement",
            isCreditCardRepayment = true,
        ),
        TransactionRecord(
            id = "tx_4",
            type = TransactionType.EXPENSE,
            amountMinor = 8600L,
            currencyCode = "EUR",
            accountId = "acc_euro_travel",
            categoryId = "cat_travel",
            description = "Eurostar Business Lounge",
            merchant = "Eurostar",
            dateMillis = dateAt(cur.year, cur.month, curDay(4), 14),
        ),
        TransactionRecord(
            id = "tx_5",
            type = TransactionType.EXPENSE,
            amountMinor = 285000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_housing",
            description = "Monthly Residence Lease",
            merchant = "Kensington Residences",
            dateMillis = dateAt(cur.year, cur.month, 1, 9),
        ),
        TransactionRecord(
            id = "tx_6",
            type = TransactionType.INCOME,
            amountMinor = 945000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_salary",
            description = "Executive Payroll Deposit",
            merchant = "Northstar Capital",
            dateMillis = dateAt(cur.year, cur.month, curDay(6), 8),
        ),
        TransactionRecord(
            id = "tx_7",
            type = TransactionType.EXPENSE,
            amountMinor = 24600L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_utilities",
            description = "Fiber Internet & Grid Power",
            merchant = "ConEdison & Sonic",
            dateMillis = dateAt(cur.year, cur.month, curDay(8), 11),
        ),
        TransactionRecord(
            id = "tx_8",
            type = TransactionType.EXPENSE,
            amountMinor = 22400L,
            currencyCode = "USD",
            accountId = "acc_amex",
            categoryId = "cat_dining",
            description = "Client Dinner — L'Atelier",
            merchant = "L'Atelier Bistro",
            dateMillis = dateAt(cur.year, cur.month, curDay(10), 20),
        ),
        TransactionRecord(
            id = "tx_9",
            type = TransactionType.INCOME,
            amountMinor = 215000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_freelance",
            description = "Advisory Retainer Payout",
            merchant = "Stripe Payout",
            dateMillis = dateAt(cur.year, cur.month, curDay(12), 15),
        ),
        TransactionRecord(
            id = "tx_10",
            type = TransactionType.EXPENSE,
            amountMinor = 19500L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_health",
            description = "Equinox Club Membership",
            merchant = "Equinox",
            dateMillis = dateAt(cur.year, cur.month, curDay(14), 7),
        ),
        TransactionRecord(
            id = "tx_11",
            type = TransactionType.INCOME,
            amountMinor = 14850L,
            currencyCode = "USD",
            accountId = "acc_savings",
            categoryId = "cat_interest",
            description = "Treasury Monthly Yield",
            merchant = "Marcus Reserve",
            dateMillis = dateAt(cur.year, cur.month, curDay(5), 9),
        ),
        TransactionRecord(
            id = "tx_12",
            type = TransactionType.EXPENSE,
            amountMinor = 31200L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_shopping",
            description = "Apple Store Accessories",
            merchant = "Apple",
            dateMillis = dateAt(cur.year, cur.month, curDay(7), 15),
            notes = "USB-C travel dock & protective sleeve",
        ),
        TransactionRecord(
            id = "tx_13",
            type = TransactionType.EXPENSE,
            amountMinor = 6850L,
            currencyCode = "USD",
            accountId = "acc_cash",
            categoryId = "cat_transport",
            description = "Metro & Airport Express",
            merchant = "City Transit",
            dateMillis = dateAt(cur.year, cur.month, curDay(9), 18),
        ),
        TransactionRecord(
            id = "tx_prev_1",
            type = TransactionType.INCOME,
            amountMinor = 945000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_salary",
            description = "Previous Month Payroll",
            merchant = "Northstar Capital",
            dateMillis = dateAt(prevY, prevM, 15, 8),
        ),
        TransactionRecord(
            id = "tx_prev_2",
            type = TransactionType.EXPENSE,
            amountMinor = 285000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_housing",
            description = "Previous Month Lease",
            merchant = "Kensington Residences",
            dateMillis = dateAt(prevY, prevM, 1, 9),
        ),
        TransactionRecord(
            id = "tx_prev_3",
            type = TransactionType.EXPENSE,
            amountMinor = 61200L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_groceries",
            description = "Whole Foods Monthly Total",
            merchant = "Whole Foods Market",
            dateMillis = dateAt(prevY, prevM, 19, 17),
        ),
        TransactionRecord(
            id = "tx_prev_4",
            type = TransactionType.EXPENSE,
            amountMinor = 39800L,
            currencyCode = "USD",
            accountId = "acc_amex",
            categoryId = "cat_dining",
            description = "Client Hospitality & Dining",
            merchant = "Balthazar",
            dateMillis = dateAt(prevY, prevM, 22, 21),
        ),
        TransactionRecord(
            id = "tx_two_1",
            type = TransactionType.INCOME,
            amountMinor = 910000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_salary",
            description = "Earlier Payroll Deposit",
            merchant = "Northstar Capital",
            dateMillis = dateAt(twoY, twoM, 15, 8),
        ),
        TransactionRecord(
            id = "tx_two_2",
            type = TransactionType.EXPENSE,
            amountMinor = 342000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_housing",
            description = "Earlier Residence & Utilities",
            merchant = "Kensington Residences",
            dateMillis = dateAt(twoY, twoM, 2, 9),
        ),
    )
}

private fun sampleBudgets(): List<BudgetRecord> {
    val cur = MonthPeriod.current()
    val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    val prevY = prevCal.get(Calendar.YEAR)
    val prevM = prevCal.get(Calendar.MONTH) + 1
    return listOf(
        BudgetRecord(
            id = "bud_cur_all",
            name = "Monthly Spending Limit",
            scope = BudgetScope.OVERALL,
            limitMinor = 520000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_cur_dining",
            name = "Dining & Coffee",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_dining",
            limitMinor = 45000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_cur_groceries",
            name = "Groceries",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_groceries",
            limitMinor = 65000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_cur_utilities",
            name = "Utilities & Bills",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_utilities",
            limitMinor = 35000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_cur_shopping",
            name = "Shopping",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_shopping",
            limitMinor = 50000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_prev_all",
            name = "Last Month Limit",
            scope = BudgetScope.OVERALL,
            limitMinor = 520000L,
            currencyCode = "USD",
            year = prevY,
            month = prevM,
        ),
    )
}

internal fun computeSpendlyUiState(
    profile: UserProfileRecord?,
    accounts: List<AccountRecord>,
    transactions: List<TransactionRecord>,
    budgets: List<BudgetRecord>,
    remoteCategories: List<CategoryRecord> = emptyList(),
    selectedPeriod: MonthPeriod = MonthPeriod.current(),
    homeFilter: HomeTransactionFilter = HomeTransactionFilter.ALL,
    lastDeletedTransaction: TransactionRecord? = null,
    activityFilterPeriod: MonthPeriod? = null,
    activityFilterCategoryId: String? = null,
    activityFilterAccountId: String? = null,
    activityFilterType: TransactionType? = null,
    activitySearchQuery: String = "",
    activitySortOption: SortOption = SortOption.NEWEST,
    reportPeriod: ReportPeriod = ReportPeriod.THIS_MONTH,
    isLoading: Boolean = false,
    syncError: String? = null,
    isSyncing: Boolean = false,
    initialDisplayName: String = "Spendly User",
): SpendlyUiState {
    val mergedCategories = (defaultCategories() + remoteCategories).distinctBy { it.id }

    val defaultCurrencyCode = profile?.defaultCurrencyCode ?: "USD"
    val accountsMap = accounts.associateBy { it.id }
    val categoriesMap = mergedCategories.associateBy { it.id }
    val activeAccounts = accounts.filter { !it.isArchived }
    val archivedAccounts = accounts.filter { it.isArchived }

    fun toBase(amountMinor: Long, currencyCode: String): Long? {
        return when (val res = MoneyFormatter.convertMinor(amountMinor, currencyCode, defaultCurrencyCode)) {
            is ConversionResult.Available -> res.convertedMinor
            is ConversionResult.Unavailable -> null
        }
    }

    // Single-pass O(T + A) balance accumulation
    val balanceDeltaByAcc = HashMap<String, Long>(accounts.size * 2)
    val txCountByAcc = HashMap<String, Int>(accounts.size * 2)
    val periodSet = HashSet<MonthPeriod>(12)

    val cal = Calendar.getInstance()
    repeat(6) {
        periodSet.add(MonthPeriod(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1))
        cal.add(Calendar.MONTH, -1)
    }
    budgets.forEach { periodSet.add(MonthPeriod(it.year, it.month)) }

    val monthlyTxs = ArrayList<TransactionRecord>()
    var monthlyExpBase = 0L
    var monthlyIncBase = 0L
    var monthlyExpTotalOverallBudget = 0L

    val overallBudget = budgets.firstOrNull {
        it.scope == BudgetScope.OVERALL &&
            it.year == selectedPeriod.year &&
            it.month == selectedPeriod.month
    }
    val categoryBudgets = budgets.filter {
        it.scope == BudgetScope.CATEGORY &&
            it.year == selectedPeriod.year &&
            it.month == selectedPeriod.month
    }

    transactions.forEach { tx ->
        val isSelfTransfer = tx.type == TransactionType.TRANSFER && tx.destinationAccountId == tx.accountId
        val srcAcc = accountsMap[tx.accountId]
        if (srcAcc != null) {
            txCountByAcc[srcAcc.id] = (txCountByAcc[srcAcc.id] ?: 0) + 1
            if (!isSelfTransfer) {
                val delta = if (srcAcc.type.isLiability) {
                    when (tx.type) {
                        TransactionType.EXPENSE -> tx.amountMinor
                        TransactionType.INCOME -> -tx.amountMinor
                        TransactionType.TRANSFER -> tx.amountMinor
                    }
                } else {
                    when (tx.type) {
                        TransactionType.EXPENSE -> -tx.amountMinor
                        TransactionType.INCOME -> tx.amountMinor
                        TransactionType.TRANSFER -> -tx.amountMinor
                    }
                }
                balanceDeltaByAcc[srcAcc.id] = (balanceDeltaByAcc[srcAcc.id] ?: 0L) + delta
            }
        }

        val destId = tx.destinationAccountId
        if (tx.type == TransactionType.TRANSFER && destId != null && !isSelfTransfer) {
            val destAcc = accountsMap[destId]
            if (destAcc != null) {
                txCountByAcc[destId] = (txCountByAcc[destId] ?: 0) + 1
                val destDelta = if (destAcc.type.isLiability) -tx.amountMinor else tx.amountMinor
                balanceDeltaByAcc[destId] = (balanceDeltaByAcc[destId] ?: 0L) + destDelta
            }
        }

        val txPeriod = DateFormatters.monthPeriodOf(tx.dateMillis)
        periodSet.add(txPeriod)
        if (txPeriod == selectedPeriod) {
            monthlyTxs.add(tx)
            val baseVal = toBase(tx.amountMinor, tx.currencyCode) ?: 0L
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    monthlyExpBase += baseVal
                    if (overallBudget != null) {
                        val inBudCurr = when (val c = MoneyFormatter.convertMinor(tx.amountMinor, tx.currencyCode, overallBudget.currencyCode)) {
                            is ConversionResult.Available -> c.convertedMinor
                            is ConversionResult.Unavailable -> 0L
                        }
                        monthlyExpTotalOverallBudget += inBudCurr
                    }
                }
                TransactionType.INCOME -> monthlyIncBase += baseVal
                TransactionType.TRANSFER -> Unit
            }
        }
    }

    val availablePeriods = periodSet.sortedWith(
        compareByDescending<MonthPeriod> { it.year }.thenByDescending { it.month }
    )

    var totalAssets = 0L
    var totalLiabilities = 0L
    val accountSummaries = accounts.map { acc ->
        val calcBalance = acc.openingBalanceMinor + (balanceDeltaByAcc[acc.id] ?: 0L)
        val convertedBase = toBase(calcBalance, acc.currencyCode)
        if (!acc.isArchived) {
            if (acc.type.isLiability) {
                totalLiabilities += (convertedBase ?: 0L)
            } else {
                totalAssets += (convertedBase ?: 0L)
            }
        }
        AccountSummary(
            account = acc,
            calculatedBalanceMinor = calcBalance,
            convertedBaseMinor = convertedBase,
            transactionCount = txCountByAcc[acc.id] ?: 0,
        )
    }

    val overallBudgetSummary = overallBudget?.let { b ->
        val remaining = b.limitMinor - monthlyExpTotalOverallBudget
        val progress = if (b.limitMinor > 0L) monthlyExpTotalOverallBudget.toFloat() / b.limitMinor.toFloat() else 0f
        BudgetSummary(
            budget = b,
            category = null,
            spentMinor = monthlyExpTotalOverallBudget,
            remainingMinor = remaining,
            progress = progress,
        )
    }

    val categoryBudgetSummaries = categoryBudgets.map { b ->
        val spentInBudgetCurrency = monthlyTxs
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE && it.categoryId == b.categoryId }
            .sumOf { tx ->
                when (val c = MoneyFormatter.convertMinor(tx.amountMinor, tx.currencyCode, b.currencyCode)) {
                    is ConversionResult.Available -> c.convertedMinor
                    is ConversionResult.Unavailable -> 0L
                }
            }
        val remaining = b.limitMinor - spentInBudgetCurrency
        val progress = if (b.limitMinor > 0L) spentInBudgetCurrency.toFloat() / b.limitMinor.toFloat() else 0f
        BudgetSummary(
            budget = b,
            category = categoriesMap[b.categoryId],
            spentMinor = spentInBudgetCurrency,
            remainingMinor = remaining,
            progress = progress,
        )
    }

    val remainingOverallBudgetBaseMinor = overallBudgetSummary?.let { summary ->
        toBase(summary.remainingMinor, summary.budget.currencyCode)
    }

    return SpendlyUiState(
        isLoading = isLoading,
        syncError = syncError,
        isSyncing = isSyncing,
        onboardingComplete = profile?.onboardingComplete ?: false,
        defaultCurrencyCode = defaultCurrencyCode,
        selectedLanguage = profile?.selectedLanguage ?: "English",
        themeMode = profile?.themeMode ?: ThemeMode.LIGHT,
        profileName = profile?.profileName ?: initialDisplayName,
        budgetAlertsEnabled = profile?.budgetAlertsEnabled ?: true,
        weeklyDigestEnabled = profile?.weeklyDigestEnabled ?: true,
        dailyReminderEnabled = profile?.dailyReminderEnabled ?: false,
        privacyHideBalances = profile?.privacyHideBalances ?: false,
        isPreviewDataActive = profile?.isPreviewDataActive ?: false,
        selectedPeriod = selectedPeriod,
        homeFilter = homeFilter,
        accounts = accounts,
        categories = mergedCategories,
        transactions = transactions,
        budgets = budgets,
        lastDeletedTransaction = lastDeletedTransaction,
        activityFilterPeriod = activityFilterPeriod,
        activityFilterCategoryId = activityFilterCategoryId,
        activityFilterAccountId = activityFilterAccountId,
        activityFilterType = activityFilterType,
        activitySearchQuery = activitySearchQuery,
        activitySortOption = activitySortOption,
        reportPeriod = reportPeriod,
        accountsMap = accountsMap,
        categoriesMap = categoriesMap,
        availablePeriods = availablePeriods,
        activeAccounts = activeAccounts,
        archivedAccounts = archivedAccounts,
        accountSummaries = accountSummaries,
        totalAssetsBaseMinor = totalAssets,
        totalCreditOutstandingBaseMinor = totalLiabilities,
        totalNetBalanceBaseMinor = totalAssets - totalLiabilities,
        monthlyTransactions = monthlyTxs,
        monthlyExpensesBaseMinor = monthlyExpBase,
        monthlyIncomeBaseMinor = monthlyIncBase,
        monthlyNetCashFlowBaseMinor = monthlyIncBase - monthlyExpBase,
        overallBudgetSummary = overallBudgetSummary,
        categoryBudgetSummaries = categoryBudgetSummaries,
        remainingOverallBudgetBaseMinor = remainingOverallBudgetBaseMinor,
    )
}

