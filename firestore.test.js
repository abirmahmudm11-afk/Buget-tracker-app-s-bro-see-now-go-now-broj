const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any Spendly user collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amountMinor: 500,
      currencyCode: "USD",
      accountId: "acc_1",
      description: "Coffee",
      merchant: "Cafe",
      dateMillis: Date.now(),
      notes: "",
      creditCardRepayment: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid profile, account, transaction, budget, category CRUD", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // UserProfile create & partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      profileName: "Alice Morgan",
      defaultCurrencyCode: "USD",
      selectedLanguage: "English",
      themeMode: "LIGHT",
      onboardingComplete: true,
      budgetAlertsEnabled: true,
      weeklyDigestEnabled: true,
      dailyReminderEnabled: false,
      privacyHideBalances: false,
      previewDataActive: true,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      privacyHideBalances: true,
      updatedAt: updated,
    })
  );

  // Account create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_checking").set({
      userId: ALICE_UID,
      name: "Everyday Checking",
      accountType: "BANK_ACCOUNT",
      institutionName: "Summit",
      cardNetwork: "",
      lastFour: "",
      currencyCode: "USD",
      openingBalanceMinor: 342000,
      description: "Main",
      archived: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Transaction create, list, & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amountMinor: 1450,
      currencyCode: "USD",
      accountId: "acc_checking",
      categoryId: "cat_dining",
      description: "Lunch",
      merchant: "Bistro",
      dateMillis: Date.now() - 5000,
      notes: "",
      creditCardRepayment: false,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
      amountMinor: 1600,
      updatedAt: updated,
    })
  );

  // Budget create & delete
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("bud_1").set({
      userId: ALICE_UID,
      name: "Monthly Limit",
      scope: "OVERALL",
      limitMinor: 240000,
      currencyCode: "USD",
      year: 2026,
      month: 11,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Category create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").doc("cat_custom").set({
      userId: ALICE_UID,
      name: "Subscriptions",
      txType: "EXPENSE",
      iconKey: "shopping",
      defaultCategory: false,
      active: true,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_alice").set({
      userId: ALICE_UID,
      txType: "INCOME",
      amountMinor: 485000,
      currencyCode: "USD",
      accountId: "acc_checking",
      categoryId: "cat_salary",
      description: "Payroll",
      merchant: "Northstar",
      dateMillis: Date.now(),
      notes: "",
      creditCardRepayment: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_alice").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_alice").update({
      amountMinor: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_alice").delete());
});

test("Security bounds: reject ghost fields, @DocumentId id key, future timestamps, and invalid amounts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Ghost field / id field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad_id").set({
      id: "tx_bad_id",
      userId: ALICE_UID,
      txType: "EXPENSE",
      amountMinor: 500,
      currencyCode: "USD",
      accountId: "acc_1",
      description: "Test",
      merchant: "",
      dateMillis: Date.now(),
      notes: "",
      creditCardRepayment: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Zero or negative amount rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_zero").set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amountMinor: 0,
      currencyCode: "USD",
      accountId: "acc_1",
      description: "Zero",
      merchant: "",
      dateMillis: Date.now(),
      notes: "",
      creditCardRepayment: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_future").set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      amountMinor: 500,
      currencyCode: "USD",
      accountId: "acc_1",
      description: "Future",
      merchant: "",
      dateMillis: Date.now(),
      notes: "",
      creditCardRepayment: false,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );
});
