package com.example

import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.BudgetRecord
import com.example.model.BudgetScope
import com.example.model.MonthPeriod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UserProfileRecord
import com.example.util.ConversionResult
import com.example.util.CsvUtils
import com.example.util.MoneyFormatter
import com.example.viewmodel.computeSpendlyUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun csvUtils_parseCsvLine_handlesQuotedCommasAndEscapedQuotes() {
        val fields = CsvUtils.parseCsvLine("2026-11-18,\"Coffee, Bagel \"\"Special\"\"\",12.50,Expense")
        assertEquals(4, fields.size)
        assertEquals("2026-11-18", fields[0])
        assertEquals("Coffee, Bagel \"Special\"", fields[1])
        assertEquals("12.50", fields[2])
        assertEquals("Expense", fields[3])
    }

    @Test
    fun csvUtils_parseCsvRows_rejectsInvalidDatesAndHandlesJpyZeroDecimal() {
        val csv = """
            Date,Description,Amount,Type
            2026-02-31,Leap Overflow,500,Expense
            2026-13-01,Month Overflow,500,Expense
            2026-11-18,"Ramen, Shinjuku",1200,Expense
            2026-11-19,Bonus,50000,Income
        """.trimIndent()

        val rows = CsvUtils.parseCsvRows(
            raw = csv,
            currencyCode = "JPY",
            destAccountId = "acc_jpy",
            isDuplicateCandidate = { _, _, desc, _ -> desc == "Bonus" },
        )

        assertEquals(4, rows.size)
        assertNull(rows[0].parsedDateMillis)
        assertEquals("Invalid date format (expected YYYY-MM-DD)", rows[0].errorMessage)
        assertNull(rows[1].parsedDateMillis)
        assertEquals("Invalid date format (expected YYYY-MM-DD)", rows[1].errorMessage)

        assertNull(rows[2].errorMessage)
        assertEquals("Ramen, Shinjuku", rows[2].description)
        assertEquals(1200L, rows[2].parsedAmountMinor)
        assertFalse(rows[2].isDuplicate)

        assertNull(rows[3].errorMessage)
        assertTrue(rows[3].isDuplicate)
        assertEquals(TransactionType.INCOME, rows[3].type)
    }

    @Test
    fun csvUtils_escapeAndFormat_neutralizesFormulasAndFormatsCurrencies() {
        assertEquals("12.50", CsvUtils.formatMinorForCsv(1250L, "USD"))
        assertEquals("1500", CsvUtils.formatMinorForCsv(1500L, "JPY"))
        assertEquals("\"'=SUM(A1:A2)\"", CsvUtils.escapeCsvField("=SUM(A1:A2)"))
        assertEquals("\"\"\"Quoted\"\", item\"", CsvUtils.escapeCsvField("\"Quoted\", item"))
    }

    @Test
    fun computeSpendlyUiState_guardsSelfTransferAndExcludesArchivedFromNetWorth() {
        val checking = AccountRecord(
            id = "acc_active",
            name = "Active Checking",
            type = AccountType.BANK_ACCOUNT,
            currencyCode = "USD",
            openingBalanceMinor = 50_000L,
        )
        val archived = AccountRecord(
            id = "acc_archived",
            name = "Old Vault",
            type = AccountType.SAVINGS_ACCOUNT,
            currencyCode = "USD",
            openingBalanceMinor = 200_000L,
            isArchived = true,
        )
        val now = System.currentTimeMillis()
        val selfTransfer = TransactionRecord(
            id = "tx_self",
            type = TransactionType.TRANSFER,
            amountMinor = 10_000L,
            currencyCode = "USD",
            accountId = checking.id,
            destinationAccountId = checking.id,
            description = "Self transfer edge case",
            dateMillis = now,
        )

        val state = computeSpendlyUiState(
            profile = UserProfileRecord(defaultCurrencyCode = "USD", onboardingComplete = true),
            accounts = listOf(checking, archived),
            transactions = listOf(selfTransfer),
            budgets = emptyList(),
        )

        val checkingBal = state.accountSummaries.first { it.account.id == checking.id }.calculatedBalanceMinor
        assertEquals(50_000L, checkingBal)
        assertEquals(50_000L, state.totalAssetsBaseMinor)
        assertEquals(50_000L, state.totalNetBalanceBaseMinor)
        assertEquals(1, state.activeAccounts.size)
        assertEquals(1, state.archivedAccounts.size)
    }

    @Test
    fun computeSpendlyUiState_computesBudgetAlertsNearLimitAndOverspent() {
        val checking = AccountRecord(
            id = "acc_checking",
            name = "Checking",
            type = AccountType.BANK_ACCOUNT,
            currencyCode = "USD",
            openingBalanceMinor = 500_000L,
        )
        val period = MonthPeriod.current()
        val diningBudget = BudgetRecord(
            id = "bud_dining",
            name = "Dining",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_dining",
            limitMinor = 10_000L,
            currencyCode = "USD",
            year = period.year,
            month = period.month,
        )
        val groceriesBudget = BudgetRecord(
            id = "bud_groceries",
            name = "Groceries",
            scope = BudgetScope.CATEGORY,
            categoryId = "cat_groceries",
            limitMinor = 10_000L,
            currencyCode = "USD",
            year = period.year,
            month = period.month,
        )

        val now = System.currentTimeMillis()
        val diningTx = TransactionRecord(
            id = "tx_dining",
            type = TransactionType.EXPENSE,
            amountMinor = 8_600L, // 86% -> near limit
            currencyCode = "USD",
            accountId = checking.id,
            categoryId = "cat_dining",
            description = "Bistro",
            dateMillis = now,
        )
        val groceriesTx = TransactionRecord(
            id = "tx_groceries",
            type = TransactionType.EXPENSE,
            amountMinor = 12_000L, // 120% -> overspent
            currencyCode = "USD",
            accountId = checking.id,
            categoryId = "cat_groceries",
            description = "Supermarket",
            dateMillis = now,
        )

        val state = computeSpendlyUiState(
            profile = UserProfileRecord(defaultCurrencyCode = "USD", onboardingComplete = true),
            accounts = listOf(checking),
            transactions = listOf(diningTx, groceriesTx),
            budgets = listOf(diningBudget, groceriesBudget),
            selectedPeriod = period,
        )

        val diningSummary = state.categoryBudgetSummaries.first { it.budget.id == "bud_dining" }
        val groceriesSummary = state.categoryBudgetSummaries.first { it.budget.id == "bud_groceries" }

        assertTrue(diningSummary.isNearLimit)
        assertFalse(diningSummary.isOverspent)
        assertTrue(groceriesSummary.isOverspent)
        assertFalse(groceriesSummary.isNearLimit)
        assertEquals(20_600L, state.monthlyExpensesBaseMinor)
    }
}

