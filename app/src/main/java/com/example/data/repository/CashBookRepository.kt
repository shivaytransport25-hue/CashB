package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CashBook
import com.example.data.model.CashTransaction
import com.example.data.model.FirestoreCollections
import com.example.data.model.Party
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CashBookRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // ------------------------------------------------------------------------
    // CashBook Operations
    // ------------------------------------------------------------------------

    fun observeCashBooks(userId: String? = null): Flow<List<CashBook>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/cashbooks"
        emitAll(
            db.collection("users").document(uid).collection("cashbooks")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CashBook::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createCashBook(
        name: String,
        description: String = "",
        currencySymbol: String = "₹",
        bookId: String = UUID.randomUUID().toString()
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/unauthenticated/cashbooks")
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("cashbooks").document(bookId)
        val book = CashBook(
            id = bookId,
            userId = uid,
            name = name.trim().take(80),
            description = description.trim().take(200),
            currencySymbol = currencySymbol.trim().ifEmpty { "₹" }.take(10),
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(book).await()
            Result.success(bookId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCashBook(book: CashBook): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(book.id.isNotBlank()) { "CashBook ID must not be blank for update" }
        requireNotNull(book.createdAt) { "CashBook createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("cashbooks").document(book.id)
        return try {
            docRef.set(
                book.copy(
                    name = book.name.trim().take(80),
                    description = book.description.trim().take(200),
                    currencySymbol = book.currencySymbol.trim().ifEmpty { "₹" }.take(10),
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCashBook(bookId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("cashbooks").document(bookId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCashBookById(targetUserId: String, bookId: String): Result<CashBook?> {
        val docRef = db.collection("users").document(targetUserId)
            .collection("cashbooks").document(bookId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    CashBook::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // Party Operations
    // ------------------------------------------------------------------------

    fun observeParties(userId: String? = null): Flow<List<Party>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/parties"
        emitAll(
            db.collection("users").document(uid).collection("parties")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Party::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createParty(
        name: String,
        phone: String = "",
        role: String = "Customer",
        notes: String = "",
        partyId: String = UUID.randomUUID().toString()
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid).collection("parties").document(partyId)
        val validRole = if (role in CashTransaction.PARTY_ROLES) role else "Customer"
        val party = Party(
            id = partyId,
            userId = uid,
            name = name.trim().take(80),
            phone = phone.trim().take(30),
            role = validRole,
            notes = notes.trim().take(250),
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(party).await()
            Result.success(partyId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateParty(party: Party): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(party.id.isNotBlank()) { "Party ID must not be blank for update" }
        requireNotNull(party.createdAt) { "Party createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("parties").document(party.id)
        val validRole = if (party.role in CashTransaction.PARTY_ROLES) party.role else "Customer"
        return try {
            docRef.set(
                party.copy(
                    name = party.name.trim().take(80),
                    phone = party.phone.trim().take(30),
                    role = validRole,
                    notes = party.notes.trim().take(250),
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteParty(partyId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.PARTIES).document(partyId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getPartyById(targetUserId: String, partyId: String): Result<Party?> {
        val docRef = db.collection(FirestoreCollections.USERS).document(targetUserId)
            .collection(FirestoreCollections.PARTIES).document(partyId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    Party::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------------
    // CashTransaction (Voucher) Operations
    // ------------------------------------------------------------------------

    fun observeTransactions(userId: String? = null): Flow<List<CashTransaction>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/transactions"
        emitAll(
            db.collection("users").document(uid).collection("transactions")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CashTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTransaction(
        bookId: String,
        type: String,
        amount: Double,
        remark: String,
        category: String,
        paymentMode: String,
        partyId: String = "",
        partyName: String = "",
        transactionDate: Long = System.currentTimeMillis(),
        txId: String = UUID.randomUUID().toString()
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        val validType = if (type == CashTransaction.TYPE_CASH_OUT) {
            CashTransaction.TYPE_CASH_OUT
        } else {
            CashTransaction.TYPE_CASH_IN
        }
        val validMode = if (paymentMode in CashTransaction.PAYMENT_MODES) {
            paymentMode
        } else {
            "Cash"
        }
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
        val tx = CashTransaction(
            id = txId,
            userId = uid,
            bookId = bookId,
            type = validType,
            amount = amount,
            remark = remark.trim().take(250),
            category = category.trim().ifEmpty { "General" }.take(60),
            paymentMode = validMode,
            partyId = partyId.trim().take(128),
            partyName = partyName.trim().take(80),
            transactionDate = if (transactionDate > 0L) transactionDate else System.currentTimeMillis(),
            createdAt = null,
            updatedAt = null
        )
        return try {
            docRef.set(tx).await()
            Result.success(txId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(tx: CashTransaction): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        require(tx.id.isNotBlank()) { "Transaction ID must not be blank for update" }
        requireNotNull(tx.createdAt) { "Transaction createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("transactions").document(tx.id)
        return try {
            docRef.set(
                tx.copy(
                    remark = tx.remark.trim().take(250),
                    category = tx.category.trim().ifEmpty { "General" }.take(60),
                    partyName = tx.partyName.trim().take(80),
                    updatedAt = null
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(txId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(FirestoreCollections.USERS).document(uid)
            .collection(FirestoreCollections.TRANSACTIONS).document(txId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getTransactionById(targetUserId: String, txId: String): Result<CashTransaction?> {
        val docRef = db.collection(FirestoreCollections.USERS).document(targetUserId)
            .collection(FirestoreCollections.TRANSACTIONS).document(txId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    CashTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeTransactionsByParty(partyId: String, userId: String? = null): Flow<List<CashTransaction>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
        val path = FirestoreCollections.transactionsPath(uid)
        emitAll(
            db.collection(FirestoreCollections.USERS).document(uid)
                .collection(FirestoreCollections.TRANSACTIONS)
                .whereEqualTo("userId", uid)
                .whereEqualTo("partyId", partyId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CashTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.transactionDate }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTransactionsByBook(bookId: String, userId: String? = null): Flow<List<CashTransaction>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
        val path = FirestoreCollections.transactionsPath(uid)
        emitAll(
            db.collection(FirestoreCollections.USERS).document(uid)
                .collection(FirestoreCollections.TRANSACTIONS)
                .whereEqualTo("userId", uid)
                .whereEqualTo("bookId", bookId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CashTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.transactionDate }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }
}
