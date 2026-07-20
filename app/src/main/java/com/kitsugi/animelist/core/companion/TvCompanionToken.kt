package com.kitsugi.animelist.core.companion

import java.security.SecureRandom

/**
 * Cryptographically-secure token helpers for the TV Companion pairing flow.
 *
 * Tokens are 32-byte (256-bit) random values encoded as lowercase hex strings,
 * yielding a 64-character access token that is embedded in every companion URL
 * (e.g. `http://192.168.1.42:8080/companion?token=<TOKEN>`).
 */
object TvCompanionToken {

    private val random = SecureRandom()

    /** Byte-length of each generated token (256 bits). */
    private const val TOKEN_BYTES = 32

    /** Regex that a well-formed hex token must satisfy. */
    private val VALID_PATTERN = Regex("^[0-9a-f]{${TOKEN_BYTES * 2}}$")

    /**
     * Generates a new cryptographically-random token.
     * @return 64-character lowercase hex string.
     */
    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES).also { random.nextBytes(it) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Returns true when [candidate] is a structurally valid companion token
     * (correct length, hex characters only). Does NOT verify active session.
     */
    fun isValid(candidate: String): Boolean =
        VALID_PATTERN.matches(candidate)

    /**
     * Constant-time comparison of two token strings to mitigate timing attacks.
     */
    fun safeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }
}
