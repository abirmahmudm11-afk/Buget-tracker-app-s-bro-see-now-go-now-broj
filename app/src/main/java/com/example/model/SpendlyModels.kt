package com.example.model

enum class AccountType(
    val displayName: String,
    val isLiability: Boolean = false,
    val supportsCardDetails: Boolean = false,
) {
    BANK_ACCOUNT("Bank Account", isLiability = false, supportsCardDetails = false),
    DEBIT_CARD("Debit Card", isLiability = false, supportsCardDetails = true),
    CREDIT_CARD("Credit Card", isLiability = true, supportsCardDetails = true),
    PREPAID_CARD("Prepaid Card", isLiability = false, supportsCardDetails = true),
    CASH_WALLET("Cash Wallet", isLiability = false, supportsCardDetails = false),
    DIGITAL_WALLET("Digital Wallet", isLiability = false, supportsCardDetails = false),
    SAVINGS_ACCOUNT("Savings Account", isLiability = false, supportsCardDetails = false),
    CUSTOM("Custom Account", isLiability = false, supportsCardDetails = false),
}

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer"),
}

enum class BudgetScope(val label: String) {
    OVERALL("Overall Monthly"),
    CATEGORY("Category Budget"),
}

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST("Highest Amount"),
    LOWEST("Lowest Amount"),
}

enum class ReportPeriod(val label: String, val monthsCount: Int) {
    THIS_MONTH("This Month", 1),
    LAST_MONTH("Last Month", 1),
    LAST_3_MONTHS("Last 3 Months", 3),
}

data class CurrencySpec(
    val code: String,
    val symbol: String,
    val displayName: String,
    val minorDecimals: Int = 2,
    val rateToUsd: Double? = null,
    val rateSource: String? = null,
    val rateDate: String? = null,
)

data class AccountRecord(
    val id: String,
    val name: String,
    val type: AccountType,
    val institutionName: String,
    val cardNetwork: String = "",
    val lastFour: String = "",
    val currencyCode: String = "USD",
    val openingBalanceMinor: Long = 0L,
    val creditLimitMinor: Long? = null,
    val description: String = "",
    val isArchived: Boolean = false,
)

data class CategoryRecord(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconKey: String,
    val isDefault: Boolean = true,
    val isActive: Boolean = true,
)

data class TransactionRecord(
    val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val currencyCode: String,
    val accountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val description: String,
    val merchant: String = "",
    val dateMillis: Long,
    val notes: String = "",
    val isCreditCardRepayment: Boolean = false,
)

data class BudgetRecord(
    val id: String,
    val name: String,
    val scope: BudgetScope,
    val categoryId: String? = null,
    val limitMinor: Long,
    val currencyCode: String,
    val year: Int,
    val month: Int, // 1..12
)

data class MonthPeriod(
    val year: Int,
    val month: Int, // 1..12
)

data class AccountSummary(
    val account: AccountRecord,
    val calculatedBalanceMinor: Long,
    val convertedBaseMinor: Long?,
    val transactionCount: Int,
)

data class BudgetSummary(
    val budget: BudgetRecord,
    val category: CategoryRecord?,
    val spentMinor: Long,
    val remainingMinor: Long,
    val progress: Float,
) {
    val isOverspent: Boolean get() = spentMinor > budget.limitMinor
    val isNearLimit: Boolean get() = !isOverspent && progress >= 0.85f
}

data class CsvImportRow(
    val rowNumber: Int,
    val dateText: String,
    val parsedDateMillis: Long?,
    val description: String,
    val amountText: String,
    val parsedAmountMinor: Long?,
    val type: TransactionType,
    val categoryId: String,
    val isDuplicate: Boolean,
    val errorMessage: String? = null,
)
