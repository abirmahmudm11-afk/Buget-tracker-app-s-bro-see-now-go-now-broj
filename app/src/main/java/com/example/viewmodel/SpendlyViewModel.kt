package com.example.viewmodel

import android.util.Log
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
) {
    val availablePeriods: List<MonthPeriod>
        get() {
            val base = buildList {
                val cal = Calendar.getInstance()
                repeat(6) {
                    add(MonthPeriod(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1))
                    cal.add(Calendar.MONTH, -1)
                }
                transactions.forEach { add(DateFormatters.monthPeriodOf(it.dateMillis)) }
                budgets.forEach { add(MonthPeriod(it.year, it.month)) }
            }
            return base.distinct()
                .sortedWith(compareByDescending<MonthPeriod> { it.year }.thenByDescending { it.month })
        }

    val activeAccounts: List<AccountRecord>
        get() = accounts.filter { !it.isArchived }

    val archivedAccounts: List<AccountRecord>
        get() = accounts.filter { it.isArchived }

    fun accountById(id: String): AccountRecord? = accounts.firstOrNull { it.id == id }
    fun categoryById(id: String?): CategoryRecord? = categories.firstOrNull { it.id == id }

    fun convertToBase(amountMinor: Long, currencyCode: String): Long? {
        return when (val res = MoneyFormatter.convertMinor(amountMinor, currencyCode, defaultCurrencyCode)) {
            is ConversionResult.Available -> res.convertedMinor
            is ConversionResult.Unavailable -> null
        }
    }

    val accountSummaries: List<AccountSummary>
        get() = accounts.map { acc ->
            val relevantTx = transactions.filter {
                it.accountId == acc.id || it.destinationAccountId == acc.id
            }
            var balance = acc.openingBalanceMinor
            relevantTx.forEach { tx ->
                if (acc.type.isLiability) {
                    when (tx.type) {
                        TransactionType.EXPENSE -> if (tx.accountId == acc.id) balance += tx.amountMinor
                        TransactionType.INCOME -> if (tx.accountId == acc.id) balance -= tx.amountMinor
                        TransactionType.TRANSFER -> {
                            if (tx.destinationAccountId == acc.id) balance -= tx.amountMinor
                            if (tx.accountId == acc.id) balance += tx.amountMinor
                        }
                    }
                } else {
                    when (tx.type) {
                        TransactionType.EXPENSE -> if (tx.accountId == acc.id) balance -= tx.amountMinor
                        TransactionType.INCOME -> if (tx.accountId == acc.id) balance += tx.amountMinor
                        TransactionType.TRANSFER -> {
                            if (tx.accountId == acc.id) balance -= tx.amountMinor
                            if (tx.destinationAccountId == acc.id) balance += tx.amountMinor
                        }
                    }
                }
            }
            AccountSummary(
                account = acc,
                calculatedBalanceMinor = balance,
                convertedBaseMinor = convertToBase(balance, acc.currencyCode),
                transactionCount = relevantTx.size,
            )
        }

    val totalAssetsBaseMinor: Long
        get() = accountSummaries
            .filter { !it.account.isArchived && !it.account.type.isLiability }
            .sumOf { it.convertedBaseMinor ?: 0L }

    val totalCreditOutstandingBaseMinor: Long
        get() = accountSummaries
            .filter { !it.account.isArchived && it.account.type.isLiability }
            .sumOf { it.convertedBaseMinor ?: 0L }

    val totalNetBalanceBaseMinor: Long
        get() = totalAssetsBaseMinor - totalCreditOutstandingBaseMinor

    val monthlyTransactions: List<TransactionRecord>
        get() = transactions.filter { DateFormatters.monthPeriodOf(it.dateMillis) == selectedPeriod }

    val monthlyExpensesBaseMinor: Long
        get() = monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { convertToBase(it.amountMinor, it.currencyCode) ?: 0L }

    val monthlyIncomeBaseMinor: Long
        get() = monthlyTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { convertToBase(it.amountMinor, it.currencyCode) ?: 0L }

    val monthlyNetCashFlowBaseMinor: Long
        get() = monthlyIncomeBaseMinor - monthlyExpensesBaseMinor

    val overallBudgetSummary: BudgetSummary?
        get() {
            val b = budgets.firstOrNull {
                it.scope == BudgetScope.OVERALL &&
                    it.year == selectedPeriod.year &&
                    it.month == selectedPeriod.month
            } ?: return null
            val spentInBudgetCurrency = monthlyTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { tx ->
                    when (val c = MoneyFormatter.convertMinor(tx.amountMinor, tx.currencyCode, b.currencyCode)) {
                        is ConversionResult.Available -> c.convertedMinor
                        is ConversionResult.Unavailable -> 0L
                    }
                }
            val remaining = b.limitMinor - spentInBudgetCurrency
            val progress = if (b.limitMinor > 0L) spentInBudgetCurrency.toFloat() / b.limitMinor.toFloat() else 0f
            return BudgetSummary(
                budget = b,
                category = null,
                spentMinor = spentInBudgetCurrency,
                remainingMinor = remaining,
                progress = progress,
            )
        }

    val categoryBudgetSummaries: List<BudgetSummary>
        get() = budgets
            .filter {
                it.scope == BudgetScope.CATEGORY &&
                    it.year == selectedPeriod.year &&
                    it.month == selectedPeriod.month
            }
            .map { b ->
                val spentInBudgetCurrency = monthlyTransactions
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
                    category = categoryById(b.categoryId),
                    spentMinor = spentInBudgetCurrency,
                    remainingMinor = remaining,
                    progress = progress,
                )
            }

    val remainingOverallBudgetBaseMinor: Long?
        get() {
            val summary = overallBudgetSummary ?: return null
            return convertToBase(summary.remainingMinor, summary.budget.currencyCode)
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
        return kotlinx.coroutines.flow.flow {
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
        val mergedCategories = (defaultCategories() + remoteCategories).distinctBy { it.id }

        SpendlyUiState(
            isLoading = anyLoading && profile == null,
            syncError = local.syncError ?: streamError,
            isSyncing = local.isSyncing,
            onboardingComplete = profile?.onboardingComplete ?: false,
            defaultCurrencyCode = profile?.defaultCurrencyCode ?: "USD",
            selectedLanguage = profile?.selectedLanguage ?: "English",
            themeMode = profile?.themeMode ?: ThemeMode.LIGHT,
            profileName = profile?.profileName ?: initialDisplayName,
            budgetAlertsEnabled = profile?.budgetAlertsEnabled ?: true,
            weeklyDigestEnabled = profile?.weeklyDigestEnabled ?: true,
            dailyReminderEnabled = profile?.dailyReminderEnabled ?: false,
            privacyHideBalances = profile?.privacyHideBalances ?: false,
            isPreviewDataActive = profile?.isPreviewDataActive ?: false,
            selectedPeriod = local.selectedPeriod,
            homeFilter = local.homeFilter,
            accounts = accounts,
            categories = mergedCategories,
            transactions = transactions,
            budgets = budgets,
            lastDeletedTransaction = local.lastDeletedTransaction,
            activityFilterPeriod = local.activityFilterPeriod,
            activityFilterCategoryId = local.activityFilterCategoryId,
            activityFilterAccountId = local.activityFilterAccountId,
            activityFilterType = local.activityFilterType,
            activitySearchQuery = local.activitySearchQuery,
            activitySortOption = local.activitySortOption,
            reportPeriod = local.reportPeriod,
        )
    }.stateIn(
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
                description = description.trim().ifBlank { merchant.trim().ifBlank { type.label } },
                merchant = merchant.trim(),
                dateMillis = dateMillis,
                notes = notes.trim(),
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
                val defaultWallet = AccountRecord(
                    id = "acc_cash_default",
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
            val tx = TransactionRecord(
                id = "tx_${UUID.randomUUID().toString().take(8)}",
                type = TransactionType.EXPENSE,
                amountMinor = amountMinor,
                currencyCode = resolvedCurrency,
                accountId = resolvedAccountId,
                categoryId = categoryId,
                description = note.trim().ifBlank { catName },
                merchant = note.trim(),
                dateMillis = dateMillis,
                notes = note.trim(),
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
    ) {
        val existing = existingId?.let { uiState.value.accountById(it) }
        val record = AccountRecord(
            id = existingId ?: "acc_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            type = type,
            institutionName = institutionName.trim(),
            cardNetwork = if (type.supportsCardDetails) cardNetwork.trim() else "",
            lastFour = if (type.supportsCardDetails) lastFour.trim().take(4) else "",
            currencyCode = currencyCode,
            openingBalanceMinor = openingBalanceMinor,
            creditLimitMinor = if (type == AccountType.CREDIT_CARD) creditLimitMinor else null,
            description = description.trim(),
            isArchived = existing?.isArchived ?: false,
        )
        viewModelScope.launch {
            repository.saveAccount(record, isUpdate = existingId != null)
                .onFailure { err -> localFilters.update { it.copy(syncError = err.localizedMessage) } }
        }
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
            name = name.trim(),
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
    return Calendar.getInstance().apply {
        set(year, month - 1, day, hour, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun sampleAccounts(): List<AccountRecord> = listOf(
    AccountRecord(
        id = "acc_checking",
        name = "Everyday Checking",
        type = AccountType.BANK_ACCOUNT,
        institutionName = "Summit National",
        currencyCode = "USD",
        openingBalanceMinor = 342000L,
        description = "Primary household checking ledger",
    ),
    AccountRecord(
        id = "acc_sapphire",
        name = "Emerald Rewards",
        type = AccountType.CREDIT_CARD,
        institutionName = "Summit National",
        cardNetwork = "Visa",
        lastFour = "4821",
        currencyCode = "USD",
        openingBalanceMinor = 28500L,
        creditLimitMinor = 500000L,
        description = "Monthly rewards card — paid in full",
    ),
    AccountRecord(
        id = "acc_cash",
        name = "Cash Pocket",
        type = AccountType.CASH_WALLET,
        institutionName = "Physical Wallet",
        currencyCode = "USD",
        openingBalanceMinor = 18500L,
        description = "Day-to-day cash spending",
    ),
    AccountRecord(
        id = "acc_euro_travel",
        name = "Euro Travel Card",
        type = AccountType.PREPAID_CARD,
        institutionName = "Wise",
        cardNetwork = "Mastercard",
        lastFour = "9014",
        currencyCode = "EUR",
        openingBalanceMinor = 62000L,
        description = "Multi-currency travel wallet",
    ),
    AccountRecord(
        id = "acc_savings",
        name = "Emergency Reserve",
        type = AccountType.SAVINGS_ACCOUNT,
        institutionName = "Ally Reserve",
        currencyCode = "USD",
        openingBalanceMinor = 1250000L,
        description = "6-month rainy day buffer",
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

    return listOf(
        TransactionRecord(
            id = "tx_1",
            type = TransactionType.EXPENSE,
            amountMinor = 8425L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_groceries",
            description = "Trader Joe's Weekly Market",
            merchant = "Trader Joe's",
            dateMillis = dateAt(cur.year, cur.month, 23, 9),
            notes = "Produce, pantry staples, coffee beans",
        ),
        TransactionRecord(
            id = "tx_2",
            type = TransactionType.EXPENSE,
            amountMinor = 1480L,
            currencyCode = "USD",
            accountId = "acc_cash",
            categoryId = "cat_dining",
            description = "Oat Cortado & Croissant",
            merchant = "Sightglass Coffee",
            dateMillis = dateAt(cur.year, cur.month, 22, 10),
        ),
        TransactionRecord(
            id = "tx_3",
            type = TransactionType.TRANSFER,
            amountMinor = 45000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            destinationAccountId = "acc_sapphire",
            description = "Emerald Rewards Statement Payment",
            merchant = "Internal Repayment",
            dateMillis = dateAt(cur.year, cur.month, 21, 16),
            notes = "Repayment reduces card liability without duplicating spending",
            isCreditCardRepayment = true,
        ),
        TransactionRecord(
            id = "tx_4",
            type = TransactionType.EXPENSE,
            amountMinor = 4250L,
            currencyCode = "EUR",
            accountId = "acc_euro_travel",
            categoryId = "cat_transport",
            description = "Regional Rail Pass",
            merchant = "EuroRail",
            dateMillis = dateAt(cur.year, cur.month, 20, 14),
        ),
        TransactionRecord(
            id = "tx_5",
            type = TransactionType.EXPENSE,
            amountMinor = 135000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_housing",
            description = "Monthly Apartment Rent",
            merchant = "Oakwood Properties",
            dateMillis = dateAt(cur.year, cur.month, 1, 9),
        ),
        TransactionRecord(
            id = "tx_6",
            type = TransactionType.INCOME,
            amountMinor = 485000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_salary",
            description = "Bi-weekly Payroll Deposit",
            merchant = "Northstar Studio",
            dateMillis = dateAt(cur.year, cur.month, 15, 8),
        ),
        TransactionRecord(
            id = "tx_7",
            type = TransactionType.EXPENSE,
            amountMinor = 21900L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_utilities",
            description = "Fiber Internet & Electric",
            merchant = "City Grid Utility",
            dateMillis = dateAt(cur.year, cur.month, 12, 11),
        ),
        TransactionRecord(
            id = "tx_8",
            type = TransactionType.EXPENSE,
            amountMinor = 16400L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_dining",
            description = "Neighborhood Bistro Dinner",
            merchant = "L'Atelier Bistro",
            dateMillis = dateAt(cur.year, cur.month, 9, 20),
        ),
        TransactionRecord(
            id = "tx_9",
            type = TransactionType.INCOME,
            amountMinor = 65000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_freelance",
            description = "UI Audit Consulting",
            merchant = "Kiteworks",
            dateMillis = dateAt(cur.year, cur.month, 6, 15),
        ),
        TransactionRecord(
            id = "tx_prev_1",
            type = TransactionType.INCOME,
            amountMinor = 485000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_salary",
            description = "Previous Month Payroll",
            merchant = "Northstar Studio",
            dateMillis = dateAt(prevY, prevM, 15, 8),
        ),
        TransactionRecord(
            id = "tx_prev_2",
            type = TransactionType.EXPENSE,
            amountMinor = 135000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_housing",
            description = "Previous Month Rent",
            merchant = "Oakwood Properties",
            dateMillis = dateAt(prevY, prevM, 1, 9),
        ),
        TransactionRecord(
            id = "tx_prev_3",
            type = TransactionType.EXPENSE,
            amountMinor = 34200L,
            currencyCode = "USD",
            accountId = "acc_sapphire",
            categoryId = "cat_groceries",
            description = "Monthly Groceries Total",
            merchant = "Whole Foods",
            dateMillis = dateAt(prevY, prevM, 19, 17),
        ),
        TransactionRecord(
            id = "tx_two_1",
            type = TransactionType.INCOME,
            amountMinor = 460000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_salary",
            description = "Earlier Payroll",
            merchant = "Northstar Studio",
            dateMillis = dateAt(twoY, twoM, 15, 8),
        ),
        TransactionRecord(
            id = "tx_two_2",
            type = TransactionType.EXPENSE,
            amountMinor = 189000L,
            currencyCode = "USD",
            accountId = "acc_checking",
            categoryId = "cat_housing",
            description = "Earlier Rent & Utilities",
            merchant = "Oakwood Properties",
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
            limitMinor = 240000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_cur_dining",
            name = "Dining & Coffee",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_dining",
            limitMinor = 16000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_cur_groceries",
            name = "Groceries",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_groceries",
            limitMinor = 40000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_cur_utilities",
            name = "Utilities & Bills",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_utilities",
            limitMinor = 25000L,
            currencyCode = "USD",
            year = cur.year,
            month = cur.month,
        ),
        BudgetRecord(
            id = "bud_prev_all",
            name = "Last Month Limit",
            scope = BudgetScope.OVERALL,
            limitMinor = 240000L,
            currencyCode = "USD",
            year = prevY,
            month = prevM,
        ),
    )
}
