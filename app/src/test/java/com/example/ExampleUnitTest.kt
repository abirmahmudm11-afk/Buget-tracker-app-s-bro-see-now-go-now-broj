package com.example

import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UserProfileRecord
import com.example.util.ConversionResult
import com.example.util.MoneyFormatter
import com.example.viewmodel.computeSpendlyUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun creditCardRepayment_reducesLiabilityWithoutCountingAsExpense() {
        val checking = AccountRecord(
            id = "acc_checking",
            name = "Checking",
            type = AccountType.BANK_ACCOUNT,
            institutionName = "Summit",
            currencyCode = "USD",
            openingBalanceMinor = 100_000L,
        )
        val card = AccountRecord(
            id = "acc_card",
            name = "Rewards Card",
            type = AccountType.CREDIT_CARD,
            institutionName = "Summit",
            cardNetwork = "Visa",
            lastFour = "1234",
            currencyCode = "USD",
            openingBalanceMinor = 0L,
            creditLimitMinor = 500_000L,
        )

        val now = System.currentTimeMillis()
        val groceryExpense = TransactionRecord(
            id = "tx_expense",
            type = TransactionType.EXPENSE,
            amountMinor = 4_000L,
            currencyCode = "USD",
            accountId = card.id,
            categoryId = "cat_groceries",
            description = "Groceries",
            merchant = "Market",
            dateMillis = now,
        )
        val repayment = TransactionRecord(
            id = "tx_repay",
            type = TransactionType.TRANSFER,
            amountMinor = 4_000L,
            currencyCode = "USD",
            accountId = checking.id,
            destinationAccountId = card.id,
            description = "Card Repayment",
            dateMillis = now,
            isCreditCardRepayment = true,
        )

        val state = computeSpendlyUiState(
            profile = UserProfileRecord(defaultCurrencyCode = "USD", onboardingComplete = true),
            accounts = listOf(checking, card),
            transactions = listOf(groceryExpense, repayment),
            budgets = emptyList(),
        )

        val checkingBal = state.accountSummaries.first { it.account.id == checking.id }.calculatedBalanceMinor
        val cardOutstanding = state.accountSummaries.first { it.account.id == card.id }.calculatedBalanceMinor

        assertEquals(96_000L, checkingBal)
        assertEquals(0L, cardOutstanding)
        assertEquals(4_000L, state.monthlyExpensesBaseMinor)
    }

    @Test
    fun missingExchangeRate_returnsUnavailableInsteadOfFabricating() {
        val result = MoneyFormatter.convertMinor(10_000L, "TRY", "USD")
        assertTrue(result is ConversionResult.Unavailable)
    }

    @Test
    fun parseMajorToMinor_enforcesCurrencyPrecision() {
        assertEquals(12550L, MoneyFormatter.parseMajorToMinor("125.50", "USD"))
        assertEquals(null, MoneyFormatter.parseMajorToMinor("125.509", "USD"))
        assertEquals(1500L, MoneyFormatter.parseMajorToMinor("1500", "JPY"))
        assertEquals(null, MoneyFormatter.parseMajorToMinor("1500.5", "JPY"))
    }

    @Test
    fun transactionData_createAndUpdateMaps_conformToFirestoreRules() {
        val data = com.example.model.TransactionData(
            id = "tx_coffee",
            amount = 4.75,
            date = 0L,
            category = "cat_dining",
            note = "Morning latte",
            currencyCode = "$",
            accountId = "",
        )
        val createMap = data.toCreateMap("user_123")
        assertEquals("user_123", createMap["userId"])
        assertEquals("EXPENSE", createMap["txType"])
        assertEquals(475L, createMap["amountMinor"])
        assertEquals("USD", createMap["currencyCode"])
        assertEquals("acc_cash_default", createMap["accountId"])
        assertEquals("cat_dining", createMap["categoryId"])
        assertEquals("Morning latte", createMap["description"])
        assertEquals(1L, createMap["dateMillis"])
        assertTrue(createMap.containsKey("createdAt"))
        assertTrue(createMap.containsKey("updatedAt"))
        assertTrue(!createMap.containsKey("id"))
        assertTrue(!createMap.containsKey("destinationAccountId"))

        val updateMap = data.copy(category = "").toUpdateMap()
        assertEquals(com.google.firebase.firestore.FieldValue.delete(), updateMap["categoryId"])
        assertEquals(com.google.firebase.firestore.FieldValue.delete(), updateMap["destinationAccountId"])
        assertTrue(!updateMap.containsKey("createdAt"))
    }
}
