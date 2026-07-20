package com.kitsugi.animelist.data.manga

import android.content.Context
import com.kitsugi.animelist.data.local.MangaSourceStateEntity
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.data.manga.model.SourceHealthStatus
import com.kitsugi.animelist.data.manga.model.SourceRuntimeStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MangaSourceStateStore(context: Context) {

    private val prefs = context.getSharedPreferences("manga_source_state", Context.MODE_PRIVATE)
    private val dao = KitsugiDatabase.getDatabase(context).mangaSourceStateDao()
    private val configStore = SourceConfigStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getHealthStatus(source: MangaSource): SourceHealthStatus {
        val raw = prefs.getString(key(source, KEY_HEALTH), null) ?: return SourceHealthStatus.Unknown
        return runCatching { SourceHealthStatus.valueOf(raw) }.getOrDefault(SourceHealthStatus.Unknown)
    }

    fun getLastUpdatedAt(source: MangaSource): Long {
        return prefs.getLong(key(source, KEY_UPDATED_AT), 0L)
    }

    fun getLastReason(source: MangaSource): String? {
        return prefs.getString(key(source, KEY_REASON), null)
    }

    fun getLastErrorType(source: MangaSource): String? {
        return prefs.getString(key(source, KEY_ERROR_TYPE), null)
    }

    fun getRuntimeStats(source: MangaSource): SourceRuntimeStats {
        return SourceRuntimeStats(
            successCount = prefs.getInt(key(source, KEY_SUCCESS_COUNT), 0),
            failureCount = prefs.getInt(key(source, KEY_FAILURE_COUNT), 0),
            avgSearchMs = prefs.getLong(key(source, KEY_AVG_SEARCH_MS), 0L),
            avgPopularMs = prefs.getLong(key(source, KEY_AVG_POPULAR_MS), 0L),
            avgDetailsMs = prefs.getLong(key(source, KEY_AVG_DETAILS_MS), 0L),
            avgChapterMs = prefs.getLong(key(source, KEY_AVG_CHAPTER_MS), 0L),
            avgPageMs = prefs.getLong(key(source, KEY_AVG_PAGE_MS), 0L),
            avgImageMs = prefs.getLong(key(source, KEY_AVG_IMAGE_MS), 0L),
        )
    }

    fun getFailureStreak(source: MangaSource): Int {
        return prefs.getInt(key(source, KEY_FAILURE_STREAK), 0)
    }

    fun getCooldownUntil(source: MangaSource): Long {
        return prefs.getLong(key(source, KEY_COOLDOWN_UNTIL), 0L)
    }

    fun isCoolingDown(source: MangaSource): Boolean {
        return getCooldownUntil(source) > System.currentTimeMillis()
    }

    fun setHealthStatus(source: MangaSource, status: SourceHealthStatus, reason: String? = null) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(key(source, KEY_HEALTH), status.name)
            .putLong(key(source, KEY_UPDATED_AT), now)
            .putLong(key(source, KEY_LAST_CHECKED_AT), now)
            .putString(key(source, KEY_REASON), reason)
            .putString(key(source, KEY_ERROR_TYPE), null)
            .apply()
        persistSnapshotAsync(source)
    }

    fun recordOperationSuccess(source: MangaSource, operation: String, elapsedMs: Long) {
        val now = System.currentTimeMillis()
        val currentStats = getRuntimeStats(source)
        val updatedStats = when (operation.lowercase()) {
            OP_SEARCH -> currentStats.copy(avgSearchMs = smoothAverage(currentStats.avgSearchMs, elapsedMs))
            OP_POPULAR -> currentStats.copy(avgPopularMs = smoothAverage(currentStats.avgPopularMs, elapsedMs))
            OP_DETAILS -> currentStats.copy(avgDetailsMs = smoothAverage(currentStats.avgDetailsMs, elapsedMs))
            OP_CHAPTERS -> currentStats.copy(avgChapterMs = smoothAverage(currentStats.avgChapterMs, elapsedMs))
            OP_PAGES -> currentStats.copy(avgPageMs = smoothAverage(currentStats.avgPageMs, elapsedMs))
            OP_IMAGE -> currentStats.copy(avgImageMs = smoothAverage(currentStats.avgImageMs, elapsedMs))
            else -> currentStats
        }

        prefs.edit()
            .putString(key(source, KEY_HEALTH), SourceHealthStatus.Healthy.name)
            .putLong(key(source, KEY_UPDATED_AT), now)
            .putLong(key(source, KEY_LAST_CHECKED_AT), now)
            .putLong(key(source, KEY_LAST_SUCCESS_AT), now)
            .putInt(key(source, KEY_SUCCESS_COUNT), currentStats.successCount + 1)
            .putInt(key(source, KEY_FAILURE_STREAK), 0)
            .putLong(key(source, KEY_COOLDOWN_UNTIL), 0L)
            .putLong(key(source, KEY_AVG_SEARCH_MS), updatedStats.avgSearchMs)
            .putLong(key(source, KEY_AVG_POPULAR_MS), updatedStats.avgPopularMs)
            .putLong(key(source, KEY_AVG_DETAILS_MS), updatedStats.avgDetailsMs)
            .putLong(key(source, KEY_AVG_CHAPTER_MS), updatedStats.avgChapterMs)
            .putLong(key(source, KEY_AVG_PAGE_MS), updatedStats.avgPageMs)
            .putLong(key(source, KEY_AVG_IMAGE_MS), updatedStats.avgImageMs)
            .apply()
        persistSnapshotAsync(source)
    }

    fun recordOperationFailure(
        source: MangaSource,
        operation: String,
        reason: String? = null,
        statusOverride: SourceHealthStatus? = null,
        elapsedMs: Long? = null,
    ) {
        val now = System.currentTimeMillis()
        val currentStats = getRuntimeStats(source)
        val category = SourceFailureClassifier.classifyCategory(
            reason?.let { IllegalStateException(it) }
        )
        val resolvedStatus = statusOverride ?: classifyFailure(reason)
        val nextFailureStreak = getFailureStreak(source) + 1
        val cooldownUntil = computeCooldownUntil(
            status = resolvedStatus,
            failureStreak = nextFailureStreak,
            now = now,
        )
        val updatedStats = when (operation.lowercase()) {
            OP_SEARCH -> currentStats.copy(avgSearchMs = elapsedMs?.let { smoothAverage(currentStats.avgSearchMs, it) } ?: currentStats.avgSearchMs)
            OP_POPULAR -> currentStats.copy(avgPopularMs = elapsedMs?.let { smoothAverage(currentStats.avgPopularMs, it) } ?: currentStats.avgPopularMs)
            OP_DETAILS -> currentStats.copy(avgDetailsMs = elapsedMs?.let { smoothAverage(currentStats.avgDetailsMs, it) } ?: currentStats.avgDetailsMs)
            OP_CHAPTERS -> currentStats.copy(avgChapterMs = elapsedMs?.let { smoothAverage(currentStats.avgChapterMs, it) } ?: currentStats.avgChapterMs)
            OP_PAGES -> currentStats.copy(avgPageMs = elapsedMs?.let { smoothAverage(currentStats.avgPageMs, it) } ?: currentStats.avgPageMs)
            OP_IMAGE -> currentStats.copy(avgImageMs = elapsedMs?.let { smoothAverage(currentStats.avgImageMs, it) } ?: currentStats.avgImageMs)
            else -> currentStats
        }

        prefs.edit()
            .putString(key(source, KEY_HEALTH), resolvedStatus.name)
            .putLong(key(source, KEY_UPDATED_AT), now)
            .putLong(key(source, KEY_LAST_CHECKED_AT), now)
            .putLong(key(source, KEY_LAST_FAILURE_AT), now)
            .putString(key(source, KEY_REASON), reason)
            .putString(key(source, KEY_ERROR_TYPE), category.name)
            .putInt(key(source, KEY_FAILURE_COUNT), currentStats.failureCount + 1)
            .putInt(key(source, KEY_FAILURE_STREAK), nextFailureStreak)
            .putLong(key(source, KEY_COOLDOWN_UNTIL), cooldownUntil)
            .putLong(key(source, KEY_AVG_SEARCH_MS), updatedStats.avgSearchMs)
            .putLong(key(source, KEY_AVG_POPULAR_MS), updatedStats.avgPopularMs)
            .putLong(key(source, KEY_AVG_DETAILS_MS), updatedStats.avgDetailsMs)
            .putLong(key(source, KEY_AVG_CHAPTER_MS), updatedStats.avgChapterMs)
            .putLong(key(source, KEY_AVG_PAGE_MS), updatedStats.avgPageMs)
            .putLong(key(source, KEY_AVG_IMAGE_MS), updatedStats.avgImageMs)
            .apply()
        persistSnapshotAsync(source)
    }

    fun isSearchEligible(source: MangaSource): Boolean {
        if (isCoolingDown(source)) return false
        return when (getHealthStatus(source)) {
            SourceHealthStatus.Disabled -> false
            SourceHealthStatus.Broken -> {
                val updatedAt = getLastUpdatedAt(source)
                updatedAt == 0L || System.currentTimeMillis() - updatedAt > BROKEN_RETRY_COOLDOWN_MS
            }
            else -> true
        }
    }

    fun persistSnapshotAsync(source: MangaSource) {
        scope.launch {
            dao.upsert(buildEntitySnapshot(source))
        }
    }

    fun resetSource(source: MangaSource) {
        prefs.edit()
            .remove(key(source, KEY_HEALTH))
            .remove(key(source, KEY_REASON))
            .remove(key(source, KEY_ERROR_TYPE))
            .remove(key(source, KEY_UPDATED_AT))
            .remove(key(source, KEY_LAST_CHECKED_AT))
            .remove(key(source, KEY_LAST_SUCCESS_AT))
            .remove(key(source, KEY_LAST_FAILURE_AT))
            .remove(key(source, KEY_SUCCESS_COUNT))
            .remove(key(source, KEY_FAILURE_COUNT))
            .remove(key(source, KEY_FAILURE_STREAK))
            .remove(key(source, KEY_COOLDOWN_UNTIL))
            .remove(key(source, KEY_AVG_SEARCH_MS))
            .remove(key(source, KEY_AVG_POPULAR_MS))
            .remove(key(source, KEY_AVG_DETAILS_MS))
            .remove(key(source, KEY_AVG_CHAPTER_MS))
            .remove(key(source, KEY_AVG_PAGE_MS))
            .remove(key(source, KEY_AVG_IMAGE_MS))
            .apply()
        scope.launch {
            dao.deleteByKey(source.stableSourceKey())
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        scope.launch {
            dao.clearAll()
        }
    }

    private fun buildEntitySnapshot(source: MangaSource): MangaSourceStateEntity {
        val stats = getRuntimeStats(source)
        return MangaSourceStateEntity(
            sourceKey = source.stableSourceKey(),
            sourceName = source.name,
            pkgName = source.pkgName,
            lang = source.lang,
            baseUrl = source.baseUrl,
            activeDomain = configStore.getActiveDomain(source),
            healthStatus = getHealthStatus(source).name,
            lastReason = getLastReason(source),
            lastErrorType = getLastErrorType(source),
            lastCheckedAt = prefs.getLong(key(source, KEY_LAST_CHECKED_AT), 0L),
            lastSuccessAt = prefs.getLong(key(source, KEY_LAST_SUCCESS_AT), 0L),
            lastFailureAt = prefs.getLong(key(source, KEY_LAST_FAILURE_AT), 0L),
            successCount = stats.successCount,
            failureCount = stats.failureCount,
            avgSearchMs = stats.avgSearchMs,
            avgPopularMs = stats.avgPopularMs,
            avgDetailsMs = stats.avgDetailsMs,
            avgChapterMs = stats.avgChapterMs,
            avgPageMs = stats.avgPageMs,
            avgImageMs = stats.avgImageMs,
            updatedAt = getLastUpdatedAt(source),
        )
    }

    private fun classifyFailure(reason: String?): SourceHealthStatus {
        val message = reason.orEmpty().lowercase()
        return when {
            message.contains("cloudflare") || message.contains("captcha") -> SourceHealthStatus.CaptchaRequired
            message.contains("429") || message.contains("too many requests") -> SourceHealthStatus.RateLimited
            message.contains("404") || message.contains("not found") || message.contains("unable to resolve host") -> SourceHealthStatus.Broken
            message.isBlank() -> SourceHealthStatus.Degraded
            else -> SourceHealthStatus.Degraded
        }
    }

    private fun smoothAverage(current: Long, sample: Long): Long {
        if (sample <= 0L) return current
        if (current <= 0L) return sample
        return ((current * 3L) + sample) / 4L
    }

    private fun computeCooldownUntil(
        status: SourceHealthStatus,
        failureStreak: Int,
        now: Long,
    ): Long {
        val durationMs = when (status) {
            SourceHealthStatus.RateLimited -> 15L * 60L * 1000L
            SourceHealthStatus.CaptchaRequired -> 30L * 60L * 1000L
            SourceHealthStatus.Broken -> if (failureStreak >= 2) 2L * 60L * 60L * 1000L else 0L
            SourceHealthStatus.Degraded -> if (failureStreak >= 3) 10L * 60L * 1000L else 0L
            else -> 0L
        }
        return if (durationMs > 0L) now + durationMs else 0L
    }

    private fun key(source: MangaSource, suffix: String): String {
        return "${source.stableSourceKey()}::$suffix"
    }

    private companion object {
        const val BROKEN_RETRY_COOLDOWN_MS = 6 * 60 * 60 * 1000L

        const val KEY_HEALTH = "health"
        const val KEY_REASON = "reason"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_UPDATED_AT = "updated_at"
        const val KEY_LAST_CHECKED_AT = "last_checked_at"
        const val KEY_LAST_SUCCESS_AT = "last_success_at"
        const val KEY_LAST_FAILURE_AT = "last_failure_at"
        const val KEY_SUCCESS_COUNT = "success_count"
        const val KEY_FAILURE_COUNT = "failure_count"
        const val KEY_FAILURE_STREAK = "failure_streak"
        const val KEY_COOLDOWN_UNTIL = "cooldown_until"
        const val KEY_AVG_SEARCH_MS = "avg_search_ms"
        const val KEY_AVG_POPULAR_MS = "avg_popular_ms"
        const val KEY_AVG_DETAILS_MS = "avg_details_ms"
        const val KEY_AVG_CHAPTER_MS = "avg_chapter_ms"
        const val KEY_AVG_PAGE_MS = "avg_page_ms"
        const val KEY_AVG_IMAGE_MS = "avg_image_ms"

        const val OP_SEARCH = "search"
        const val OP_POPULAR = "popular"
        const val OP_DETAILS = "details"
        const val OP_CHAPTERS = "chapters"
        const val OP_PAGES = "pages"
        const val OP_IMAGE = "image"
    }
}
