package com.kitsugi.animelist.core.player

/**
 * T1.7: PostPlayMode
 *
 * Bir bölüm bittikten sonra oynatıcının ne yapacağını tanımlar.
 * MoeList / NuvioTV PostPlayMode referansından adapte edildi.
 */
enum class PostPlayMode(val displayName: String) {
    /** Kullanıcı Manuel atlar, oynatıcı bekler */
    MANUAL("Elle Geç"),

    /** Geri sayım sonrası otomatik sonraki bölüm */
    AUTO_PLAY_NEXT("Otomatik Oynat"),

    /**
     * Binge (dizi izleme) modu: belirli sayıda bölümden sonra
     * "Devam edilsin mi?" sorusu gelir (PlayerStillWatchingController ile birlikte çalışır).
     */
    BINGE_PROMPT("Binge Modu"),

    /**
     * Bölüm sonunda bir özet / mini sonuç ekranı gösterir,
     * ardından kullanıcı etkileşimi bekler.
     */
    END_PROMPT("Son Özeti Göster"),

    /** Aynı bölümü baştan döngüye alır */
    LOOP("Döngü");

    companion object {
        fun fromOrdinal(ordinal: Int): PostPlayMode =
            entries.getOrElse(ordinal) { AUTO_PLAY_NEXT }

        fun fromString(value: String?): PostPlayMode =
            entries.firstOrNull { it.name == value } ?: AUTO_PLAY_NEXT
    }
}
