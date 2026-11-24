import SwiftUI

struct TransactionListView: View {
    @ObservedObject var repository: FirestoreRepository

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                if repository.transactions.isEmpty {
                    ContentUnavailableView(
                        "No Expenses Yet",
                        systemImage: "tray",
                        description: Text("Transactions synced from Firestore will appear here.")
                    )
                    .padding(.top, 48)
                } else {
                    ForEach(repository.transactions, id: \.resolvedId) { tx in
                        TransactionRowCard(transaction: tx)
                            .transition(.move(edge: .bottom).combined(with: .opacity))
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .animation(.spring(response: 0.38, dampingFraction: 0.82), value: repository.transactions)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Spendly Activity")
        .onAppear {
            repository.startObservingTransactions()
        }
    }
}

private struct TransactionRowCard: View {
    let transaction: TransactionData
    @State private var isPressed = false

    private var categoryStyle: (symbol: String, tint: Color) {
        switch transaction.category.lowercased() {
        case "groceries", "cat_groceries": return ("cart.fill", Color(red: 0.02, green: 0.59, blue: 0.41))
        case "dining", "cat_dining": return ("fork.knife", Color(red: 0.92, green: 0.35, blue: 0.05))
        case "housing", "cat_housing": return ("house.fill", Color(red: 0.15, green: 0.39, blue: 0.92))
        case "transport", "cat_transport": return ("car.fill", Color(red: 0.01, green: 0.52, blue: 0.78))
        case "utilities", "cat_utilities": return ("bolt.fill", Color(red: 0.85, green: 0.47, blue: 0.02))
        case "shopping", "cat_shopping": return ("bag.fill", Color(red: 0.49, green: 0.23, blue: 0.93))
        case "health", "cat_health": return ("cross.case.fill", Color(red: 0.88, green: 0.11, blue: 0.28))
        case "travel", "cat_travel": return ("airplane", Color(red: 0.05, green: 0.58, blue: 0.53))
        default: return ("chart.pie.fill", Color.accentColor)
        }
    }

    private var formattedAmount: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = transaction.currencyCode
        let value = Double(transaction.amountMinor) / 100.0
        let raw = formatter.string(from: NSNumber(value: value)) ?? String(format: "%.2f", value)
        switch transaction.resolvedKind {
        case .expense: return "-\(raw)"
        case .income: return "+\(raw)"
        case .transfer: return raw
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(categoryStyle.tint.opacity(0.14))
                    .frame(width: 44, height: 44)
                Image(systemName: categoryStyle.symbol)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(categoryStyle.tint)
            }

            VStack(alignment: .leading, spacing: 3) {
                Text(transaction.description.isEmpty ? transaction.category : transaction.description)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text("\(transaction.category.replacingOccurrences(of: "cat_", with: "").capitalized) • \(transaction.date.formatted(date: .abbreviated, time: .omitted))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            Text(formattedAmount)
                .font(.subheadline.weight(.bold).monospacedDigit())
                .foregroundStyle(transaction.resolvedKind == .income ? Color.green : Color.red)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 13)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .scaleEffect(isPressed ? 0.975 : 1.0)
        .animation(.spring(response: 0.26, dampingFraction: 0.72), value: isPressed)
        .onLongPressGesture(minimumDuration: 0.1, pressing: { pressing in
            isPressed = pressing
        }, perform: {})
    }
}
