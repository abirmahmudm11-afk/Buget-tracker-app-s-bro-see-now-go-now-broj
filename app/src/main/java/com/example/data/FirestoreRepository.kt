package com.example.data

import android.content.Context
import com.example.R
import com.example.model.BudgetData
import com.example.model.FirestoreCollections
import com.example.model.TransactionData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Asynchronous Firestore CRUD repository using Kotlin Coroutines and Flow.
 * Enforces user isolation under `/users/{uid}/transactions` and `/users/{uid}/budgets`.
 */
class FirestoreRepository(
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

    private fun transactionsCollection(userId: String) =
        db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.TRANSACTIONS)

    private fun budgetsCollection(userId: String) =
        db.collection(FirestoreCollections.USERS)
            .document(userId)
            .collection(FirestoreCollections.BUDGET)

    fun observeTransactions(userId: String): Flow<List<TransactionData>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "${FirestoreCollections.USERS}/$userId/${FirestoreCollections.TRANSACTIONS}"
        emitAll(
            transactionsCollection(userId)
                .orderBy("dateMillis", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toTransactionData() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw (firestoreError ?: error)
                }
        )
    }.flowOn(Dispatchers.IO)

    suspend fun getTransactions(userId: String): Result<List<TransactionData>> = withContext(Dispatchers.IO) {
        val path = "${FirestoreCollections.USERS}/$userId/${FirestoreCollections.TRANSACTIONS}"
        try {
            val uid = requireUserId()
            require(uid == userId) { "Authenticated UID mismatch." }
            val snapshot = transactionsCollection(uid)
                .orderBy("dateMillis", Query.Direction.DESCENDING)
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { it.toTransactionData() }
            Result.success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createTransaction(transaction: TransactionData): Result<String> = withContext(Dispatchers.IO) {
        val docId = transaction.id.ifBlank { "tx_${UUID.randomUUID().toString().take(8)}" }
        var path = "${FirestoreCollections.USERS}/${auth.currentUser?.uid ?: "unauthenticated"}/${FirestoreCollections.TRANSACTIONS}/$docId"
        try {
            val uid = requireUserId()
            val resolvedMinor = transaction.resolvedAmountMinor()
            require(resolvedMinor > 0L) { "Transaction amount must be positive." }
            path = "${FirestoreCollections.USERS}/$uid/${FirestoreCollections.TRANSACTIONS}/$docId"
            transactionsCollection(uid)
                .document(docId)
                .set(transaction.copy(id = docId).toCreateMap(uid))
                .await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(transaction: TransactionData): Result<Unit> = withContext(Dispatchers.IO) {
        var path = "${FirestoreCollections.USERS}/${auth.currentUser?.uid ?: "unauthenticated"}/${FirestoreCollections.TRANSACTIONS}/${transaction.id}"
        try {
            val uid = requireUserId()
            require(transaction.id.isNotBlank()) { "Transaction ID must not be blank for update." }
            require(transaction.resolvedAmountMinor() > 0L) { "Transaction amount must be positive." }
            path = "${FirestoreCollections.USERS}/$uid/${FirestoreCollections.TRANSACTIONS}/${transaction.id}"
            transactionsCollection(uid)
                .document(transaction.id)
                .update(transaction.toUpdateMap())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        var path = "${FirestoreCollections.USERS}/${auth.currentUser?.uid ?: "unauthenticated"}/${FirestoreCollections.TRANSACTIONS}/$transactionId"
        try {
            val uid = requireUserId()
            require(transactionId.isNotBlank()) { "Transaction ID must not be blank for delete." }
            path = "${FirestoreCollections.USERS}/$uid/${FirestoreCollections.TRANSACTIONS}/$transactionId"
            transactionsCollection(uid)
                .document(transactionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeBudgets(userId: String): Flow<List<BudgetData>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank." }
        val path = "${FirestoreCollections.USERS}/$userId/${FirestoreCollections.BUDGET}"
        emitAll(
            budgetsCollection(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toBudgetData() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw (firestoreError ?: error)
                }
        )
    }.flowOn(Dispatchers.IO)

    suspend fun upsertBudget(budget: BudgetData, isUpdate: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = budget.id.ifBlank { "bud_${UUID.randomUUID().toString().take(8)}" }
        var path = "${FirestoreCollections.USERS}/${auth.currentUser?.uid ?: "unauthenticated"}/${FirestoreCollections.BUDGET}/$docId"
        try {
            val uid = requireUserId()
            require(budget.limitMinor > 0L) { "Budget limit must be positive." }
            path = "${FirestoreCollections.USERS}/$uid/${FirestoreCollections.BUDGET}/$docId"
            val docRef = budgetsCollection(uid).document(docId)
            if (isUpdate) {
                docRef.update(budget.toUpdateMap()).await()
            } else {
                docRef.set(budget.copy(id = docId).toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        var path = "${FirestoreCollections.USERS}/${auth.currentUser?.uid ?: "unauthenticated"}/${FirestoreCollections.BUDGET}/$budgetId"
        try {
            val uid = requireUserId()
            require(budgetId.isNotBlank()) { "Budget ID must not be blank." }
            path = "${FirestoreCollections.USERS}/$uid/${FirestoreCollections.BUDGET}/$budgetId"
            budgetsCollection(uid).document(budgetId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toTransactionData(): TransactionData? {
        val minor = getLong("amountMinor") ?: return null
        val millis = getLong("dateMillis") ?: 0L
        return TransactionData(
            id = id,
            userId = getString("userId") ?: "",
            amount = minor / 100.0,
            amountMinor = minor,
            date = millis,
            category = getString("categoryId") ?: "",
            note = getString("notes") ?: "",
            txType = getString("txType") ?: "EXPENSE",
            currencyCode = getString("currencyCode") ?: "USD",
            accountId = getString("accountId") ?: "",
            destinationAccountId = getString("destinationAccountId"),
            description = getString("description") ?: "",
            merchant = getString("merchant") ?: "",
            creditCardRepayment = getBoolean("creditCardRepayment") ?: false,
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        )
    }

    private fun DocumentSnapshot.toBudgetData(): BudgetData? {
        val name = getString("name") ?: return null
        return BudgetData(
            id = id,
            userId = getString("userId") ?: "",
            name = name,
            scope = getString("scope") ?: "OVERALL",
            category = getString("categoryId"),
            limitMinor = getLong("limitMinor") ?: 0L,
            currencyCode = getString("currencyCode") ?: "USD",
            year = (getLong("year") ?: 2026L).toInt(),
            month = (getLong("month") ?: 11L).toInt(),
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        )
    }
}
