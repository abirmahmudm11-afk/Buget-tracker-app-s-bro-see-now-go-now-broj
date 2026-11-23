package src.database.schema

/**
 * Normalized Spendly Financial Data Schema (Integer Minor-Unit Precision).
 * Monetary fields store integer minor units (e.g., USD cents) rather than floating point.
 */

data class AccountEntity(
    val id: String,
    val name: String,
    val type: String, // BANK_ACCOUNT, DEBIT_CARD, CREDIT_CARD, PREPAID_CARD, CASH_WALLET, DIGITAL_WALLET, SAVINGS_ACCOUNT, CUSTOM
    val institutionName: String,
    val cardNetwork: String? = null,
    val lastFour: String? = null, // Optional 4-digit identifier only; full PAN/CVV/PIN never stored
    val currencyCode: String = "USD",
    val openingBalanceMinor: Long = 0L,
    val creditLimitMinor: Long? = null,
    val description: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CategoryEntity(
    val id: String,
    val name: String,
    val type: String, // EXPENSE, INCOME
    val icon: String,
    val colorHex: String,
    val isDefault: Boolean = true,
    val isActive: Boolean = true,
)

data class TransactionEntity(
    val id: String,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val amountMinor: Long,
    val currencyCode: String,
    val accountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val description: String,
    val merchant: String? = null,
    val transactionDate: Long,
    val notes: String? = null,
    val status: String = "POSTED", // POSTED, REVERSED, ADJUSTMENT
    val linkedTransactionId: String? = null, // Links refunds or credit-card repayment legs
    val createdAt: Long,
    val updatedAt: Long,
)

data class BudgetEntity(
    val id: String,
    val name: String,
    val scope: String, // OVERALL, CATEGORY
    val categoryId: String? = null,
    val amountMinor: Long,
    val currencyCode: String,
    val period: String, // YYYY-MM (e.g., "2026-11")
    val createdAt: Long,
    val updatedAt: Long,
)

data class SettingsEntity(
    val id: String = "app_settings",
    val defaultCurrency: String = "USD",
    val language: String = "English",
    val theme: String = "LIGHT", // LIGHT default
    val onboardingComplete: Boolean = false,
    val preferencesJson: String = "{}",
)

data class ExchangeRateEntity(
    val baseCurrency: String,
    val quoteCurrency: String,
    val rate: Double,
    val source: String,
    val rateDate: String,
    val createdAt: Long,
)
