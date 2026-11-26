package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.BudgetRecord
import com.example.model.BudgetScope
import com.example.model.HomeTransactionFilter
import com.example.model.MonthPeriod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UserProfileRecord
import com.example.ui.components.QuickExpenseModalSheet
import com.example.ui.screens.AccountDetailScreen
import com.example.ui.screens.AccountFormSheet
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsDetailScreen
import com.example.ui.theme.SpendlyTheme
import com.example.viewmodel.SpendlyUiState
import com.example.viewmodel.computeSpendlyUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpendlyUiFlowsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleUiState(
        currencyCode: String = "USD",
        accounts: List<AccountRecord> = listOf(
            AccountRecord(
                id = "acc_checking",
                name = "Summit Checking",
                type = AccountType.BANK_ACCOUNT,
                institutionName = "Summit",
                currencyCode = currencyCode,
                openingBalanceMinor = 250_000L,
            ),
            AccountRecord(
                id = "acc_jpy",
                name = "Tokyo Travel Card",
                type = AccountType.DEBIT_CARD,
                institutionName = "MUFG",
                lastFour = "4412",
                currencyCode = "JPY",
                openingBalanceMinor = 50_000L,
            ),
        ),
        transactions: List<TransactionRecord> = listOf(
            TransactionRecord(
                id = "tx_1",
                type = TransactionType.EXPENSE,
                amountMinor = 2_450L,
                currencyCode = currencyCode,
                accountId = "acc_checking",
                categoryId = "cat_groceries",
                description = "Organic Market",
                merchant = "Whole Foods",
                dateMillis = System.currentTimeMillis(),
            ),
        ),
        budgets: List<BudgetRecord> = emptyList(),
    ): SpendlyUiState {
        return computeSpendlyUiState(
            profile = UserProfileRecord(
                userId = "user_test",
                profileName = "Alex Morgan",
                defaultCurrencyCode = currencyCode,
                onboardingComplete = true,
            ),
            accounts = accounts,
            transactions = transactions,
            budgets = budgets,
            selectedPeriod = MonthPeriod.current(),
        )
    }

    @Test
    fun authScreen_rendersCapabilitiesAndDismissesErrorBanner() {
        composeTestRule.setContent {
            SpendlyTheme {
                AuthScreen(
                    onAuthSuccess = {},
                    enableAutoSignIn = false,
                    initialError = "Sign-in was cancelled. Tap Continue with Google to retry.",
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_hero_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_error_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign-in was cancelled. Tap Continue with Google to retry.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("auth_error_dismiss").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("auth_error_banner").assertDoesNotExist()
    }

    @Test
    fun onboardingScreen_switchesLanguageTogglesSampleAndConfirms() {
        var capturedCurrency = ""
        var capturedLanguage = ""
        var capturedSample = true

        composeTestRule.setContent {
            SpendlyTheme {
                OnboardingScreen(
                    initialCurrencyCode = "USD",
                    initialLanguage = "English",
                    onGetStarted = { cur, lang, sample ->
                        capturedCurrency = cur
                        capturedLanguage = lang
                        capturedSample = sample
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_language_selector").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_lang_option_1").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_sample_switch").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("onboarding_get_started").performClick()
        composeTestRule.waitForIdle()

        assertEquals("USD", capturedCurrency)
        assertEquals("বাংলা (Bangla)", capturedLanguage)
        assertFalse(capturedSample)
    }

    @Test
    fun homeScreen_togglesPrivacyAndSwitchesFilters() {
        var privacyToggled = false
        var selectedFilter: HomeTransactionFilter? = null

        composeTestRule.setContent {
            SpendlyTheme {
                HomeScreen(
                    uiState = sampleUiState(),
                    onSelectPeriod = {},
                    onSelectHomeFilter = { selectedFilter = it },
                    onToggleTheme = {},
                    onTogglePrivacy = { privacyToggled = true },
                    onOpenSettings = {},
                    onAddExpense = {},
                    onAddIncome = {},
                    onAddTransfer = {},
                    onQuickLogExpense = { _, _, _, _, _ -> },
                    onViewAllTransactions = {},
                    onManageAccounts = {},
                    onOpenReports = {},
                    onOpenAccountDetail = {},
                    onOpenBudgets = {},
                    onOpenTransactionDetail = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_privacy_toggle").performClick()
        composeTestRule.onNodeWithTag("home_filter_expense").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(privacyToggled)
        assertEquals(HomeTransactionFilter.EXPENSE, selectedFilter)
    }

    @Test
    fun quickExpenseModalSheet_validatesBlankAmountAndSubmitsQuickChip() {
        var savedMinor: Long? = null
        var savedNote: String? = null

        composeTestRule.setContent {
            SpendlyTheme {
                QuickExpenseModalSheet(
                    uiState = sampleUiState(),
                    onDismiss = {},
                    onSaveExpense = { amountMinor, _, _, _, note ->
                        savedMinor = amountMinor
                        savedNote = note
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("quick_expense_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Enter an amount greater than zero").assertIsDisplayed()
        assertEquals(null, savedMinor)

        composeTestRule.onNodeWithTag("quick_expense_chip_10").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("quick_expense_note_input").performScrollTo().performTextInput("Espresso")
        composeTestRule.onNodeWithTag("quick_expense_save_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(1000L, savedMinor)
        assertEquals("Espresso", savedNote)
    }

    @Test
    fun addTransactionScreen_validatesBlankSubmitAndSubmitsJpyQuickChip() {
        var savedAmountMinor: Long? = null
        var savedCurrency: String? = null
        var savedDesc: String? = null

        composeTestRule.setContent {
            SpendlyTheme {
                AddTransactionScreen(
                    uiState = sampleUiState(),
                    initialType = TransactionType.EXPENSE,
                    initialAccountId = "acc_checking",
                    editingTransaction = null,
                    onNavigateBack = {},
                    onCreateAccountFirst = {},
                    onSaveTransaction = { _, _, amountMinor, currencyCode, _, _, _, description, _, _, _ ->
                        savedAmountMinor = amountMinor
                        savedCurrency = currencyCode
                        savedDesc = description
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Enter an amount greater than zero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add a short description").assertIsDisplayed()
        assertEquals(null, savedAmountMinor)

        composeTestRule.onNodeWithTag("pick_acc_acc_jpy").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tx_quick_chip_10").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("tx_amount_input").assertTextContains("10")
        composeTestRule.onNodeWithTag("tx_description_input").performScrollTo().performTextInput("Shinjuku Ramen")
        composeTestRule.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(10L, savedAmountMinor)
        assertEquals("JPY", savedCurrency)
        assertEquals("Shinjuku Ramen", savedDesc)
    }

    @Test
    fun budgetsScreen_editingJpyBudgetDisplaysWholeNumberAndDeletesCleanly() {
        val period = MonthPeriod.current()
        val jpyBudget = BudgetRecord(
            id = "bud_jpy",
            name = "Tokyo Overall",
            scope = BudgetScope.OVERALL,
            categoryId = null,
            limitMinor = 50_000L,
            currencyCode = "JPY",
            year = period.year,
            month = period.month,
        )
        var deletedId: String? = null

        composeTestRule.setContent {
            SpendlyTheme {
                BudgetsScreen(
                    uiState = sampleUiState(currencyCode = "JPY", budgets = listOf(jpyBudget)),
                    onSelectPeriod = {},
                    onSaveBudget = { _, _, _, _, _, _, _ -> },
                    onDeleteBudget = { deletedId = it },
                    onOpenTransactionDetail = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("budget_card_bud_jpy").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("budget_limit_input").assertTextContains("50000")
        composeTestRule.onNodeWithTag("budget_remove_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("bud_jpy", deletedId)
    }

    @Test
    fun accountDetailScreen_deleteConfirmNavigatesBackExactlyOnce() {
        var state by mutableStateOf(sampleUiState())
        var navigateBackCount = 0

        composeTestRule.setContent {
            SpendlyTheme {
                AccountDetailScreen(
                    accountId = "acc_checking",
                    uiState = state,
                    onNavigateBack = { navigateBackCount++ },
                    onSaveAccount = { id, _, _, _, _, _, _, _, _, _ -> id ?: "acc_checking" },
                    onToggleArchive = {},
                    onDeleteAccount = { deletedId ->
                        state = computeSpendlyUiState(
                            profile = UserProfileRecord(defaultCurrencyCode = "USD", onboardingComplete = true),
                            accounts = state.accounts.filterNot { it.id == deletedId },
                            transactions = emptyList(),
                            budgets = emptyList(),
                        )
                    },
                    onRecordTransactionForAccount = {},
                    onOpenTransactionDetail = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("account_detail_delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("account_delete_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, navigateBackCount)
    }

    @Test
    fun accountFormSheet_validatesNicknameAndLastFourDigits() {
        var saveCalled = false

        composeTestRule.setContent {
            SpendlyTheme {
                AccountFormSheet(
                    initialCurrency = "USD",
                    editingAccount = null,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _, _, _ ->
                        saveCalled = true
                        "acc_new"
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("account_last_four_input").performScrollTo().performTextInput("12")
        composeTestRule.onNodeWithTag("save_account_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Enter an account name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter exactly 4 digits or leave blank").assertIsDisplayed()
        assertFalse(saveCalled)
    }

    @Test
    fun categoriesScreen_rejectsDuplicateCategoryName() {
        var addedCategoryName: String? = null

        composeTestRule.setContent {
            SpendlyTheme {
                CategoriesScreen(
                    uiState = sampleUiState(),
                    onNavigateBack = {},
                    onAddCategory = { name, _, _ -> addedCategoryName = name },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cat_add_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("cat_name_input").performTextInput("Groceries")
        composeTestRule.onNodeWithTag("cat_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("A expense category named \"Groceries\" already exists").assertIsDisplayed()
        assertEquals(null, addedCategoryName)
    }

    @Test
    fun importExportScreen_previewsCsvCountsAndBackStepReturnsToStepOne() {
        composeTestRule.setContent {
            SpendlyTheme {
                ImportExportScreen(
                    uiState = sampleUiState(),
                    onNavigateBack = {},
                    isDuplicateCandidate = { _, _, _, _ -> false },
                    onCommitCsvImport = { _, _, _ -> 3 },
                    onRestoreBackupSample = {},
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("csv_preview_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Preview: 3 ready • 0 duplicate(s) skipped • 1 error(s)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("csv_back_step_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("csv_preview_button").assertIsDisplayed()
    }

    @Test
    fun settingsProfile_rejectsBlankNameAndSignOutConfirms() {
        var savedName: String? = null
        var navigatedBack = false
        var signedOut = false

        composeTestRule.setContent {
            SpendlyTheme {
                SettingsDetailScreen(
                    section = "profile",
                    uiState = sampleUiState(),
                    onNavigateBack = { navigatedBack = true },
                    onSetThemeMode = {},
                    onSetLanguage = {},
                    onUpdatePreferences = { name, _, _, _ -> savedName = name },
                    onSignOut = { signedOut = true },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Enter a display name").assertIsDisplayed()
        assertEquals(null, savedName)
        assertFalse(navigatedBack)

        composeTestRule.onNodeWithTag("profile_sign_out_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sign_out_confirm_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(signedOut)
    }

    @Test
    fun moreScreen_signOutDialogInvokesCallback() {
        var signedOut = false

        composeTestRule.setContent {
            SpendlyTheme {
                MoreScreen(
                    uiState = sampleUiState(),
                    onNavigateAccounts = {},
                    onNavigateReports = {},
                    onNavigateCategories = {},
                    onNavigateImportExport = {},
                    onNavigateCurrency = {},
                    onNavigateSettingsDetail = {},
                    onTogglePreviewData = {},
                    onSignOut = { signedOut = true },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("more_sign_out").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("more_sign_out_confirm").performClick()
        composeTestRule.waitForIdle()

        assertTrue(signedOut)
    }
}
