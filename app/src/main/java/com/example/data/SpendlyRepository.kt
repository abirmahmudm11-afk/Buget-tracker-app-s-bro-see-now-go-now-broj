package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.BudgetRecord
import com.example.model.BudgetScope
import com.example.model.CategoryRecord
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UserProfileRecord
import com.example.ui.theme.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SpendlyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeUserProfile(userId: String): Flow<UserProfileRecord?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null else snapshot.toUserProfile()
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw (firestoreError ?: error)
                }
        )
    }

    fun observeAccounts(userId: String): Flow<List<AccountRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/accounts"
        emitAll(
            userDoc(userId).collection("accounts")
                .snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toAccountRecord() } }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/transactions"
        emitAll(
            userDoc(userId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toTransactionRecord() }
                        .sortedByDescending { it.dateMillis }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeBudgets(userId: String): Flow<List<BudgetRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/budgets"
        emitAll(
            userDoc(userId).collection("budgets")
                .snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toBudgetRecord() } }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                }
        )
    }

    fun observeCategories(userId: String): Flow<List<CategoryRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "users/$userId/categories"
        emitAll(
            userDoc(userId).collection("categories")
                .snapshots()
                .map { snapshot -> snapshot.documents.mapNotNull { it.toCategoryRecord() } }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                }
        )
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String,
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw (firestoreError ?: error)
    }

    suspend fun ensureUserProfile(defaultProfile: UserProfileRecord): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = userDoc(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = defaultProfile.profileName.trim().take(80).ifBlank { "Spendly User" }
                val payload = mapOf(
                    "userId" to uid,
                    "profileName" to safeName,
                    "defaultCurrencyCode" to defaultProfile.defaultCurrencyCode.trim().take(5).ifBlank { "USD" },
                    "selectedLanguage" to defaultProfile.selectedLanguage.trim().take(40).ifBlank { "English" },
                    "themeMode" to defaultProfile.themeMode.name,
                    "onboardingComplete" to defaultProfile.onboardingComplete,
                    "budgetAlertsEnabled" to defaultProfile.budgetAlertsEnabled,
                    "weeklyDigestEnabled" to defaultProfile.weeklyDigestEnabled,
                    "dailyReminderEnabled" to defaultProfile.dailyReminderEnabled,
                    "privacyHideBalances" to defaultProfile.privacyHideBalances,
                    "previewDataActive" to defaultProfile.isPreviewDataActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileFields(updates: Map<String, Any>): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val docRef = userDoc(uid)
            val mutable = updates.toMutableMap()
            mutable["updatedAt"] = FieldValue.serverTimestamp()
            docRef.update(mutable).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAccount(account: AccountRecord, isUpdate: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/accounts/${account.id}"
        return try {
            val uid = requireUserId()
            require(account.id.isNotBlank()) { "Account ID cannot be blank" }
            require(account.name.isNotBlank()) { "Account name cannot be blank" }
            path = "users/$uid/accounts/${account.id}"
            val docRef = userDoc(uid).collection("accounts").document(account.id)
            if (isUpdate) {
                val payload = mapOf<String, Any?>(
                    "name" to account.name.trim().take(80),
                    "accountType" to account.type.name,
                    "institutionName" to account.institutionName.trim().take(80),
                    "cardNetwork" to account.cardNetwork.trim().take(40),
                    "lastFour" to account.lastFour.trim().take(4),
                    "currencyCode" to account.currencyCode.trim().take(5).ifBlank { "USD" },
                    "openingBalanceMinor" to account.openingBalanceMinor,
                    "creditLimitMinor" to account.creditLimitMinor?.coerceAtLeast(0L),
                    "description" to account.description.trim().take(240),
                    "archived" to account.isArchived,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null } as Map<String, Any>
                docRef.update(payload).await()
            } else {
                val payload = account.toCreateMap(uid)
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleArchiveAccount(accountId: String, archived: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/accounts/$accountId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/accounts/$accountId"
            userDoc(uid).collection("accounts").document(accountId)
                .update(mapOf("archived" to archived, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountCascade(accountId: String, linkedTransactionIds: List<String>): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/accounts/$accountId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/accounts/$accountId"
            val batch = db.batch()
            batch.delete(userDoc(uid).collection("accounts").document(accountId))
            linkedTransactionIds.take(400).forEach { txId ->
                batch.delete(userDoc(uid).collection("transactions").document(txId))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTransaction(tx: TransactionRecord, isUpdate: Boolean): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/${tx.id}"
        return try {
            val uid = requireUserId()
            require(tx.id.isNotBlank()) { "Transaction ID cannot be blank" }
            require(tx.amountMinor > 0L) { "Amount must be greater than zero" }
            require(tx.accountId.isNotBlank()) { "Account ID cannot be blank" }
            path = "users/$uid/transactions/${tx.id}"
            val docRef = userDoc(uid).collection("transactions").document(tx.id)
            if (isUpdate) {
                val payload = mapOf<String, Any?>(
                    "txType" to tx.type.name,
                    "amountMinor" to tx.amountMinor,
                    "currencyCode" to tx.currencyCode.trim().take(5).ifBlank { "USD" },
                    "accountId" to tx.accountId,
                    "destinationAccountId" to tx.destinationAccountId?.takeIf { it.isNotBlank() },
                    "categoryId" to tx.categoryId?.takeIf { it.isNotBlank() },
                    "description" to tx.description.trim().take(160),
                    "merchant" to tx.merchant.trim().take(100),
                    "dateMillis" to tx.dateMillis.coerceAtLeast(1L),
                    "notes" to tx.notes.trim().take(500),
                    "creditCardRepayment" to tx.isCreditCardRepayment,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null } as Map<String, Any>
                docRef.update(payload).await()
            } else {
                docRef.set(tx.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions/$transactionId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/transactions/$transactionId"
            userDoc(uid).collection("transactions").document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBudget(
        budget: BudgetRecord,
        isUpdate: Boolean,
        replacingBudgetIds: List<String> = emptyList(),
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/budgets/${budget.id}"
        return try {
            val uid = requireUserId()
            require(budget.id.isNotBlank()) { "Budget ID cannot be blank" }
            require(budget.limitMinor > 0L) { "Budget limit must be positive" }
            path = "users/$uid/budgets/${budget.id}"
            val budgetsCol = userDoc(uid).collection("budgets")
            if (isUpdate && replacingBudgetIds.isEmpty()) {
                val payload = mapOf<String, Any?>(
                    "name" to budget.name.trim().take(80),
                    "scope" to budget.scope.name,
                    "categoryId" to budget.categoryId?.takeIf { it.isNotBlank() },
                    "limitMinor" to budget.limitMinor,
                    "currencyCode" to budget.currencyCode.trim().take(5).ifBlank { "USD" },
                    "year" to budget.year,
                    "month" to budget.month,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null } as Map<String, Any>
                budgetsCol.document(budget.id).update(payload).await()
            } else {
                val batch = db.batch()
                replacingBudgetIds.forEach { oldId ->
                    if (oldId != budget.id) {
                        batch.delete(budgetsCol.document(oldId))
                    }
                }
                batch.set(budgetsCol.document(budget.id), budget.toCreateMap(uid))
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/budgets/$budgetId"
        return try {
            val uid = requireUserId()
            path = "users/$uid/budgets/$budgetId"
            userDoc(uid).collection("budgets").document(budgetId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCategory(category: CategoryRecord): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/categories/${category.id}"
        return try {
            val uid = requireUserId()
            require(category.id.isNotBlank()) { "Category ID cannot be blank" }
            require(category.name.isNotBlank()) { "Category name cannot be blank" }
            path = "users/$uid/categories/${category.id}"
            userDoc(uid).collection("categories").document(category.id)
                .set(category.toCreateMap(uid))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun commitBatchTransactions(transactions: List<TransactionRecord>): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/transactions"
        return try {
            val uid = requireUserId()
            path = "users/$uid/transactions"
            transactions.chunked(400).forEach { chunk ->
                val batch = db.batch()
                val col = userDoc(uid).collection("transactions")
                chunk.forEach { tx ->
                    batch.set(col.document(tx.id), tx.toCreateMap(uid))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun seedLedgerData(
        accounts: List<AccountRecord>,
        transactions: List<TransactionRecord>,
        budgets: List<BudgetRecord>,
        categories: List<CategoryRecord>,
        previewDataActive: Boolean,
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val root = userDoc(uid)
            val batch = db.batch()
            categories.forEach { cat ->
                batch.set(root.collection("categories").document(cat.id), cat.toCreateMap(uid))
            }
            accounts.forEach { acc ->
                batch.set(root.collection("accounts").document(acc.id), acc.toCreateMap(uid))
            }
            transactions.forEach { tx ->
                batch.set(root.collection("transactions").document(tx.id), tx.toCreateMap(uid))
            }
            budgets.forEach { bud ->
                batch.set(root.collection("budgets").document(bud.id), bud.toCreateMap(uid))
            }
            batch.update(
                root,
                mapOf(
                    "previewDataActive" to previewDataActive,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun clearAllLedgerRecords(
        accountIds: List<String>,
        transactionIds: List<String>,
        budgetIds: List<String>,
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val root = userDoc(uid)
            val allRefs = buildList {
                accountIds.forEach { add(root.collection("accounts").document(it)) }
                transactionIds.forEach { add(root.collection("transactions").document(it)) }
                budgetIds.forEach { add(root.collection("budgets").document(it)) }
            }
            allRefs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
            root.update(
                mapOf(
                    "previewDataActive" to false,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun AccountRecord.toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(80),
        "accountType" to type.name,
        "institutionName" to institutionName.trim().take(80),
        "cardNetwork" to cardNetwork.trim().take(40),
        "lastFour" to lastFour.trim().take(4),
        "currencyCode" to currencyCode.trim().take(5).ifBlank { "USD" },
        "openingBalanceMinor" to openingBalanceMinor,
        "creditLimitMinor" to creditLimitMinor?.coerceAtLeast(0L),
        "description" to description.trim().take(240),
        "archived" to isArchived,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun TransactionRecord.toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "txType" to type.name,
        "amountMinor" to amountMinor.coerceAtLeast(1L),
        "currencyCode" to currencyCode.trim().take(5).ifBlank { "USD" },
        "accountId" to accountId,
        "destinationAccountId" to destinationAccountId?.takeIf { it.isNotBlank() },
        "categoryId" to categoryId?.takeIf { it.isNotBlank() },
        "description" to description.trim().take(160),
        "merchant" to merchant.trim().take(100),
        "dateMillis" to dateMillis.coerceAtLeast(1L),
        "notes" to notes.trim().take(500),
        "creditCardRepayment" to isCreditCardRepayment,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun BudgetRecord.toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(80),
        "scope" to scope.name,
        "categoryId" to categoryId?.takeIf { it.isNotBlank() },
        "limitMinor" to limitMinor.coerceAtLeast(1L),
        "currencyCode" to currencyCode.trim().take(5).ifBlank { "USD" },
        "year" to year.coerceIn(2000, 2100),
        "month" to month.coerceIn(1, 12),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    ).filterValues { it != null } as Map<String, Any>

    private fun CategoryRecord.toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(60),
        "txType" to if (type == TransactionType.INCOME) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
        "iconKey" to iconKey.trim().take(40).ifBlank { "shopping" },
        "defaultCategory" to isDefault,
        "active" to isActive,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    private fun DocumentSnapshot.toUserProfile(): UserProfileRecord {
        val rawTheme = getString("themeMode") ?: ThemeMode.LIGHT.name
        val theme = enumValues<ThemeMode>().firstOrNull { it.name == rawTheme } ?: ThemeMode.LIGHT
        return UserProfileRecord(
            userId = getString("userId") ?: id,
            profileName = getString("profileName") ?: "Alex Morgan",
            defaultCurrencyCode = getString("defaultCurrencyCode") ?: "USD",
            selectedLanguage = getString("selectedLanguage") ?: "English",
            themeMode = theme,
            onboardingComplete = getBoolean("onboardingComplete") ?: false,
            budgetAlertsEnabled = getBoolean("budgetAlertsEnabled") ?: true,
            weeklyDigestEnabled = getBoolean("weeklyDigestEnabled") ?: true,
            dailyReminderEnabled = getBoolean("dailyReminderEnabled") ?: false,
            privacyHideBalances = getBoolean("privacyHideBalances") ?: false,
            isPreviewDataActive = getBoolean("previewDataActive") ?: false,
        )
    }

    private fun DocumentSnapshot.toAccountRecord(): AccountRecord? {
        val name = getString("name") ?: return null
        val rawType = getString("accountType") ?: AccountType.BANK_ACCOUNT.name
        val type = enumValues<AccountType>().firstOrNull { it.name == rawType } ?: AccountType.BANK_ACCOUNT
        return AccountRecord(
            id = id,
            name = name,
            type = type,
            institutionName = getString("institutionName") ?: "",
            cardNetwork = getString("cardNetwork") ?: "",
            lastFour = getString("lastFour") ?: "",
            currencyCode = getString("currencyCode") ?: "USD",
            openingBalanceMinor = getLong("openingBalanceMinor") ?: 0L,
            creditLimitMinor = getLong("creditLimitMinor"),
            description = getString("description") ?: "",
            isArchived = getBoolean("archived") ?: false,
        )
    }

    private fun DocumentSnapshot.toTransactionRecord(): TransactionRecord? {
        val rawType = getString("txType") ?: TransactionType.EXPENSE.name
        val type = enumValues<TransactionType>().firstOrNull { it.name == rawType } ?: TransactionType.EXPENSE
        return TransactionRecord(
            id = id,
            type = type,
            amountMinor = getLong("amountMinor") ?: 0L,
            currencyCode = getString("currencyCode") ?: "USD",
            accountId = getString("accountId") ?: "",
            destinationAccountId = getString("destinationAccountId"),
            categoryId = getString("categoryId"),
            description = getString("description") ?: "",
            merchant = getString("merchant") ?: "",
            dateMillis = getLong("dateMillis") ?: 0L,
            notes = getString("notes") ?: "",
            isCreditCardRepayment = getBoolean("creditCardRepayment") ?: false,
        )
    }

    private fun DocumentSnapshot.toBudgetRecord(): BudgetRecord? {
        val name = getString("name") ?: return null
        val rawScope = getString("scope") ?: BudgetScope.OVERALL.name
        val scope = enumValues<BudgetScope>().firstOrNull { it.name == rawScope } ?: BudgetScope.OVERALL
        return BudgetRecord(
            id = id,
            name = name,
            scope = scope,
            categoryId = getString("categoryId"),
            limitMinor = getLong("limitMinor") ?: 0L,
            currencyCode = getString("currencyCode") ?: "USD",
            year = (getLong("year") ?: 2026L).toInt(),
            month = (getLong("month") ?: 11L).toInt(),
        )
    }

    private fun DocumentSnapshot.toCategoryRecord(): CategoryRecord? {
        val name = getString("name") ?: return null
        val rawType = getString("txType") ?: TransactionType.EXPENSE.name
        val type = enumValues<TransactionType>().firstOrNull { it.name == rawType } ?: TransactionType.EXPENSE
        return CategoryRecord(
            id = id,
            name = name,
            type = type,
            iconKey = getString("iconKey") ?: "shopping",
            isDefault = getBoolean("defaultCategory") ?: false,
            isActive = getBoolean("active") ?: true,
        )
    }
}
