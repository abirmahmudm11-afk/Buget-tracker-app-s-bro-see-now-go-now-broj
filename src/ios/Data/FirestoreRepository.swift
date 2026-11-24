import Foundation
import Combine
import FirebaseAuth
import FirebaseFirestore

@MainActor
final class FirestoreRepository: ObservableObject {
    @Published private(set) var transactions: [TransactionData] = []
    @Published private(set) var isSyncing: Bool = false
    @Published var errorMessage: String?

    private let db: Firestore
    private var listenerRegistration: ListenerRegistration?

    init(databaseId: String = "ai-studio-android-bugettra-7d767511-376c-4493-be00-16332e1c1a33") {
        self.db = Firestore.firestore(database: databaseId)
    }

    deinit {
        listenerRegistration?.remove()
    }

    private func requireUID() throws -> String {
        guard let uid = Auth.auth().currentUser?.uid, !uid.isEmpty else {
            throw NSError(
                domain: "SpendlyAuth",
                code: 401,
                userInfo: [NSLocalizedDescriptionKey: "Sign in with Google or Apple before accessing Firestore."]
            )
        }
        return uid
    }

    private func transactionsCollection(uid: String) -> CollectionReference {
        db.collection(SpendlyFirestoreCollections.users)
            .document(uid)
            .collection(SpendlyFirestoreCollections.transactions)
    }

    func startObservingTransactions() {
        listenerRegistration?.remove()
        guard let uid = Auth.auth().currentUser?.uid, !uid.isEmpty else { return }
        isSyncing = true

        listenerRegistration = transactionsCollection(uid: uid)
            .order(by: "dateMillis", descending: true)
            .addSnapshotListener { [weak self] snapshot, error in
                Task { @MainActor in
                    guard let self else { return }
                    self.isSyncing = false
                    if let error {
                        self.errorMessage = error.localizedDescription
                        return
                    }
                    let docs = snapshot?.documents ?? []
                    self.transactions = docs.compactMap { doc in
                        let data = doc.data()
                        guard let minor = data["amountMinor"] as? Int64 else { return nil }
                        let millis = data["dateMillis"] as? Int64 ?? 0
                        return TransactionData(
                            id: doc.documentID,
                            userId: data["userId"] as? String ?? uid,
                            amount: Double(minor) / 100.0,
                            amountMinor: minor,
                            date: Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0),
                            category: data["categoryId"] as? String ?? "",
                            note: data["notes"] as? String ?? "",
                            txType: data["txType"] as? String ?? TransactionKind.expense.rawValue,
                            currencyCode: data["currencyCode"] as? String ?? "USD",
                            accountId: data["accountId"] as? String ?? "",
                            destinationAccountId: data["destinationAccountId"] as? String,
                            description: data["description"] as? String ?? "",
                            merchant: data["merchant"] as? String ?? "",
                            creditCardRepayment: data["creditCardRepayment"] as? Bool ?? false
                        )
                    }
                }
            }
    }

    func createTransaction(_ transaction: TransactionData) async throws -> String {
        let uid = try requireUID()
        let docId = transaction.id ?? "tx_\(UUID().uuidString.prefix(8).lowercased())"
        try await transactionsCollection(uid: uid)
            .document(docId)
            .setData(transaction.toFirestoreDictionary(uid: uid))
        return docId
    }

    func updateTransaction(_ transaction: TransactionData) async throws {
        let uid = try requireUID()
        guard let docId = transaction.id, !docId.isEmpty else {
            throw NSError(domain: "SpendlyFirestore", code: 400, userInfo: [NSLocalizedDescriptionKey: "Missing transaction ID"])
        }
        var payload = transaction.toFirestoreDictionary(uid: uid)
        payload.removeValue(forKey: "createdAt")
        payload.removeValue(forKey: "userId")
        try await transactionsCollection(uid: uid).document(docId).updateData(payload)
    }

    func deleteTransaction(id: String) async throws {
        let uid = try requireUID()
        try await transactionsCollection(uid: uid).document(id).delete()
    }
}
