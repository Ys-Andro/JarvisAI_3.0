const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

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

// --- Tests ---
test("Unauthenticated user: cannot read todo lists", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("todo_lists").get());
});

test("Authenticated user: can create shared todo list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("todo_lists").doc("list_1").set({
      title: "Team Project",
      description: "Sprint tasks",
      ownerId: ALICE_UID,
      ownerEmail: "alice@example.com",
      memberUids: [ALICE_UID, BOB_UID],
      colorHex: "#4285F4",
      totalTasks: 0,
      completedTasks: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Member can read shared todo list", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("todo_lists").doc("list_1").set({
      title: "Shared List",
      ownerId: ALICE_UID,
      ownerEmail: "alice@example.com",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("todo_lists").doc("list_1").get());

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(charlieDb.collection("todo_lists").doc("list_1").get());
});

test("List query: member can list lists they belong to", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("todo_lists").doc("list_1").set({
      title: "Alice & Bob List",
      ownerId: ALICE_UID,
      ownerEmail: "alice@example.com",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("todo_lists").where("memberUids", "array-contains", BOB_UID).get()
  );
});

test("Task operations: member can add task and update task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("todo_lists").doc("list_1").set({
      title: "Alice & Bob List",
      ownerId: ALICE_UID,
      ownerEmail: "alice@example.com",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("todo_lists").doc("list_1").collection("tasks").doc("task_1").set({
      listId: "list_1",
      title: "Finish UI design",
      description: "Compose screens",
      completed: false,
      priority: "HIGH",
      createdByUid: BOB_UID,
      createdByName: "Bob",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Bob updates completion status
  await assertSucceeds(
    bobDb.collection("todo_lists").doc("list_1").collection("tasks").doc("task_1").update({
      completed: true,
      updatedAt: new Date(),
    })
  );

  // Non-member Charlie cannot read or write tasks
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("todo_lists").doc("list_1").collection("tasks").doc("task_1").get()
  );
});

test("Security: Non-member cannot create tasks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("todo_lists").doc("list_secure").set({
      title: "Private Project",
      ownerId: ALICE_UID,
      ownerEmail: "alice@example.com",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertFails(
    charlieDb.collection("todo_lists").doc("list_secure").collection("tasks").doc("task_charlie").set({
      listId: "list_secure",
      title: "Unauthorized Task",
      completed: false,
      priority: "MEDIUM",
      createdByUid: CHARLIE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    })
  );
});

test("Security: Member cannot modify memberUids, but Owner can", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("todo_lists").doc("list_collab").set({
      title: "Collab Project",
      ownerId: ALICE_UID,
      ownerEmail: "alice@example.com",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Member Bob cannot add Charlie to memberUids
  await assertFails(
    bobDb.collection("todo_lists").doc("list_collab").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Owner Alice CAN add Charlie to memberUids
  await assertSucceeds(
    aliceDb.collection("todo_lists").doc("list_collab").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Member Charlie can remove themselves (self-removal)
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  await assertSucceeds(
    charlieDb.collection("todo_lists").doc("list_collab").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: new Date(),
    })
  );
});
