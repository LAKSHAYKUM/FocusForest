package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

data class AuthUserState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FirebaseAuthManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val _authState = MutableStateFlow(AuthUserState(user = auth.currentUser))
    val authState: StateFlow<AuthUserState> = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = _authState.value.copy(
                user = firebaseAuth.currentUser,
                isLoading = false
            )
        }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Sign in anonymously (offline or quick guest sync)
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result: AuthResult = auth.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("Anonymous sign in returned null user")
            _authState.value = AuthUserState(user = user, isLoading = false, errorMessage = null)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            Result.failure(e)
        }
    }

    /**
     * Sign in with Email and Password
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw IllegalStateException("Email sign in returned null user")
            _authState.value = AuthUserState(user = user, isLoading = false, errorMessage = null)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            Result.failure(e)
        }
    }

    /**
     * Register with Email and Password
     */
    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw IllegalStateException("Sign up returned null user")
            _authState.value = AuthUserState(user = user, isLoading = false, errorMessage = null)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
            Result.failure(e)
        }
    }

    /**
     * Google Sign-In using Android CredentialManager and GoogleIdTokenCredential
     */
    suspend fun signInWithGoogle(context: Context, serverClientId: String): Result<FirebaseUser> {
        return try {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val credentialManager = CredentialManager.create(context)

            // Generate a random nonce for replay attack prevention
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase returned null user after Google sign-in")
                _authState.value = AuthUserState(user = user, isLoading = false, errorMessage = null)
                Result.success(user)
            } else {
                throw IllegalStateException("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GoogleIdTokenParsingException) {
            _authState.value = _authState.value.copy(isLoading = false, errorMessage = "Google ID token parsing error")
            Result.failure(e)
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Google Sign-In failed")
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        auth.signOut()
        _authState.value = AuthUserState(user = null, isLoading = false, errorMessage = null)
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
}
