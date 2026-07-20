package com.kitsugi.animelist.core.security

import android.content.Context

object RepoVerifier {
    private val trustedDomains = listOf(
        "github.com",
        "githubusercontent.com",
        "raw.githubusercontent.com",
        "keiyoushi.github.io",
        "kotatsu.app",
        "strem.fun",
        "stremio.com",
        "kitsugi.app"
    )

    fun isUrlTrusted(context: Context, url: String): Boolean {
        val host = try {
            android.net.Uri.parse(url).host
        } catch (_: Exception) { null } ?: return false

        // Check if host matches any trusted domains
        if (trustedDomains.any { host.equals(it, ignoreCase = true) || host.endsWith(".$it", ignoreCase = true) }) {
            return true
        }

        // Check if host is in user-trusted repositories list
        val prefs = context.getSharedPreferences("security_trust", Context.MODE_PRIVATE)
        val userTrusted = prefs.getStringSet("trusted_repos", emptySet()) ?: emptySet()
        return userTrusted.contains(host.lowercase())
    }

    fun trustRepo(context: Context, url: String) {
        val host = try {
            android.net.Uri.parse(url).host
        } catch (_: Exception) { null } ?: return
        val prefs = context.getSharedPreferences("security_trust", Context.MODE_PRIVATE)
        val userTrusted = (prefs.getStringSet("trusted_repos", emptySet()) ?: emptySet()).toMutableSet()
        userTrusted.add(host.lowercase())
        prefs.edit().putStringSet("trusted_repos", userTrusted).apply()
    }
}
