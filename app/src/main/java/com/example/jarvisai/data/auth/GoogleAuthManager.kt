package com.example.jarvisai.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) {

    private fun getWebClientId(): String {
        return context.getString(R.string.default_web_client_id)
    }

    suspend fun attemptAutoSignIn(): Boolean {
        if (auth.currentUser != null) return true
        val clientId = try {
            getWebClientId()
        } catch (e: Exception) {
            Log.e("Auth", "Failed to get web client ID: ${e.message}")
            return false
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            handleCredential(result.credential)
            true
        } catch (e: Exception) {
            Log.d("Auth", "Silent auto-sign-in skipped/failed: ${e.message}")
            false
        }
    }

    suspend fun signInWithGoogle(): Result<Unit> {
        val clientId = try {
            getWebClientId()
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Web client ID no configurado: ${e.message}"))
        }

        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            handleCredential(result.credential)
            Result.success(Unit)
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelado por el usuario: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("Auth", "Error durante Google Sign-In", e)
            Result.failure(e)
        }
    }

    private suspend fun handleCredential(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
        } else {
            throw IllegalArgumentException("Tipo de credencial no reconocido: ${credential.type}")
        }
    }

    suspend fun signOut() {
        auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Error al limpiar credenciales: ${e.message}", e)
        }
    }
}
