package com.kitsugi.animelist.data.settings

/**
 * V2-F05 – StartTab
 *
 * Uygulamanın başlangıçta hangi sekmeyi açacağını belirler.
 * MoeList StartTab.kt referans alındı.
 *
 * LAST_USED: Son kullanılan sekme (dinamik)
 * HOME:      Ana ekran
 * MY_LIST:   Anime/manga listesi
 * EXPLORE:   Keşfet
 * PROFILE:   Profil
 */
enum class StartTab(
    val id: String,
    val label: String,
    val emoji: String
) {
    LAST_USED(
        id = "LAST_USED",
        label = "Son Kullanılan",
        emoji = "🕐"
    ),
    HOME(
        id = "HOME",
        label = "Ana Sayfa",
        emoji = "🏠"
    ),
    MY_LIST(
        id = "MY_LIST",
        label = "Listem",
        emoji = "📋"
    ),
    EXPLORE(
        id = "EXPLORE",
        label = "Keşfet",
        emoji = "🔍"
    ),
    PROFILE(
        id = "PROFILE",
        label = "Profil",
        emoji = "👤"
    ),
    MANGA(
        id = "MANGA",
        label = "Manga",
        emoji = "📚"
    );

    companion object {
        fun fromId(id: String): StartTab =
            entries.find { it.id == id } ?: LAST_USED

        fun all(): List<StartTab> = entries.toList()
    }
}
