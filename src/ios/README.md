# Spendly iOS Format (`src/ios/`)

Native **Swift / SwiftUI** (iOS 17+) implementation matching the Android Kotlin/Compose Spendly architecture, Firebase Firestore schema, and 120Hz ProMotion spring animations.

## Structure
- `SpendlyApp.swift` — SwiftUI app entry point with Firebase initialization and color-scheme preference support.
- `Models/DataModel.swift` — `TransactionData` (`amount`, `date`, `category`, `note`, `amountMinor`) and `BudgetData` (`Codable`, `Identifiable`, `Hashable`) matching `/users/{uid}/transactions` and `/users/{uid}/budgets`.
- `Data/FirestoreRepository.swift` — Swift Concurrency (`async`/`await` & `AsyncThrowingStream`) CRUD repository targeting the configured named Firestore database.
- `Views/TransactionListView.swift` — Smooth `LazyVStack` transaction list with SF Symbols category icons, tabular monospaced figures, and ProMotion `.spring` transitions.
