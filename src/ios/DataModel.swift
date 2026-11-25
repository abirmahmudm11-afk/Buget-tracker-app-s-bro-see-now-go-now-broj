import Foundation
import FirebaseFirestore

enum SpendlyFirestoreCollections {
    static let users = "users"
    static let transactions = "transactions"
    static let budget = "budgets"
}

enum TransactionKind: String, Codable, CaseIterable {
    case expense = "EXPENSE"
    case income = "INCOME"
    case transfer = "TRANSFER"
}

struct TransactionData: Identifiable, Codable, Hashable {
    @DocumentID var id: String?
    var userId: String
    var amount: Double
    var amountMinor: Int64
    var date: Date
    var category: String
    var note: String
    var txType: String
    var currencyCode: String
    var accountId: String
    var destinationAccountId: String?
    var description: String
    var merchant: String
    var creditCardRepayment: Bool

    var resolvedId: String { id ?? UUID().uuidString }

    var resolvedKind: TransactionKind {
        TransactionKind(rawValue: txType) ?? .expense
    }

    init(
        id: String? = nil,
        userId: String = "",
        amount: Double,
        amountMinor: Int64? = nil,
        date: Date = Date(),
        category: String,
        note: String = "",
        txType: String = TransactionKind.expense.rawValue,
        currencyCode: String = "USD",
        accountId: String = "acc_cash_default",
        destinationAccountId: String? = nil,
        description: String = "",
        merchant: String = "",
        creditCardRepayment: Bool = false
    ) {
        self.id = id
        self.userId = userId
        self.amount = amount
        self.amountMinor = amountMinor ?? Int64((amount * 100.0).rounded())
        self.date = date
        self.category = category
        self.note = note
        self.txType = txType
        self.currencyCode = currencyCode
        self.accountId = accountId
        self.destinationAccountId = destinationAccountId
        self.description = description.isEmpty ? note : description
        self.merchant = merchant
        self.creditCardRepayment = creditCardRepayment
    }

    func toFirestoreDictionary(uid: String) -> [String: Any] {
        var payload: [String: Any] = [
            "userId": uid,
            "txType": txType,
            "amountMinor": max(1, amountMinor),
            "currencyCode": currencyCode.isEmpty ? "USD" : currencyCode,
            "accountId": accountId.isEmpty ? "acc_cash_default" : accountId,
            "description": String((description.isEmpty ? note : description).prefix(160)),
            "merchant": String(merchant.prefix(100)),
            "dateMillis": Int64(date.timeIntervalSince1970 * 1000.0),
            "notes": String(note.prefix(500)),
            "creditCardRepayment": creditCardRepayment,
            "createdAt": FieldValue.serverTimestamp(),
            "updatedAt": FieldValue.serverTimestamp()
        ]
        if !category.isEmpty {
            payload["categoryId"] = category
        }
        if let destinationAccountId, !destinationAccountId.isEmpty {
            payload["destinationAccountId"] = destinationAccountId
        }
        return payload
    }
}

struct BudgetData: Identifiable, Codable, Hashable {
    @DocumentID var id: String?
    var userId: String
    var name: String
    var scope: String
    var category: String?
    var limitMinor: Int64
    var currencyCode: String
    var year: Int
    var month: Int
}
