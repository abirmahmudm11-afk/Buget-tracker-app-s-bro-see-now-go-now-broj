package com.example.viewmodel

import com.example.model.AccountRecord
import com.example.model.AccountSummary
import com.example.model.AccountType
import com.example.model.BudgetRecord
import com.example.model.BudgetScope
import com.example.model.BudgetSummary
import com.example.model.CategoryRecord
import com.example.model.CsvImportRow
import com.example.model.MonthPeriod
import com.example.model.ReportPeriod
import com.example.model.SortOption
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.theme.ThemeMode
import com.example.util.ConversionResult
import com.example.util.DateFormatters
import com.example.util.MoneyFormatter
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SpendlyUiState(
    val onboardingComplete: Boolean = false,
    val defaultCurrencyCode: String = "USD",
    val selectedLanguage: String = "English",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val profileName: String = "Alex Morgan",
    val budgetAlertsEnabled: Boolean = true,
    val weeklyDigestEnabled: Boolean = true,
    val privacyHideBalances: Boolean = false,
    val isPreviewDataActive: Boolean = true,
    val selectedPeriod: MonthPeriod = MonthPeriod(2026, 11),
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
    val availablePeriods: List<MonthPeriod> = listOf(
        MonthPeriod(2026, 11),
        MonthPeriod(2026, 10),
        MonthPeriod(2026, 9),
        MonthPeriod(2026, 8),
    )

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
                    // Credit card: purchases increase outstanding liability; repayments & income/refunds decrease it
                    when (tx.type) {
                        TransactionType.EXPENSE -> if (tx.accountId == acc.id) balance += tx.amountMinor
                        TransactionType.INCOME -> if (tx.accountId == acc.id) balance -= tx.amountMinor
                        TransactionType.TRANSFER -> {
                            if (tx.destinationAccountId == acc.id) balance -= tx.amountMinor
                            if (tx.accountId == acc.id) balance += tx.amountMinor
                        }
                    }
                } else {
                    // Asset account
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

class SpendlyViewModel : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(SpendlyUiState())
    val uiState: StateFlow<SpendlyUiState> = _uiState.asStateFlow()

    fun completeOnboarding(
        currencyCode: String,
        language: String,
        loadPreviewSample: Boolean,
    ) {
        _uiState.update { current ->
            if (loadPreviewSample) {
                current.copy(
                    onboardingComplete = true,
                    defaultCurrencyCode = currencyCode,
                    selectedLanguage = language,
                    isPreviewDataActive = true,
                    accounts = sampleAccounts(),
                    transactions = sampleTransactions(),
                    budgets = sampleBudgets(),
                )
            } else {
                current.copy(
                    onboardingComplete = true,
                    defaultCurrencyCode = currencyCode,
                    selectedLanguage = language,
                    isPreviewDataActive = false,
                    accounts = emptyList(),
                    transactions = emptyList(),
                    budgets = emptyList(),
                )
            }
        }
    }

    fun togglePreviewSampleData(enableSample: Boolean) {
        _uiState.update { current ->
            if (enableSample) {
                current.copy(
                    isPreviewDataActive = true,
                    accounts = sampleAccounts(),
                    transactions = sampleTransactions(),
                    budgets = sampleBudgets(),
                )
            } else {
                current.copy(
                    isPreviewDataActive = false,
                    accounts = emptyList(),
                    transactions = emptyList(),
                    budgets = emptyList(),
                    lastDeletedTransaction = null,
                )
            }
        }
    }

    fun setDefaultCurrency(code: String) {
        _uiState.update { it.copy(defaultCurrencyCode = code) }
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setSelectedPeriod(period: MonthPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
    }

    fun updateProfileAndPreferences(
        profileName: String? = null,
        budgetAlertsEnabled: Boolean? = null,
        weeklyDigestEnabled: Boolean? = null,
        privacyHideBalances: Boolean? = null,
    ) {
        _uiState.update {
            it.copy(
                profileName = profileName ?: it.profileName,
                budgetAlertsEnabled = budgetAlertsEnabled ?: it.budgetAlertsEnabled,
                weeklyDigestEnabled = weeklyDigestEnabled ?: it.weeklyDigestEnabled,
                privacyHideBalances = privacyHideBalances ?: it.privacyHideBalances,
            )
        }
    }

    // Activity Filters
    fun setActivitySearch(query: String) {
        _uiState.update { it.copy(activitySearchQuery = query) }
    }

    fun setActivityFilters(
        period: MonthPeriod? = _uiState.value.activityFilterPeriod,
        type: TransactionType? = _uiState.value.activityFilterType,
        accountId: String? = _uiState.value.activityFilterAccountId,
        categoryId: String? = _uiState.value.activityFilterCategoryId,
        sortOption: SortOption = _uiState.value.activitySortOption,
    ) {
        _uiState.update {
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
        _uiState.update {
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
        _uiState.update { it.copy(reportPeriod = period) }
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
        val destAcc = if (type == TransactionType.TRANSFER && destinationAccountId != null) {
            _uiState.value.accountById(destinationAccountId)
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
            description = description.trim(),
            merchant = merchant.trim(),
            dateMillis = dateMillis,
            notes = notes.trim(),
            isCreditCardRepayment = isCcRepayment,
        )

        _uiState.update { current ->
            val updated = if (existingId != null) {
                current.transactions.map { if (it.id == existingId) record else it }
            } else {
                listOf(record) + current.transactions
            }
            current.copy(transactions = updated.sortedByDescending { it.dateMillis })
        }
    }

    fun deleteTransaction(id: String) {
        _uiState.update { current ->
            val target = current.transactions.firstOrNull { it.id == id }
            current.copy(
                transactions = current.transactions.filterNot { it.id == id },
                lastDeletedTransaction = target,
            )
        }
    }

    fun undoDeleteTransaction() {
        _uiState.update { current ->
            val deleted = current.lastDeletedTransaction ?: return@update current
            current.copy(
                transactions = (current.transactions + deleted).sortedByDescending { it.dateMillis },
                lastDeletedTransaction = null,
            )
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
        val existing = existingId?.let { _uiState.value.accountById(it) }
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
        _uiState.update { current ->
            val updated = if (existingId != null) {
                current.accounts.map { if (it.id == existingId) record else it }
            } else {
                current.accounts + record
            }
            current.copy(accounts = updated)
        }
    }

    fun toggleArchiveAccount(accountId: String) {
        _uiState.update { current ->
            current.copy(
                accounts = current.accounts.map {
                    if (it.id == accountId) it.copy(isArchived = !it.isArchived) else it
                }
            )
        }
    }

    fun deleteAccount(accountId: String) {
        _uiState.update { current ->
            current.copy(
                accounts = current.accounts.filterNot { it.id == accountId },
                transactions = current.transactions.filterNot {
                    it.accountId == accountId || it.destinationAccountId == accountId
                },
            )
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
        _uiState.update { current ->
            val filtered = current.budgets.filterNot { existing ->
                if (existingId != null) existing.id == existingId
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
            }
            current.copy(budgets = filtered + record)
        }
    }

    fun deleteBudget(budgetId: String) {
        _uiState.update { current ->
            current.copy(budgets = current.budgets.filterNot { it.id == budgetId })
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
        _uiState.update { it.copy(categories = it.categories + record) }
    }

    // CSV Import Preview Commit
    fun commitCsvImport(
        rows: List<CsvImportRow>,
        destinationAccountId: String,
        currencyCode: String,
    ): Int {
        val validRows = rows.filter { it.errorMessage == null && !it.isDuplicate && it.parsedAmountMinor != null && it.parsedDateMillis != null }
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
        _uiState.update { current ->
            current.copy(transactions = (newTx + current.transactions).sortedByDescending { it.dateMillis })
        }
        return newTx.size
    }

    fun isDuplicateCandidate(dateMillis: Long, amountMinor: Long, description: String, accountId: String): Boolean {
        val targetDay = DateFormatters.toDayKey(dateMillis)
        return _uiState.value.transactions.any { tx ->
            tx.accountId == accountId &&
                tx.amountMinor == amountMinor &&
                DateFormatters.toDayKey(tx.dateMillis) == targetDay &&
                tx.description.equals(description.trim(), ignoreCase = true)
        }
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

private fun sampleTransactions(): List<TransactionRecord> = listOf(
    TransactionRecord(
        id = "tx_1",
        type = TransactionType.EXPENSE,
        amountMinor = 8425L,
        currencyCode = "USD",
        accountId = "acc_sapphire",
        categoryId = "cat_groceries",
        description = "Trader Joe's Weekly Market",
        merchant = "Trader Joe's",
        dateMillis = dateAt(2026, 11, 23, 9),
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
        dateMillis = dateAt(2026, 11, 22, 10),
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
        dateMillis = dateAt(2026, 11, 21, 16),
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
        dateMillis = dateAt(2026, 11, 20, 14),
    ),
    TransactionRecord(
        id = "tx_5",
        type = TransactionType.EXPENSE,
        amountMinor = 135000L,
        currencyCode = "USD",
        accountId = "acc_checking",
        categoryId = "cat_housing",
        description = "November Apartment Rent",
        merchant = "Oakwood Properties",
        dateMillis = dateAt(2026, 11, 1, 9),
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
        dateMillis = dateAt(2026, 11, 15, 8),
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
        dateMillis = dateAt(2026, 11, 12, 11),
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
        dateMillis = dateAt(2026, 11, 9, 20),
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
        dateMillis = dateAt(2026, 11, 6, 15),
    ),
    // October historical records for Reports & Month comparison
    TransactionRecord(
        id = "tx_oct_1",
        type = TransactionType.INCOME,
        amountMinor = 485000L,
        currencyCode = "USD",
        accountId = "acc_checking",
        categoryId = "cat_salary",
        description = "October Payroll",
        merchant = "Northstar Studio",
        dateMillis = dateAt(2026, 10, 15, 8),
    ),
    TransactionRecord(
        id = "tx_oct_2",
        type = TransactionType.EXPENSE,
        amountMinor = 135000L,
        currencyCode = "USD",
        accountId = "acc_checking",
        categoryId = "cat_housing",
        description = "October Apartment Rent",
        merchant = "Oakwood Properties",
        dateMillis = dateAt(2026, 10, 1, 9),
    ),
    TransactionRecord(
        id = "tx_oct_3",
        type = TransactionType.EXPENSE,
        amountMinor = 34200L,
        currencyCode = "USD",
        accountId = "acc_sapphire",
        categoryId = "cat_groceries",
        description = "Monthly Groceries Total",
        merchant = "Whole Foods",
        dateMillis = dateAt(2026, 10, 19, 17),
    ),
    TransactionRecord(
        id = "tx_sep_1",
        type = TransactionType.INCOME,
        amountMinor = 460000L,
        currencyCode = "USD",
        accountId = "acc_checking",
        categoryId = "cat_salary",
        description = "September Payroll",
        merchant = "Northstar Studio",
        dateMillis = dateAt(2026, 9, 15, 8),
    ),
    TransactionRecord(
        id = "tx_sep_2",
        type = TransactionType.EXPENSE,
        amountMinor = 189000L,
        currencyCode = "USD",
        accountId = "acc_checking",
        categoryId = "cat_housing",
        description = "September Rent & Utilities",
        merchant = "Oakwood Properties",
        dateMillis = dateAt(2026, 9, 2, 9),
    ),
)

private fun sampleBudgets(): List<BudgetRecord> = listOf(
    BudgetRecord(
        id = "bud_nov_all",
        name = "November Monthly Limit",
        scope = BudgetScope.OVERALL,
        limitMinor = 240000L,
        currencyCode = "USD",
        year = 2026,
        month = 11,
    ),
    BudgetRecord(
        id = "bud_nov_dining",
        name = "Dining & Coffee",
        scope = BudgetScope.CATEGORY,
        categoryId = "cat_dining",
        limitMinor = 16000L,
        currencyCode = "USD",
        year = 2026,
        month = 11,
    ),
    BudgetRecord(
        id = "bud_nov_groceries",
        name = "Groceries",
        scope = BudgetScope.CATEGORY,
        categoryId = "cat_groceries",
        limitMinor = 40000L,
        currencyCode = "USD",
        year = 2026,
        month = 11,
    ),
    BudgetRecord(
        id = "bud_nov_utilities",
        name = "Utilities & Bills",
        scope = BudgetScope.CATEGORY,
        categoryId = "cat_utilities",
        limitMinor = 25000L,
        currencyCode = "USD",
        year = 2026,
        month = 11,
    ),
    BudgetRecord(
        id = "bud_oct_all",
        name = "October Monthly Limit",
        scope = BudgetScope.OVERALL,
        limitMinor = 240000L,
        currencyCode = "USD",
        year = 2026,
        month = 10,
    ),
)
