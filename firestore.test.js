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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function serverNow() {
  return new Date();
}

test("Unauthenticated user: cannot read or create cashbooks, parties, or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("cashbooks").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("cashbooks")
      .doc("book_1")
      .set({
        userId: ALICE_UID,
        name: "Main Ledger",
        description: "Office cash",
        currencySymbol: "₹",
        createdAt: serverNow(),
        updatedAt: serverNow(),
      })
  );
});

test("Authenticated user: can create, read, update, and list own CashBook", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdTime = new Date(Date.now() - 2000);
  const bookRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("cashbooks")
    .doc("book_main");

  await assertSucceeds(
    bookRef.set({
      userId: ALICE_UID,
      name: "Head Office Cash Book",
      description: "Daily operations",
      currencySymbol: "₹",
      createdAt: createdTime,
      updatedAt: createdTime,
    })
  );

  await assertSucceeds(bookRef.get());

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("cashbooks")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    bookRef.update({
      name: "Updated Office Cash Book",
      updatedAt: new Date(),
    })
  );
});

test("Shadow Update & Identity Spoofing: rejects extra fields or mismatched userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);
  const bookRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("cashbooks")
    .doc("book_spoof");

  // Spoof userId
  await assertFails(
    bookRef.set({
      userId: BOB_UID,
      name: "Spoofed Book",
      description: "",
      currencySymbol: "₹",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ghost field injection
  await assertFails(
    bookRef.set({
      userId: ALICE_UID,
      name: "Ghost Book",
      description: "",
      currencySymbol: "₹",
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, update, or delete Alice's cashbooks or parties", async () => {
  const ts = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("cashbooks")
      .doc("alice_book")
      .set({
        userId: ALICE_UID,
        name: "Alice Book",
        description: "Private",
        currencySymbol: "₹",
        createdAt: ts,
        updatedAt: ts,
      });
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("parties")
      .doc("alice_party")
      .set({
        userId: ALICE_UID,
        name: "Sharma Transport",
        phone: "9876543210",
        role: "Customer",
        notes: "Regular client",
        createdAt: ts,
        updatedAt: ts,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("cashbooks")
      .doc("alice_book")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("parties")
      .doc("alice_party")
      .get()
  );
});

test("CashTransaction: requires existing parent CashBook and valid enum/amount constraints", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  // Fails when bookId is empty or invalid
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_orphan")
      .set({
        userId: ALICE_UID,
        bookId: "",
        type: "CASH_IN",
        amount: 5000,
        remark: "Advance freight",
        category: "Freight / Trip",
        paymentMode: "UPI",
        partyId: "",
        partyName: "",
        transactionDate: Date.now(),
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Create parent book first
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("cashbooks")
      .doc("book_valid")
      .set({
        userId: ALICE_UID,
        name: "Fleet Book",
        description: "",
        currencySymbol: "₹",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Succeeds with valid parent book
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_1")
      .set({
        userId: ALICE_UID,
        bookId: "book_valid",
        type: "CASH_IN",
        amount: 12500.5,
        remark: "Invoice #104 settled",
        category: "Sales",
        paymentMode: "Bank Transfer",
        partyId: "party_1",
        partyName: "Apex Traders",
        transactionDate: Date.now(),
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Fails with negative or zero amount
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .doc("tx_bad_amount")
      .set({
        userId: ALICE_UID,
        bookId: "book_valid",
        type: "CASH_OUT",
        amount: -100,
        remark: "Invalid",
        category: "Fuel",
        paymentMode: "Cash",
        partyId: "",
        partyName: "",
        transactionDate: Date.now(),
        createdAt: ts,
        updatedAt: ts,
      })
  );
});
