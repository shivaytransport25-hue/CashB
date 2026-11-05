const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Validates and atomically updates account balances when a transaction is written to Firestore.
 * Prevents race conditions and guarantees server-side financial consistency.
 */
exports.onTransactionCreated = functions.firestore
  .document("businesses/{businessId}/transactions/{transactionId}")
  .onCreate(async (snap, context) => {
    const { businessId, transactionId } = context.params;
    const txn = snap.data();

    if (!txn || txn.status !== "posted") return null;

    const accountRef = db
      .collection("businesses")
      .doc(businessId)
      .collection("accounts")
      .doc(txn.accountId);

    return db.runTransaction(async (t) => {
      const accDoc = await t.get(accountRef);
      if (!accDoc.exists) {
        throw new Error(`Account ${txn.accountId} does not exist`);
      }

      const accData = accDoc.data();
      let currentBal = accData.currentBalancePaise || 0;
      const amount = txn.amountPaise || 0;

      if (txn.transactionType === "cashReceived" || txn.transactionType === "bankReceived") {
        currentBal += amount;
      } else if (txn.transactionType === "cashPaid" || txn.transactionType === "bankPayment") {
        if (!accData.allowNegativeBalance && (currentBal - amount) < 0) {
          throw new Error(`Transaction violates non-negative balance rule for account ${accData.accountName}`);
        }
        currentBal -= amount;
      } else if (txn.transactionType === "transfer") {
        // Source leg of transfer
        currentBal -= amount;
      }

      t.update(accountRef, {
        currentBalancePaise: currentBal,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Write Audit Log Entry
      const auditRef = db
        .collection("businesses")
        .doc(businessId)
        .collection("audit_logs")
        .doc();

      t.set(auditRef, {
        action: "SERVER_CONFIRMED_POST",
        entityType: "transaction",
        entityId: transactionId,
        voucherNumber: txn.voucherNumber,
        amountPaise: txn.amountPaise,
        performedBy: txn.createdBy,
        performedByName: txn.createdByName,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });
  });

/**
 * Handles atomic server-side account-to-account transfers.
 */
exports.onTransferCreated = functions.firestore
  .document("businesses/{businessId}/transfers/{transferId}")
  .onCreate(async (snap, context) => {
    const { businessId, transferId } = context.params;
    const transfer = snap.data();

    const sourceRef = db.collection("businesses").doc(businessId).collection("accounts").doc(transfer.sourceAccountId);
    const targetRef = db.collection("businesses").doc(businessId).collection("accounts").doc(transfer.targetAccountId);

    return db.runTransaction(async (t) => {
      const sourceDoc = await t.get(sourceRef);
      const targetDoc = await t.get(targetRef);

      if (!sourceDoc.exists || !targetDoc.exists) {
        throw new Error("One or both transfer accounts do not exist");
      }

      const sourceBal = (sourceDoc.data().currentBalancePaise || 0) - transfer.amountPaise;
      const targetBal = (targetDoc.data().currentBalancePaise || 0) + transfer.amountPaise;

      t.update(sourceRef, { currentBalancePaise: sourceBal });
      t.update(targetRef, { currentBalancePaise: targetBal });
    });
  });

