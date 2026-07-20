package com.kitsugi.animelist.core.companion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages the lifecycle of a single active TV Companion pairing session.
 *
 * ## Security contract
 * - Only **one** client session is permitted at a time (single-session policy).
 * - Tokens expire after [SESSION_TIMEOUT_MS] milliseconds (default 15 minutes).
 * - A new client authenticating with a fresh token automatically revokes the
 *   previous session (token rotation).
 * - Pending mutation requests (e.g. addon installs) require explicit TV-side
 *   approval before being committed.
 */
class TvCompanionSessionManager {

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        /** Session idle timeout: 15 minutes. */
        const val SESSION_TIMEOUT_MS = 15 * 60 * 1_000L

        /** Pending approval timeout: 2 minutes. */
        const val APPROVAL_TIMEOUT_MS = 2 * 60 * 1_000L
    }

    // ── State ─────────────────────────────────────────────────────────────────

    enum class ConnectionState { IDLE, PENDING_APPROVAL, CONNECTED, EXPIRED }

    data class SessionState(
        val token: String = "",
        val connectionState: ConnectionState = ConnectionState.IDLE,
        val connectedAt: Long = 0L,
        val pendingRequest: PendingRequest? = null
    )

    data class PendingRequest(
        val id: String,
        val action: String,          // e.g. "INSTALL_ADDON", "ADD_REPO"
        val payload: String,         // JSON payload of the mutation
        val proposedAt: Long
    )

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    /** The active pairing token. Replaced on every [rotateToken] call. */
    private val activeToken = AtomicReference(TvCompanionToken.generate())

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns the current pairing token (used to build the companion URL). */
    fun currentToken(): String = activeToken.get()

    /**
     * Rotates the active token, invalidating any existing client session.
     * Call when the QR screen is shown or the user explicitly refreshes.
     */
    fun rotateToken(): String {
        val newToken = TvCompanionToken.generate()
        activeToken.set(newToken)
        _sessionState.update { SessionState(token = newToken) }
        return newToken
    }

    /**
     * Attempts to authenticate an incoming client request with [candidateToken].
     *
     * Returns `true` and transitions to [ConnectionState.CONNECTED] when:
     *  - [candidateToken] matches the current active token (constant-time).
     *  - The session has not expired.
     *
     * Returns `false` otherwise.
     */
    fun authenticate(candidateToken: String): Boolean {
        val current = activeToken.get()
        if (!TvCompanionToken.isValid(candidateToken)) return false
        if (!TvCompanionToken.safeEquals(current, candidateToken)) return false
        _sessionState.update { state ->
            state.copy(
                token = current,
                connectionState = ConnectionState.CONNECTED,
                connectedAt = System.currentTimeMillis()
            )
        }
        return true
    }

    /**
     * Returns `true` when there is an active, non-expired session for [token].
     */
    fun isAuthenticated(token: String): Boolean {
        val state = _sessionState.value
        if (state.connectionState != ConnectionState.CONNECTED) return false
        if (!TvCompanionToken.safeEquals(state.token, token)) return false
        val elapsed = System.currentTimeMillis() - state.connectedAt
        return if (elapsed < SESSION_TIMEOUT_MS) {
            true
        } else {
            expire()
            false
        }
    }

    /**
     * Posts a [PendingRequest] for TV-side approval.
     * Returns `false` if a request is already pending.
     */
    fun requestApproval(id: String, action: String, payload: String): Boolean {
        val state = _sessionState.value
        if (state.pendingRequest != null) return false
        _sessionState.update { s ->
            s.copy(
                connectionState = ConnectionState.PENDING_APPROVAL,
                pendingRequest = PendingRequest(
                    id = id,
                    action = action,
                    payload = payload,
                    proposedAt = System.currentTimeMillis()
                )
            )
        }
        return true
    }

    /**
     * TV user approved the pending request. Returns the approved [PendingRequest]
     * or null if there was nothing pending.
     */
    fun approvePending(): PendingRequest? {
        val pending = _sessionState.value.pendingRequest ?: return null
        _sessionState.update { s ->
            s.copy(connectionState = ConnectionState.CONNECTED, pendingRequest = null)
        }
        return pending
    }

    /**
     * TV user rejected the pending request. Clears the pending state.
     */
    fun rejectPending() {
        _sessionState.update { s ->
            s.copy(connectionState = ConnectionState.CONNECTED, pendingRequest = null)
        }
    }

    /**
     * Marks the session as expired (idle timeout reached).
     */
    fun expire() {
        _sessionState.update { s ->
            s.copy(connectionState = ConnectionState.EXPIRED, pendingRequest = null)
        }
    }

    /**
     * Fully terminates the session and rotates the token (called on sign-out or
     * manual disconnect from TV settings).
     */
    fun disconnect() {
        rotateToken()
    }

    /**
     * Checks and expires the session if it has been idle longer than
     * [SESSION_TIMEOUT_MS]. Safe to call from any coroutine context.
     */
    fun tickExpiry() {
        val state = _sessionState.value
        if (state.connectionState != ConnectionState.CONNECTED) return
        val elapsed = System.currentTimeMillis() - state.connectedAt
        if (elapsed >= SESSION_TIMEOUT_MS) expire()
    }
}
