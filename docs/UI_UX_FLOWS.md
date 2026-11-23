# SPENDLY — UI/UX Interaction & Flow Specification (`docs/UI_UX_FLOWS.md`)

## 1. Global Interactive States
Every interactive surface, card, button, filter chip, and form across Spendly handles:
- **Default:** Material 3 tonal surface with crisp `1.dp` `outlineVariant` hairline border.
- **Pressed:** Material ripple feedback (`Modifier.clickable` / `Surface(onClick = ...)`).
- **Focused:** Keyboard/D-pad highlight and primary outline on active text fields.
- **Disabled:** Reduced contrast + disabled action button during async submission (`isSaving = true`).
- **Loading:** Inline `CircularProgressIndicator` on primary save buttons to prevent double submission.
- **Success:** Navigation back + `Snackbar` confirmation (with working **Undo** on transaction deletion).
- **Error:** Inline field-level error messages while preserving all user-entered form fields.
- **Empty:** Custom editorial illustration (`img_empty_ledger`) + helpful guidance + direct primary CTA.
- **Offline:** All core tracking, filtering, budgeting, and reporting operate local-first without network dependency.

## 2. Key User Flows
1. **Onboarding -> Home:**
   - Preselects `USD ($)` and `English`.
   - Currency button opens `SearchableCurrencySheet`; preference updates only on `Confirm`.
   - `Get Started` transitions to `Home` with optional editable preview data (toggleable off to inspect empty states).
2. **Home -> Quick Actions & Drilldowns:**
   - `MonthSelectorChip` opens `MonthPickerSheet` and recalculates all monthly expenses, income, net flow, and budgets.
   - `Add Expense` / `Add Income` opens `AddTransactionScreen` preselected and auto-focused on amount input.
   - `View All` opens `Activity` pre-filtered to the selected month.
   - `Manage Accounts` opens `AccountsScreen`.
   - Tapping any transaction row opens `TransactionDetailSheet` (`Edit` or `Delete` with confirmation + `Undo` snackbar).
3. **Add / Edit Transaction:**
   - Segmented `Expense | Income | Transfer` selector dynamically filters categories or switches to `From Account` -> `To Account`.
   - Transfers to a `Credit Card` display a live repayment badge and reduce card liability without counting as a second shopping expense.
4. **Accounts & Cards:**
   - Supports all 8 account types (`Bank Account`, `Debit Card`, `Credit Card`, `Prepaid Card`, `Cash Wallet`, `Digital Wallet`, `Savings Account`, `Custom Account`).
   - Explicitly blocks and never asks for full card numbers, CVVs, PINs, or bank passwords.
5. **CSV Import Wizard:**
   - Paste/edit CSV -> Map destination account & currency -> Preview rows with duplicate detection & validation error badges -> Explicit `Confirm Import` before adding valid rows.
