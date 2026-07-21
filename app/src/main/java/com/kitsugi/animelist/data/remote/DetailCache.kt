package com.kitsugi.animelist.data.remote

import java.util.concurrent.ConcurrentHashMap

object DetailCache {
    private val mediaDetails = ConcurrentHashMap<String, KitsugiMediaDetail>()
    private val mediaCharacters = ConcurrentHashMap<String, List<KitsugiCharacter>>()
    private val mediaStaff = ConcurrentHashMap<String, List<KitsugiStaff>>()
    private val mediaRelations = ConcurrentHashMap<String, List<KitsugiRelation>>()
    private val mediaStats = ConcurrentHashMap<String, KitsugiStats>()
    private val mediaReviews = ConcurrentHashMap<String, List<KitsugiReview>>()
    private val mediaEpisodes = ConcurrentHashMap<String, List<KitsugiStreamingEpisode>>()
    private val mediaRecommendations = ConcurrentHashMap<String, List<KitsugiRelation>>()

    private val characterDetails = ConcurrentHashMap<String, KitsugiCharacterDetail>()
    private val staffDetails = ConcurrentHashMap<String, KitsugiStaffDetail>()
    private val studioDetails = ConcurrentHashMap<String, KitsugiStudioDetail>()

    private val translations = ConcurrentHashMap<String, String>()

    private fun makeKey(source: String, id: Int): String {
        return "${source.lowercase()}_$id"
    }

    // Media Details
    fun getMediaDetail(source: String, id: Int): KitsugiMediaDetail? {
        return mediaDetails[makeKey(source, id)]
    }

    fun putMediaDetail(source: String, id: Int, detail: KitsugiMediaDetail) {
        mediaDetails[makeKey(source, id)] = detail
    }

    fun removeMediaDetail(source: String, id: Int) {
        mediaDetails.remove(makeKey(source, id))
    }

    // Characters Tab
    fun getMediaCharacters(source: String, id: Int): List<KitsugiCharacter>? {
        return mediaCharacters[makeKey(source, id)]
    }

    fun putMediaCharacters(source: String, id: Int, list: List<KitsugiCharacter>) {
        mediaCharacters[makeKey(source, id)] = list
    }

    // Staff Tab
    fun getMediaStaff(source: String, id: Int): List<KitsugiStaff>? {
        return mediaStaff[makeKey(source, id)]
    }

    fun putMediaStaff(source: String, id: Int, list: List<KitsugiStaff>) {
        mediaStaff[makeKey(source, id)] = list
    }

    // Relations Tab
    fun getMediaRelations(source: String, id: Int): List<KitsugiRelation>? {
        return mediaRelations[makeKey(source, id)]
    }

    fun putMediaRelations(source: String, id: Int, list: List<KitsugiRelation>) {
        mediaRelations[makeKey(source, id)] = list
    }

    // Stats Tab
    fun getMediaStats(source: String, id: Int): KitsugiStats? {
        return mediaStats[makeKey(source, id)]
    }

    fun putMediaStats(source: String, id: Int, stats: KitsugiStats) {
        mediaStats[makeKey(source, id)] = stats
    }

    fun hasMediaStats(source: String, id: Int): Boolean {
        return mediaStats.containsKey(makeKey(source, id))
    }

    // Reviews Tab
    fun getMediaReviews(source: String, id: Int): List<KitsugiReview>? {
        return mediaReviews[makeKey(source, id)]
    }

    fun putMediaReviews(source: String, id: Int, list: List<KitsugiReview>) {
        mediaReviews[makeKey(source, id)] = list
    }

    fun removeMediaReviews(source: String, id: Int) {
        mediaReviews.remove(makeKey(source, id))
    }

    // Episodes Tab
    fun getMediaEpisodes(source: String, id: Int): List<KitsugiStreamingEpisode>? {
        return mediaEpisodes[makeKey(source, id)]
    }

    fun putMediaEpisodes(source: String, id: Int, list: List<KitsugiStreamingEpisode>) {
        mediaEpisodes[makeKey(source, id)] = list
    }

    fun removeMediaEpisodes(source: String, id: Int) {
        mediaEpisodes.remove(makeKey(source, id))
    }

    // Recommendations Tab
    fun getMediaRecommendations(source: String, id: Int): List<KitsugiRelation>? {
        return mediaRecommendations[makeKey(source, id)]
    }

    fun putMediaRecommendations(source: String, id: Int, list: List<KitsugiRelation>) {
        mediaRecommendations[makeKey(source, id)] = list
    }

    fun removeMediaRecommendations(source: String, id: Int) {
        mediaRecommendations.remove(makeKey(source, id))
    }

    fun removeMediaCharacters(source: String, id: Int) {
        mediaCharacters.remove(makeKey(source, id))
    }

    fun removeMediaStaff(source: String, id: Int) {
        mediaStaff.remove(makeKey(source, id))
    }

    fun removeMediaRelations(source: String, id: Int) {
        mediaRelations.remove(makeKey(source, id))
    }

    // Character Detail
    fun getCharacterDetail(source: String, id: Int): KitsugiCharacterDetail? {
        return characterDetails[makeKey(source, id)]
    }

    fun putCharacterDetail(source: String, id: Int, detail: KitsugiCharacterDetail) {
        characterDetails[makeKey(source, id)] = detail
    }

    // Staff Detail
    fun getStaffDetail(source: String, id: Int): KitsugiStaffDetail? {
        return staffDetails[makeKey(source, id)]
    }

    fun putStaffDetail(source: String, id: Int, detail: KitsugiStaffDetail) {
        staffDetails[makeKey(source, id)] = detail
    }

    // Studio Detail
    fun getStudioDetail(source: String, id: Int): KitsugiStudioDetail? {
        return studioDetails[makeKey(source, id)]
    }

    fun putStudioDetail(source: String, id: Int, detail: KitsugiStudioDetail) {
        studioDetails[makeKey(source, id)] = detail
    }

    // Translations (Synopsis / Biography)
    fun getTranslation(type: String, source: String, id: Int): String? {
        val key = "${type.lowercase()}_${makeKey(source, id)}"
        return translations[key]
    }

    fun putTranslation(type: String, source: String, id: Int, translation: String) {
        val key = "${type.lowercase()}_${makeKey(source, id)}"
        translations[key] = translation
    }

    // Clear all
    fun clear() {
        mediaDetails.clear()
        mediaCharacters.clear()
        mediaStaff.clear()
        mediaRelations.clear()
        mediaStats.clear()
        mediaReviews.clear()
        mediaEpisodes.clear()
        mediaRecommendations.clear()
        characterDetails.clear()
        staffDetails.clear()
        studioDetails.clear()
        translations.clear()
    }
}
