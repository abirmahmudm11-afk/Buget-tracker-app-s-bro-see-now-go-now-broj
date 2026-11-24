import SwiftUI
import FirebaseCore

@main
struct SpendlyApp: App {
    @AppStorage("spendly_dark_mode") private var isDarkMode: Bool = false

    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            RootContentView()
                .preferredColorScheme(isDarkMode ? .dark : .light)
        }
    }
}

struct RootContentView: View {
    @StateObject private var repository = FirestoreRepository()

    var body: some View {
        NavigationStack {
            TransactionListView(repository: repository)
        }
    }
}
