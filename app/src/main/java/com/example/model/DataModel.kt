package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToLong

object FirestoreCollections {
    const val USERS = "users"
    const val TRANSACTIONS = "transactions"
    const val BUDGET = "budgets"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
}

/**
 * Firestore document schema for the `transactions` collection.
 * Maps high-level expense fields (`amount`, `date`, `category`, `note`) while preserving
 * integer minor-unit precision (`amountMinor`) and `firestore.rules` parity.
 */
@Keep
@IgnoreExtraProperties
data class TransactionData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val amountMinor: Long = 0L,
    val date: Long = 0L,
    val category: String = "",
    val note: String = "",
    val txType: String = TransactionType.EXPENSE.name,
    val currencyCode: String = "USD",
    val accountId: String = "",
    val destinationAccountId: String? = null,
    val description: String = "",
    val merchant: String = "",
    val creditCardRepayment: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun resolvedAmountMinor(): Long =
        if (amountMinor > 0L) amountMinor else (amount * 100.0).roundToLong().coerceAtLeast(1L)

    fun toTransactionRecord(): TransactionRecord {
        val parsedType = enumValues<TransactionType>().firstOrNull { it.name == txType } ?: TransactionType.EXPENSE
        return TransactionRecord(
            id = id,
            type = parsedType,
            amountMinor = resolvedAmountMinor(),
            currencyCode = currencyCode.ifBlank { "USD" },
            accountId = accountId,
            destinationAccountId = destinationAccountId,
            categoryId = category.takeIf { it.isNotBlank() },
            description = description.ifBlank { note.ifBlank { parsedType.label } },
            merchant = merchant,
            dateMillis = date.coerceAtLeast(1L),
            notes = note,
            isCreditCardRepayment = creditCardRepayment,
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "txType" to txType.ifBlank { TransactionType.EXPENSE.name },
        "amountMinor" to resolvedAmountMinor(),
        "currencyCode" to currencyCode.trim().take(5).let { if (it.length in 3..5) it else "USD" },
        "accountId" to accountId.ifBlank { "acc_cash_default" },
        "destinationAccountId" to destinationAccountId?.takeIf { it.isNotBlank() },
        "categoryId" to category.takeIf { it.isNotBlank() },
        "description" to description.ifBlank { note }.trim().take(160),
        "merchant" to merchant.trim().take(100),
        "dateMillis" to date.coerceAtLeast(1L),
        "notes" to note.trim().take(500),
        "creditCardRepayment" to creditCardRepayment,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "txType" to txType.ifBlank { TransactionType.EXPENSE.name },
        "amountMinor" to resolvedAmountMinor(),
        "currencyCode" to currencyCode.trim().take(5).let { if (it.length in 3..5) it else "USD" },
        "accountId" to accountId.ifBlank { "acc_cash_default" },
        "destinationAccountId" to (destinationAccountId?.takeIf { it.isNotBlank() } ?: FieldValue.delete()),
        "categoryId" to (category.takeIf { it.isNotBlank() } ?: FieldValue.delete()),
        "description" to description.ifBlank { note }.trim().take(160),
        "merchant" to merchant.trim().take(100),
        "dateMillis" to date.coerceAtLeast(1L),
        "notes" to note.trim().take(500),
        "creditCardRepayment" to creditCardRepayment,
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    companion object {
        fun fromTransactionRecord(record: TransactionRecord, uid: String = ""): TransactionData {
            return TransactionData(
                id = record.id,
                userId = uid,
                amount = record.amountMinor / 100.0,
                amountMinor = record.amountMinor,
                date = record.dateMillis,
                category = record.categoryId ?: "",
                note = record.notes.ifBlank { record.description },
                txType = record.type.name,
                currencyCode = record.currencyCode,
                accountId = record.accountId,
                destinationAccountId = record.destinationAccountId,
                description = record.description,
                merchant = record.merchant,
                creditCardRepayment = record.isCreditCardRepayment,
            )
        }
    }
}

/**
 * Firestore document schema for the `budget` (`budgets`) collection.
 */
@Keep
@IgnoreExtraProperties
data class BudgetData(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val scope: String = BudgetScope.OVERALL.name,
    val category: String? = null,
    val limitMinor: Long = 0L,
    val currencyCode: String = "USD",
    val year: Int = 2026,
    val month: Int = 11,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toBudgetRecord(): BudgetRecord {
        val parsedScope = enumValues<BudgetScope>().firstOrNull { it.name == scope } ?: BudgetScope.OVERALL
        return BudgetRecord(
            id = id,
            name = name,
            scope = parsedScope,
            categoryId = category?.takeIf { it.isNotBlank() },
            limitMinor = limitMinor,
            currencyCode = currencyCode.ifBlank { "USD" },
            year = year,
            month = month,
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(80).ifBlank { "Monthly Budget" },
        "scope" to scope.ifBlank { BudgetScope.OVERALL.name },
        "categoryId" to category?.takeIf { it.isNotBlank() },
        "limitMinor" to limitMinor.coerceAtLeast(1L),
        "currencyCode" to currencyCode.trim().take(5).ifBlank { "USD" },
        "year" to year.coerceIn(2000, 2100),
        "month" to month.coerceIn(1, 12),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(80).ifBlank { "Monthly Budget" },
        "scope" to scope.ifBlank { BudgetScope.OVERALL.name },
        "categoryId" to category?.takeIf { it.isNotBlank() },
        "limitMinor" to limitMinor.coerceAtLeast(1L),
        "currencyCode" to currencyCode.trim().take(5).ifBlank { "USD" },
        "year" to year.coerceIn(2000, 2100),
        "month" to month.coerceIn(1, 12),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>
}
