package com.kitsugi.animelist.data.manga

import android.content.Context
import android.net.Uri
import java.util.Locale

class SourceConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences("manga_source_config", Context.MODE_PRIVATE)

    fun getActiveDomain(source: MangaSource): String? {
        return prefs.getString(key(source, KEY_ACTIVE_DOMAIN), null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun setActiveDomain(source: MangaSource, domain: String?) {
        prefs.edit().apply {
            if (domain.isNullOrBlank()) {
                remove(key(source, KEY_ACTIVE_DOMAIN))
            } else {
                putString(key(source, KEY_ACTIVE_DOMAIN), sanitizeDomain(domain))
            }
        }.apply()
    }

    fun setActiveDomainValidated(source: MangaSource, domain: String?): Boolean {
        if (domain.isNullOrBlank()) {
            setActiveDomain(source, null)
            return true
        }
        val normalized = validateDomain(domain) ?: return false
        setActiveDomain(source, normalized)
        return true
    }

    fun getPreferredBaseUrl(source: MangaSource): String? {
        return getPreferredBaseUrl(source, source.baseUrl)
    }

    fun getPreferredBaseUrl(source: MangaSource, rawBaseUrl: String): String? {
        val configured = getActiveDomain(source) ?: return null
        val raw = rawBaseUrl.trim()
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return "https://$configured"
        val scheme = uri.scheme?.takeIf { it.isNotBlank() } ?: "https"
        return "$scheme://$configured"
    }

    fun getSlowdownEnabled(source: MangaSource): Boolean {
        return prefs.getBoolean(key(source, KEY_SLOWDOWN_ENABLED), false)
    }

    fun setSlowdownEnabled(source: MangaSource, enabled: Boolean) {
        prefs.edit().putBoolean(key(source, KEY_SLOWDOWN_ENABLED), enabled).apply()
    }

    fun getUserAgentOverride(source: MangaSource): String? {
        return prefs.getString(key(source, KEY_USER_AGENT), null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun setUserAgentOverride(source: MangaSource, value: String?) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) {
                remove(key(source, KEY_USER_AGENT))
            } else {
                putString(key(source, KEY_USER_AGENT), value.trim())
            }
        }.apply()
    }

    fun setUserAgentOverrideValidated(source: MangaSource, value: String?): Boolean {
        if (value.isNullOrBlank()) {
            setUserAgentOverride(source, null)
            return true
        }
        val normalized = value.trim()
        if (!isValidUserAgent(normalized)) return false
        setUserAgentOverride(source, normalized)
        return true
    }

    fun clearAllForSource(source: MangaSource) {
        prefs.edit()
            .remove(key(source, KEY_ACTIVE_DOMAIN))
            .remove(key(source, KEY_SLOWDOWN_ENABLED))
            .remove(key(source, KEY_USER_AGENT))
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun validateDomain(domain: String?): String? {
        if (domain.isNullOrBlank()) return null
        val sanitized = sanitizeDomain(domain)
        val hostRegex = Regex("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$")
        return sanitized.takeIf { hostRegex.matches(it) }
    }

    fun isValidUserAgent(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        val normalized = value.trim()
        return normalized.length in 4..512 && !normalized.contains('\n') && !normalized.contains('\r')
    }

    private fun sanitizeDomain(domain: String): String {
        return domain.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
            .lowercase(Locale.ROOT)
    }

    private fun key(source: MangaSource, suffix: String): String {
        return "${source.stableSourceKey()}::$suffix"
    }

    private companion object {
        const val KEY_ACTIVE_DOMAIN = "active_domain"
        const val KEY_SLOWDOWN_ENABLED = "slowdown_enabled"
        const val KEY_USER_AGENT = "user_agent"
    }
}
