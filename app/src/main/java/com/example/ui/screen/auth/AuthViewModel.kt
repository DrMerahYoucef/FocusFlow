package com.example.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val auth: FirebaseAuth? = try { Firebase.auth } catch (e: Exception) { null },
    private val db: FirebaseFirestore? = try { Firebase.firestore } catch (e: Exception) { null }
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = if (user != null) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.FocusFlowApplication.instance.sessionRepository.syncWithFirestore()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                AuthState.Authenticated(user)
            } else {
                AuthState.Unauthenticated
            }
        } ?: run {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signIn(email: String, password: String) {
        val currentAuth = auth
        if (currentAuth == null) {
            _authState.value = AuthState.Error("Authentication is not available (Firebase uninitialized)")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                currentAuth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        val currentAuth = auth
        val currentDb = db
        if (currentAuth == null || currentDb == null) {
            _authState.value = AuthState.Error("Registration is not available (Firebase uninitialized)")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = currentAuth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user!!.uid
                currentDb.collection("users").document(uid).set(mapOf(
                    "uid" to uid, 
                    "username" to username, 
                    "email" to email,
                    "treeCount" to 0, 
                    "totalMinutes" to 0,
                    "createdAt" to FieldValue.serverTimestamp(), 
                    "currentRadio" to "",
                    "friends" to emptyList<String>()
                )).await()
                currentDb.collection("leaderboard").document(uid).set(
                    mapOf("uid" to uid, "username" to username, "treeCount" to 0)
                ).await()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            android.util.Log.e("AuthViewModel", "Sign out failed", e)
        }
    }

    fun resetPassword(email: String) {
        val currentAuth = auth
        if (currentAuth == null) return
        viewModelScope.launch {
            try {
                currentAuth.sendPasswordResetEmail(email).await()
            } catch (e: Exception) {
                // Ignore or handle gracefully
            }
        }
    }
}
