package com.example

import com.example.model.AccountType
import com.example.model.BudgetScope
import com.example.model.MonthPeriod
import com.example.model.TransactionType
import com.example.util.ConversionResult
import com.example.util.MoneyFormatter
import com.example.viewmodel.SpendlyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun creditCardRepayment_reducesLiabilityWithoutCountingAsExpense() {
        val vm = SpendlyViewModel()
        vm.completeOnboarding("USD", "English", loadPreviewSample = false)

        vm.saveAccount(
            existingId = null,
            name = "Checking",
            type = AccountType.BANK_ACCOUNT,
            institutionName = "Summit",
            cardNetwork = "",
            lastFour = "",
            currencyCode = "USD",
            openingBalanceMinor = 100_000L,
            creditLimitMinor = null,
            description = "",
        )
        vm.saveAccount(
            existingId = null,
            name = "Rewards Card",
            type = AccountType.CREDIT_CARD,
            institutionName = "Summit",
            cardNetwork = "Visa",
            lastFour = "1234",
            currencyCode = "USD",
            openingBalanceMinor = 0L,
            creditLimitMinor = 500_000L,
            description = "",
        )

        val state1 = vm.uiState.value
        val checkingId = state1.accounts[0].id
        val cardId = state1.accounts[1].id

        // Record a $40.00 shopping expense on the credit card
        vm.saveTransaction(
            existingId = null,
            type = TransactionType.EXPENSE,
            amountMinor = 4_000L,
            currencyCode = "USD",
            accountId = cardId,
            destinationAccountId = null,
            categoryId = "cat_groceries",
            description = "Groceries",
            merchant = "Market",
            dateMillis = System.currentTimeMillis(),
            notes = "",
        )
        // Record a $40.00 repayment transfer from Checking to Rewards Card
        vm.saveTransaction(
            existingId = null,
            type = TransactionType.TRANSFER,
            amountMinor = 4_000L,
            currencyCode = "USD",
            accountId = checkingId,
            destinationAccountId = cardId,
            categoryId = null,
            description = "Card Repayment",
            merchant = "",
            dateMillis = System.currentTimeMillis(),
            notes = "",
        )

        val state2 = vm.uiState.value
        val checkingBal = state2.accountSummaries.first { it.account.id == checkingId }.calculatedBalanceMinor
        val cardOutstanding = state2.accountSummaries.first { it.account.id == cardId }.calculatedBalanceMinor

        assertEquals(96_000L, checkingBal)
        assertEquals(0L, cardOutstanding)
        assertEquals(4_000L, state2.monthlyExpensesBaseMinor)
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
}
