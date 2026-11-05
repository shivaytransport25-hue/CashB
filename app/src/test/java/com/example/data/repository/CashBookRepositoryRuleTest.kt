package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CashTransaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CashBookRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveCashBook_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CashBookRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCashBook(
                name = "Main Office Cash Book",
                description = "Daily register",
                currencySymbol = "₹"
            )
        }
        assertTrue(createResult.isSuccess)
        val bookId = createResult.getOrThrow()

        val emittedBooks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCashBooks(uid).first { list -> list.any { it.id == bookId } }
        }
        assertTrue(emittedBooks.any { it.id == bookId && it.name == "Main Office Cash Book" })
    }

    @Test
    fun createPartyAndTransaction_withParentBook_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CashBookRepository(firestore)

        val bookId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCashBook("Fleet Ledger", "Transport operations", "₹").getOrThrow()
        }

        val partyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createParty(
                name = "Sharma Logistics",
                phone = "9876543210",
                role = "Customer",
                notes = "Freight account"
            ).getOrThrow()
        }

        val txId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTransaction(
                bookId = bookId,
                type = CashTransaction.TYPE_CASH_IN,
                amount = 15000.0,
                remark = "Trip #402 freight advance",
                category = "Freight / Trip Receipt",
                paymentMode = "UPI",
                partyId = partyId,
                partyName = "Sharma Logistics"
            ).getOrThrow()
        }

        val emittedTxs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions(uid).first { list -> list.any { it.id == txId } }
        }
        assertTrue(emittedTxs.any { it.id == txId && it.amount == 15000.0 })
    }

    @Test
    fun getCashBookById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CashBookRepository(firestore)
        val bookId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCashBook("Alice Private Ledger").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CashBookRepository(firestore)
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCashBookById(aliceUid, bookId)
        }
        assertTrue(bobResult.isFailure)
        val firestoreEx = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeCashBooks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = CashBookRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCashBooks("unauthenticated_uid").first()
            }
            fail("Expected PERMISSION_DENIED exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                firestoreException.code
            )
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_cb@test.com"
        const val BOB_EMAIL = "bob_cb@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
