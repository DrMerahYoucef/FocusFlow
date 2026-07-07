package com.example.ui.screen.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid:           String  = "",
    val username:      String  = "",
    val treeCount:     Int     = 0,
    val totalMinutes:  Int     = 0,
    val points:        Int     = 0,
    val currentStreak: Int     = 0,
    val currentRadio:  String  = "",
    val friends:       List<String> = emptyList()
)

data class LeaderboardEntry(
    val uid:           String = "",
    val username:      String = "",
    val treeCount:     Int    = 0,
    val totalMinutes:  Int    = 0,
    val points:        Int    = 0,
    val currentStreak: Int    = 0
)

data class FriendRequest(
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val toName: String = "",
    val status: String = ""
)

class CommunityViewModel : ViewModel() {

    private val firestore: FirebaseFirestore
        get() = Firebase.firestore

    private val auth: FirebaseAuth
        get() = Firebase.auth

    val currentUid: String
        get() = auth.currentUser?.uid ?: ""

    // Helper to pipe Firestore Queries to robust Kotlin Flows
    private fun <T> Query.toFlow(clazz: Class<T>): Flow<List<T>> = callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("CommunityViewModel", "Firestore Query error", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(clazz) }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    val myProfile: StateFlow<UserProfile?> = callbackFlow {
        if (currentUid.isEmpty()) {
            trySend(null)
            awaitClose {}
        } else {
            val listener = firestore.collection("users").document(currentUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("CommunityViewModel", "myProfile error", error)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(snapshot.toObject(UserProfile::class.java))
                    } else {
                        trySend(null)
                    }
                }
            awaitClose { listener.remove() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val leaderboard: StateFlow<List<LeaderboardEntry>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("treeCount", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CommunityViewModel", "Leaderboard users query error", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        LeaderboardEntry(
                            uid           = doc.getString("uid")           ?: "",
                            username      = doc.getString("username")      ?: "",
                            treeCount     = (doc.getLong("treeCount")      ?: 0).toInt(),
                            totalMinutes  = (doc.getLong("totalMinutes")   ?: 0).toInt(),
                            points        = (doc.getLong("points")         ?: 0).toInt(),
                            currentStreak = (doc.getLong("currentStreak")  ?: 0).toInt()
                        )
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val friends: StateFlow<List<UserProfile>> = callbackFlow {
        if (currentUid.isEmpty()) {
            trySend(emptyList())
            awaitClose {}
        } else {
            val listener = firestore.collection("users").document(currentUid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("CommunityViewModel", "Friends query error", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val friendUids = snapshot.get("friends") as? List<*> ?: emptyList<Any>()
                        val stringUids = friendUids.mapNotNull { it?.toString() }
                        if (stringUids.isEmpty()) {
                            trySend(emptyList<UserProfile>())
                        } else {
                            firestore.collection("users").whereIn("uid", stringUids)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    val profiles = querySnapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
                                    trySend(profiles)
                                }
                                .addOnFailureListener { err ->
                                    android.util.Log.e("CommunityViewModel", "Failed to fetch friends details", err)
                                    trySend(emptyList<UserProfile>())
                                }
                        }
                    } else {
                        trySend(emptyList<UserProfile>())
                    }
                }
            awaitClose { listener.remove() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingRequests: StateFlow<List<FriendRequest>> = flow {
        if (currentUid.isEmpty()) {
            emit(emptyList())
        } else {
            emitAll(
                firestore.collection("friend_requests")
                    .whereEqualTo("toUid", currentUid)
                    .whereEqualTo("status", "pending")
                    .toFlow(FriendRequest::class.java)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sentRequests: StateFlow<List<FriendRequest>> = flow {
        if (currentUid.isEmpty()) {
            emit(emptyList())
        } else {
            emitAll(
                firestore.collection("friend_requests")
                    .whereEqualTo("fromUid", currentUid)
                    .whereEqualTo("status", "pending")
                    .toFlow(FriendRequest::class.java)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    fun searchUsers(query: String) {
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val searchQueryPart = query.trim()
                val snap = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", searchQueryPart)
                    .whereLessThanOrEqualTo("username", searchQueryPart + "\uf8ff")
                    .limit(10)
                    .get()
                    .await()
                _searchResults.value = snap.documents
                    .mapNotNull { it.toObject(UserProfile::class.java) }
                    .filter { it.uid != currentUid }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun sendFriendRequest(toUser: UserProfile) {
        if (currentUid.isEmpty() || toUser.uid.isEmpty()) return
        viewModelScope.launch {
            try {
                val myNameSnapshot = firestore.collection("users").document(currentUid).get().await()
                val myName = myNameSnapshot.getString("username") ?: "Someone"
                firestore.collection("friend_requests")
                    .document("${currentUid}_${toUser.uid}")
                    .set(mapOf(
                        "fromUid" to currentUid,
                        "toUid" to toUser.uid,
                        "fromName" to myName,
                        "toName" to toUser.username,
                        "status" to "pending",
                        "createdAt" to FieldValue.serverTimestamp()
                    )).await()
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    fun acceptRequest(request: FriendRequest) {
        if (currentUid.isEmpty() || request.fromUid.isEmpty()) return
        viewModelScope.launch {
            try {
                // Step 1 — Mark request as accepted
                firestore.collection("friend_requests")
                    .document("${request.fromUid}_$currentUid")
                    .update("status", "accepted")
                    .await()
                
                // Step 2 — Add sender (fromUid) to MY (currentUid) friends list
                firestore.collection("users")
                    .document(currentUid)
                    .update("friends", FieldValue.arrayUnion(request.fromUid))
                    .await()
                
                // Step 3 — Add ME (currentUid) to SENDER'S (fromUid) friends list
                firestore.collection("users")
                    .document(request.fromUid)
                    .update("friends", FieldValue.arrayUnion(currentUid))
                    .await()

                // Step 4 — Verify both writes succeeded by reading back
                val myDoc = firestore.collection("users").document(currentUid).get().await()
                val senderDoc = firestore.collection("users").document(request.fromUid).get().await()
                val myFriends = myDoc.get("friends") as? List<*> ?: emptyList<String>()
                val senderFriends = senderDoc.get("friends") as? List<*> ?: emptyList<String>()

                // If either write failed silently, retry it
                if (request.fromUid !in myFriends) {
                    firestore.collection("users").document(currentUid)
                        .update("friends", FieldValue.arrayUnion(request.fromUid)).await()
                }
                if (currentUid !in senderFriends) {
                    firestore.collection("users").document(request.fromUid)
                        .update("friends", FieldValue.arrayUnion(currentUid)).await()
                }
            } catch (e: Exception) {
                android.util.Log.e("FocusIsland", "acceptRequest failed: ${e.message}", e)
            }
        }
    }

    fun declineRequest(request: FriendRequest) {
        if (currentUid.isEmpty() || request.fromUid.isEmpty()) return
        viewModelScope.launch {
            try {
                firestore.collection("friend_requests")
                    .document("${request.fromUid}_$currentUid")
                    .update("status", "declined")
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("FocusIsland", "declineRequest failed: ${e.message}", e)
            }
        }
    }

    fun removeFriend(friend: UserProfile) {
        if (currentUid.isEmpty() || friend.uid.isEmpty()) return
        viewModelScope.launch {
            try {
                val batch = firestore.batch()
                batch.update(
                    firestore.collection("users").document(currentUid),
                    "friends", FieldValue.arrayRemove(friend.uid)
                )
                batch.update(
                    firestore.collection("users").document(friend.uid),
                    "friends", FieldValue.arrayRemove(currentUid)
                )
                batch.commit().await()
            } catch (e: Exception) {
                android.util.Log.e("FocusIsland", "removeFriend failed: ${e.message}", e)
            }
        }
    }

    fun cancelRequest(request: FriendRequest) {
        if (currentUid.isEmpty() || request.toUid.isEmpty()) return
        viewModelScope.launch {
            try {
                firestore.collection("friend_requests")
                    .document("${currentUid}_${request.toUid}")
                    .delete()
                    .await()
            } catch (e: Exception) {
                android.util.Log.e("FocusIsland", "cancelRequest failed: ${e.message}", e)
            }
        }
    }

    fun sendFriendRequestById(toUid: String, toUsername: String) {
        if (currentUid.isEmpty() || toUid.isEmpty()) return
        viewModelScope.launch {
            try {
                val myName = firestore.collection("users").document(currentUid)
                    .get().await().getString("username") ?: ""
                firestore.collection("friend_requests")
                    .document("${currentUid}_${toUid}").set(mapOf(
                        "fromUid" to currentUid,
                        "toUid" to toUid,
                        "fromName" to myName,
                        "toName" to toUsername,
                        "status" to "pending",
                        "createdAt" to FieldValue.serverTimestamp()
                    )).await()
            } catch (e: Exception) {
                android.util.Log.e("FocusIsland", "sendFriendRequestById failed: ${e.message}", e)
            }
        }
    }

    fun syncTreeCount(count: Int) {
        if (currentUid.isEmpty()) return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(currentUid)
                    .update("treeCount", count).await()
                
                firestore.collection("leaderboard").document(currentUid)
                    .update("treeCount", count).await()
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    fun updateCurrentRadio(stationName: String) {
        if (currentUid.isEmpty()) return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(currentUid)
                    .update("currentRadio", stationName).await()
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }
}
