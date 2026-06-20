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
    val uid: String = "",
    val username: String = "",
    val treeCount: Int = 0,
    val currentRadio: String = "",
    val friends: List<String> = emptyList()
)

data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val treeCount: Int = 0
)

data class FriendRequest(
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val status: String = ""
)

class CommunityViewModel(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

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

    val leaderboard: StateFlow<List<LeaderboardEntry>> = firestore.collection("leaderboard")
        .orderBy("treeCount", Query.Direction.DESCENDING)
        .limit(50)
        .toFlow(LeaderboardEntry::class.java)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
                firestore.collection("friend_requests")
                    .document("${request.fromUid}_$currentUid")
                    .update("status", "accepted").await()
                
                firestore.collection("users").document(currentUid)
                    .update("friends", FieldValue.arrayUnion(request.fromUid)).await()
                
                firestore.collection("users").document(request.fromUid)
                    .update("friends", FieldValue.arrayUnion(currentUid)).await()
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    fun declineRequest(request: FriendRequest) {
        if (currentUid.isEmpty() || request.fromUid.isEmpty()) return
        viewModelScope.launch {
            try {
                firestore.collection("friend_requests")
                    .document("${request.fromUid}_$currentUid")
                    .update("status", "declined").await()
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    fun removeFriend(friend: UserProfile) {
        if (currentUid.isEmpty() || friend.uid.isEmpty()) return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(currentUid)
                    .update("friends", FieldValue.arrayRemove(friend.uid)).await()
                
                firestore.collection("users").document(friend.uid)
                    .update("friends", FieldValue.arrayRemove(currentUid)).await()
            } catch (e: Exception) {
                // Handle or log error
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
