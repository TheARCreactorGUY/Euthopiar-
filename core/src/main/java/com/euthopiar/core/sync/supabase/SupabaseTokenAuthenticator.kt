package com.euthopiar.core.sync.supabase

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class SupabaseTokenAuthenticator(
    private val authClient: SupabaseAuthClient,
    private val sessionProvider: () -> SupabaseSession?,
    private val onSessionRefreshed: (SupabaseSession) -> Unit,
    private val onSessionExpired: () -> Unit
) : Authenticator {

    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if retry fails
        if (responseCount(response) >= 2) {
            return null
        }

        val originalRequest = response.request
        val authHeader = originalRequest.header("Authorization") ?: ""
        val currentSession = sessionProvider() ?: return null

        // If the token used in the request is already different from currentSession, someone already refreshed it
        val bearerToken = if (authHeader.startsWith("Bearer ")) authHeader.substring(7) else ""
        if (bearerToken.isNotEmpty() && bearerToken != currentSession.accessToken) {
            return originalRequest.newBuilder()
                .header("Authorization", "Bearer ${currentSession.accessToken}")
                .build()
        }

        // Single-flight refresh protected by Mutex
        val newSession = runBlocking {
            refreshMutex.withLock {
                val latestSession = sessionProvider()
                if (latestSession != null && latestSession.accessToken != bearerToken && !isTokenExpiredOrNear(latestSession)) {
                    return@withLock latestSession
                }

                val refreshResult = authClient.refreshSession(latestSession?.refreshToken ?: currentSession.refreshToken)
                if (refreshResult.isSuccess) {
                    val refreshed = refreshResult.getOrThrow()
                    onSessionRefreshed(refreshed)
                    refreshed
                } else {
                    val errMsg = refreshResult.exceptionOrNull()?.message ?: ""
                    if (isPermanentAuthRejection(errMsg)) {
                        onSessionExpired()
                    }
                    null
                }
            }
        } ?: return null

        return originalRequest.newBuilder()
            .header("Authorization", "Bearer ${newSession.accessToken}")
            .build()
    }

    suspend fun getValidAccessToken(): String? {
        val session = sessionProvider() ?: return null
        if (!isTokenExpiredOrNear(session)) {
            return session.accessToken
        }

        return refreshMutex.withLock {
            val freshSession = sessionProvider() ?: return@withLock null
            if (!isTokenExpiredOrNear(freshSession)) {
                return@withLock freshSession.accessToken
            }
            val res = authClient.refreshSession(freshSession.refreshToken)
            if (res.isSuccess) {
                val updated = res.getOrThrow()
                onSessionRefreshed(updated)
                updated.accessToken
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: ""
                if (isPermanentAuthRejection(errMsg)) {
                    onSessionExpired()
                }
                null
            }
        }
    }

    private fun isPermanentAuthRejection(errorMessage: String): Boolean {
        return errorMessage.contains("HTTP 400") ||
            errorMessage.contains("HTTP 401") ||
            errorMessage.contains("HTTP 403") ||
            errorMessage.contains("invalid_grant", ignoreCase = true) ||
            errorMessage.contains("refresh_token_not_found", ignoreCase = true)
    }

    private fun isTokenExpiredOrNear(session: SupabaseSession, marginSeconds: Long = 120): Boolean {
        val now = System.currentTimeMillis() + authClient.serverClockSkewMs
        val marginMs = marginSeconds * 1000L
        return now >= (session.expiresAtMs - marginMs)
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
