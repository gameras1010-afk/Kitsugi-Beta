package com.kitsugi.animelist.data.settings

/**
 * V2-F05 – TabletMode
 *
 * Tablet (geniş ekran) düzeni modunu belirler.
 * MoeList TabletMode.kt referans alındı.
 *
 * AUTO:    Ekran genişliği > 600dp ise tablet düzeni aktif
 * ENABLED: Her zaman tablet düzeni kullan (bölünmüş panel)
 * DISABLED: Her zaman telefon düzeni kullan (tek panel)
 */
enum class TabletMode(
    val id: String,
    val label: String,
    val description: String
) {
    AUTO(
        id = "AUTO",
        label = "Otomatik",
        description = "Ekran genişliğine göre karar verilir (≥600dp = tablet)"
    ),
    ENABLED(
        id = "ENABLED",
        label = "Her Zaman Tablet",
        description = "İki sütunlu bölünmüş panel düzeni zorunlu kılınır"
    ),
    DISABLED(
        id = "DISABLED",
        label = "Her Zaman Telefon",
        description = "Tek sütunlu telefon düzeni zorunlu kılınır"
    );

    companion object {
        fun fromId(id: String): TabletMode =
            entries.find { it.id == id } ?: AUTO

        fun all(): List<TabletMode> = entries.toList()
    }
}

/**
 * Yardımcı extension — verilen ekran genişliğinde tablet modunun aktif olup olmadığını döner.
 * @param screenWidthDp Cihazın mevcut ekran genişliği (dp cinsinden)
 */
fun TabletMode.isTabletActive(screenWidthDp: Int): Boolean = when (this) {
    TabletMode.AUTO     -> screenWidthDp >= 600
    TabletMode.ENABLED  -> true
    TabletMode.DISABLED -> false
}
