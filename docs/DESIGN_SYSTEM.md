# SPENDLY GLOBAL — Design System & UI/UX Architecture (`docs/DESIGN_SYSTEM.md`)

## 1. Product Design Direction: "Vivid Fintech Canvas" (Light-Mode First)
Spendly embraces a **bright, colorful, joyful Light-Mode-first** personal finance identity. Instead of dark muted templates or sterile gray spreadsheets, Spendly pairs crisp porcelain-white surfaces (`#FFFFFF` cards over `#F4F7FC` canvas) with rich, purposeful color coding so every account type, transaction direction, budget threshold, and spending category is recognizable at a glance.

**Default Theme:** **Light Mode (`ThemeMode.LIGHT`)** is enforced as the out-of-the-box default (with optional Dark Mode toggle in Settings).

**Core Product Principle:** *Organize manually. Understand automatically.*
- Manual account records are always labeled as tracking figures (`Manual tracking value — not bank-verified`).
- Liquid/asset accounts and credit-card liabilities are visually separated with dedicated color coding.
- Missing budgets display an intentional creation card rather than a misleading `0%` progress bar.

---

## 2. Color Palette & Semantic Tokens

### Core Brand & Surface Tokens (Light Default)
| Token | Light Hex | Dark Hex | Usage |
|---|---|---|---|
| `primary` | `#2563EB` (Royal Cobalt) | `#60A5FA` | Primary CTAs, active nav highlights, focus states |
| `primaryContainer` | `#DBEAFE` (Sky Ice) | `#1E3A8A` | Active navigation pills, primary chips |
| `secondary` | `#059669` (Vivid Mint) | `#34D399` | Income, positive savings, healthy budgets |
| `secondaryContainer` | `#D1FAE5` | `#065F46` | Income icon badges, positive status pills |
| `tertiary` | `#7C3AED` (Berry Violet) | `#A78BFA` | Credit cards, analytics accents |
| `tertiaryContainer` | `#EDE9FE` | `#4C1D95` | Violet quick-action tiles, credit card badges |
| `background` | `#F4F7FC` (Porcelain Sky) | `#0B1120` | Root app canvas |
| `surface` | `#FFFFFF` (Pure White) | `#131C31` | Elevated cards, bottom bar, modal sheets |
| `surfaceContainer` | `#EDF2FA` | `#1A2540` | Segmented controls, input backing |
| `outlineVariant` | `#E2E8F0` | `#293548` | Crisp `1.dp` card hairlines |
| `error` | `#E11D48` (Coral Rose) | `#FB7185` | Expenses, validation errors, overspending |

### Multi-Hue Account & Category Accent System
- **Bank Account:** Royal Cobalt gradient (`#1D4ED8 → #3B82F6`)
- **Credit Card:** Berry Violet gradient (`#6D28D9 → #8B5CF6`)
- **Cash Wallet:** Emerald Mint gradient (`#047857 → #10B981`)
- **Prepaid / Travel Card:** Warm Amber-Orange gradient (`#D97706 → #F59E0B`)
- **Savings Account:** Deep Teal gradient (`#0F766E → #14B8A6`)
- **Categories:** Distinct vibrant icon container & accent hues (`#10B981` Groceries, `#F97316` Dining, `#6366F1` Housing, `#0EA5E9` Transit, `#EAB308` Utilities, `#EC4899` Shopping, `#14B8A6` Health, `#8B5CF6` Travel).

---

## 3. Typography System
Bundled locally via `font-util` into `app/src/main/res/font/`:
- **Display & Headings — `Plus Jakarta Sans`:** Energetic geometric clarity for greetings, net cash flow hero figures, and card titles.
- **Body & Controls — `Inter`:** Crisp legibility at all mobile densities and accessibility font scales.
- **Codes & Identifiers — `JetBrains Mono`:** Monospaced alignment for currency codes (`USD`, `EUR`, `BDT`) and masked last-4 digits (`•••• 4821`).
- **Tabular Numerals (`tnum`):** Enforced on all monetary values so columns align vertically.

---

## 4. Spacing, Shape & Component Design Tokens
- **Spacing Scale (`4.dp` / `8.dp` grid):** `SpaceXs = 4.dp`, `SpaceSm = 8.dp`, `SpaceMd = 12.dp`, `SpaceLg = 16.dp`, `SpaceXl = 24.dp`, `SpaceXxl = 32.dp`.
- **Shape Scale:**
  - `extraSmall = 8.dp`, `small = 12.dp` (chips, category badges)
  - `medium = 18.dp` (transaction rows, budget cards)
  - `large = 24.dp` (hero cash-flow card, colorful account cards)
  - `extraLarge = 28.dp` (modal bottom sheets)
- **Touch Targets:** Minimum `48.dp` interactive height/size via `minimumInteractiveComponentSize()`.

---

## 5. Information Architecture & Screen Specs
- **5-Section Bottom Navigation** (switches to `NavigationRail` on `>= 600.dp` tablets): `Home`, `Activity`, prominent central **`Add`** button, `Budgets`, `More`.
- **Onboarding:** Vivid welcome illustration, `USD ($)` preselected searchable currency picker, language selector, privacy note, optional UI preview toggle, and `Get Started`.
- **Home:** Multi-stop vibrant gradient Hero Cash Flow card, 4 colorful Quick Action tiles (`Add Expense`, `Add Income`, `Transfer`, `Accounts`), Monthly Budget progress card, horizontal colorful Account Cards strip, and Recent Activity.
- **Add Transaction:** Segmented `Expense | Income | Transfer` selector, large colorful hero amount input, category & account pickers, smart Credit Card Repayment detection, date picker, and reachable `Save Transaction` CTA.
- **Activity / Accounts / Budgets / Reports / More:** Full searchable & filterable transaction ledger with Undo delete; 8 manual account types with zero sensitive card credential collection; category & overall budgets with alerts & history; Compose `Canvas` Donut + Bar charts with CSV export; and grouped Settings/Import-Export Wizard.
