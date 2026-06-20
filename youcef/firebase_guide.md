# 🌲 FocusFlow Firebase Integration Guide

This guide details how **FocusFlow** integrates with **Firebase Services (Auth & Firestore)** to power user authentication, local-to-cloud session synchronization, real-time social dynamics (friends, requests, and current radio state sharing), and a dynamic national leaderboard database.

---

## 📂 Firestore Collections Schema Matrix

```
                     [Firestore Database Root]
                                │
        ┌───────────────────────┴───────────────────────┐
        ▼                                               ▼
┌────────────────┐                              ┌───────────────┐
│     users      │                              │  leaderboard  │
│  (Collection)  │                              │ (Collection)  │
└───────┬────────┘                              └───────┬───────┘
        │                                               │
        ├─► [User Document: {UID}]                      └─► [Leaderboard Entry: {UID}]
        │   ├── uid: "String"                               ├── uid: "String"
        │   ├── username: "String"                          ├── username: "String"
        │   ├── email: "String"                             └── treeCount: Int (completed sessions)
        │   ├── treeCount: Int
        │   ├── currentRadio: "String"
        │   ├── friends: ["UID1", "UID2", ...]
        │   │
        │   └── [sessions] (Subcollection)
        │       └─► [Session Doc: {Timestamp}]
        │           ├── date: Long (epoch ms)
        │           ├── durationSeconds: Int
        │           ├── completed: Boolean
        │           └── focusScore: Int
        │
        ▼
┌────────────────┐
│friend_requests │
│  (Collection)  │
└───────┬────────┘
        │
        └─► [Request Doc: {fromUid}_{toUid}]
            ├── fromUid: "String"
            ├── toUid: "String"
            ├── fromName: "String"
            ├── status: "pending" | "accepted" | "declined"
            └── createdAt: timestamp
```

---

## 🔐 1. Authentication Flow & Account Creation

User authentication is managed via high-level Firebase Authentication coupled with transactional Firestore provisions in `AuthViewModel`.

### 🚀 Sign Up Workflow
Upon new credential registration (`signUp`):
1. **Firebase Auth Creation**: Creates the underlying auth user context securely holding the `email` and `password`.
2. **User Profile Provisioning**: A unique profile document is generated under `/users/{uid}` mapping:
   * `uid`: Unique identifier.
   * `username`: Distinct display handle entered by the user.
   * `email`: Registrant's email address.
   * `treeCount`: Initialized to `0` (represents total completed pomodoro cycles).
   * `totalMinutes`: Initialized to `0`.
   * `createdAt`: Server-side database timestamp (`FieldValue.serverTimestamp()`).
   * `currentRadio`: Initialized to empty (`""`).
   * `friends`: Array of connected user IDs, initialized to an empty list `[]`.
3. **Leaderboard Entry Provisioning**: Generates a concurrent tracking index document under `/leaderboard/{uid}` initialized with `{"uid", "username", "treeCount": 0}`.

### 🔑 Sign In, Sign Out & Reset
* **Sign In (`signIn`)**: Standard credential verification. Automatically kicks off bidirectional session synchronization.
* **Sign Out (`signOut`)**: Flushes local Firebase Auth credentials immediately.
* **Password Reset (`resetPassword`)**: Standard background reset email delivery trigger.

---

## 🔄 2. Dual-Engine Session Synchronization

FocusFlow features an offline-first **Room-to-Firestore** bidirectional sync mechanism embedded inside `SessionRepository.kt`. This ensures user metrics are resilient to connectivity loss and instantly updated across devices.

### 🔁 Bidirectional Sync Mechanism (`syncWithFirestore`)
1. **Read Remote Instances**: Reads all existing files/documents in the user's subcollection under `/users/{uid}/sessions`.
2. **Read Local Instances**: Selects full logs inside the local SQLite/Room database (`sessionDao.getAllSessionsList()`).
3. **Download Downstream Diff**: Any session logs existing remote but not local are synced and inserted into the local Room database.
4. **Upload Upstream Diff**: Any session logs recorded locally (e.g. while offline) are pushed up to Firestore under `/users/{uid}/sessions/{date_timestamp}`.
5. **Aggregate Updates**: Re-calculates total tree count scores immediately through local immediate aggregation (`sessionDao.getCompletedCountImmediate()`). Pushes the up-to-date count directly to:
   * `/users/{uid}/treeCount`
   * `/leaderboard/{uid}/treeCount`
6. **Local Notifications**: Informs app widget receivers such as `ExamCountdownWidgetReceiver` to update home screen statistics with fresh focus parameters.

---

## 🏆 3. Leaderboard Engine

The leaderboard is entirely dynamic and computed automatically using minimal database read amplification:

1. **Ordering & Querying**: Query constraints are optimized using:
   ```kotlin
   firestore.collection("leaderboard")
       .orderBy("treeCount", Query.Direction.DESCENDING)
       .limit(50)
   ```
2. **Self-Correcting Scores**: Users do not modify or post arbitrary scores. Upon completing a Pomodoro session, the background service triggers `syncWithFirestore()` or `TimerViewModel` directly modifies `treeCount` to represent historical completed sessions validated locally against the Room container.

---

## 🤝 4. Friends & Social Connection Lifecycle

Social interactions under the Community tab employ a highly decoupled design pattern via the transactional `friend_requests` collection and array union routines in `CommunityViewModel.kt`.

### 📩 Sending a Friend Request
When user `A` sends a request to user `B`:
* Creates/Overwrites a document at `/friend_requests/A_B`:
  ```json
  {
    "fromUid": "A_UID",
    "toUid": "B_UID",
    "fromName": "A_Username",
    "status": "pending",
    "createdAt": "server_timestamp"
  }
  ```

### ✅ Accepting a Request
When user `B` accepts `A`'s request:
1. **Request status transition**: `/friend_requests/A_B` status updates to `"accepted"`.
2. **Mutual Array Union**:
   * Atomically appends `B_UID` into `/users/A_UID/friends` list via `FieldValue.arrayUnion()`.
   * Atomically appends `A_UID` into `/users/B_UID/friends` list via `FieldValue.arrayUnion()`.

### ❌ Declining a Request
* `/friend_requests/A_B` status simply updates to `"declined"`, preventing repeated spam.

### 🗑️ Removing a Friend
When mutual links are broken:
* Atomically triggers `FieldValue.arrayRemove()` for both users:
  * Removes `B_UID` from `/users/A_UID/friends`.
  * Removes `A_UID` from `/users/B_UID/friends`.

---

## 📻 5. Radio Synchronization (`currentRadio`)

An elegant feature that shares other active users' real-time studying focus track:
* Whenever the user chooses or joins a streaming radio station inside the Pomodoro screen, `updateCurrentRadio("stationName")` is triggered.
* Writes directly to the field `/users/{uid}/currentRadio` on Firestore.
* Because profiles under `friends` are active live snapshot flow streams, friends can instantly view what radio ambient channel each focus partner is presently listening to, creating a shared and connected auditory focus space.
