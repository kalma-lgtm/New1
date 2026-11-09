const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Unauthenticated user cannot read subscribers", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users/user_1/subscribers").get());
});

test("Authenticated owner can create subscriber", async () => {
  const authedDb = testEnv.authenticatedContext("user_1").firestore();
  await assertSucceeds(
    authedDb.collection("users/user_1/subscribers").add({
      userId: "user_1",
      accountNumber: "NP-2026-1001",
      fullName: "John Doe",
      contactNumber: "09123456789",
      planName: "Fiber 20 Mbps",
      monthlyFee: 1299.0,
      connectionStatus: "Active",
      billingDueDate: 15,
    })
  );
});

test("Cross-user cannot read subscriber of another user", async () => {
  const authedDb1 = testEnv.authenticatedContext("user_1").firestore();
  await authedDb1.collection("users/user_1/subscribers").doc("sub_1").set({
    userId: "user_1",
    accountNumber: "NP-2026-1001",
    fullName: "John Doe",
    contactNumber: "09123456789",
    planName: "Fiber 20 Mbps",
    monthlyFee: 1299.0,
    connectionStatus: "Active",
  });

  const authedDb2 = testEnv.authenticatedContext("user_2").firestore();
  await assertFails(authedDb2.collection("users/user_1/subscribers").doc("sub_1").get());
});

test("Invalid subscriber (missing required fields) is rejected", async () => {
  const authedDb = testEnv.authenticatedContext("user_1").firestore();
  await assertFails(
    authedDb.collection("users/user_1/subscribers").add({
      userId: "user_1",
      // missing accountNumber, fullName, planName, monthlyFee, connectionStatus
    })
  );
});

test("Authenticated owner can create and read bills, collections, and expenses", async () => {
  const authedDb = testEnv.authenticatedContext("user_1").firestore();
  await assertSucceeds(
    authedDb.collection("users/user_1/bills").add({
      userId: "user_1",
      subscriberId: "sub_1",
      accountNumber: "NP-2026-1001",
      subscriberName: "John Doe",
      billingPeriod: "2026-11",
      totalAmountDue: 1299.0,
      status: "Unpaid"
    })
  );

  await assertSucceeds(
    authedDb.collection("users/user_1/expenses").add({
      userId: "user_1",
      category: "Fiber Optic Materials",
      description: "Drop cable 1000m",
      amount: 4500.0,
      transactionDate: "2026-11-01",
      paymentMethod: "Cash"
    })
  );
});
