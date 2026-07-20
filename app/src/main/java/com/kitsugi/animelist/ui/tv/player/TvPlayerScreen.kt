package com.kitsugi.animelist.ui.tv.player

import androidx.compose.foundation.background
import androidx.compose.ui.focus.focusTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import com.kitsugi.animelist.core.player.SubtitleInput
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiFullscreenPlayerScreen
import com.kitsugi.animelist.ui.screens.fullscreen.components.MetaCastMember
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames

/**
 * TV-native oynatıcı ekranı.
 *
 * [KitsugiFullscreenPlayerScreen] zaten D-pad key handling (DPAD_CENTER/LEFT/RIGHT/UP/DOWN,
 * BACK) içerdiğinden bu katman; TV activity lifecycle'ına uygun focus bootstrap'i sağlar
 * ve gerekirse TV-spesifik overlay'leri ileride eklemenin noktasıdır.
 *
 * @param videoUrl          Oynatılacak video URL'si (boş olabilir — binge geçişlerinde ViewModel günceller)
 * @param audioUrl          Ayrı ses kanalı (opsiyonel, YouTube dual-stream için)
 * @param title             Şu an oynatılan başlık
 * @param requestHeaders    HTTP headers map (DRM / CDN gereksinimi olan kaynaklar için)
 * @param initialSubtitles  Başlangıç altyazı listesi
 * @param streamSources     Tüm kaynak listesi (kaynak değiştirme paneli için)
 * @param initialIndex      Başlangıçta seçili kaynak indeksi
 * @param malId             MyAnimeList ID
 * @param aniListId         AniList ID
 * @param season            Sezon numarası
 * @param episode           Bölüm numarası
 * @param animeTitle        Anime adı (binge geçişlerinde bölüm lookup için)
 * @param posterUrl         Poster URL (side panel ve post-play card için)
 * @param titleEnglish      İngilizce başlık
 * @param titleRomaji       Romaji başlık
 * @param titleNative       Yerel başlık
 * @param startYear         Yayın yılı
 * @param description       Özet
 * @param castList          Oyuncu listesi (side panel)
 * @param onBack            Geri navigasyon callback
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun TvPlayerScreen(
    videoUrl: String,
    audioUrl: String? = null,
    title: String = "",
    requestHeaders: Map<String, String> = emptyMap(),
    initialSubtitles: List<SubtitleInput> = emptyList(),
    streamSources: List<StreamSource> = emptyList(),
    initialIndex: Int = -1,
    malId: Int? = null,
    aniListId: Int? = null,
    season: Int = 1,
    episode: Int = 1,
    animeTitle: String = "",
    posterUrl: String? = null,
    titleEnglish: String? = null,
    titleRomaji: String? = null,
    titleNative: String? = null,
    startYear: Int? = null,
    description: String? = null,
    castList: List<MetaCastMember> = emptyList(),
    onBack: () -> Unit
) {
    // TV activity immersive mod sayesinde sistem barlar zaten gizli;
    // tüm focus'u player'ın kendi focusRequester'ına bırakıyoruz.
    val tvRootFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        tvRootFocus.requestFocusAfterFrames(frames = 2)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(tvRootFocus)
            .focusTarget()
    ) {
        KitsugiFullscreenPlayerScreen(
            videoId          = null,
            videoUrl         = videoUrl.ifBlank { null },
            audioUrl         = audioUrl,
            title            = title,
            requestHeaders   = requestHeaders,
            initialSubtitles = initialSubtitles,
            streamSources    = streamSources,
            initialIndex     = initialIndex,
            malId            = malId,
            aniListId        = aniListId,
            season           = season,
            episode          = episode,
            animeTitle       = animeTitle,
            posterUrl        = posterUrl,
            titleEnglish     = titleEnglish,
            titleRomaji      = titleRomaji,
            titleNative      = titleNative,
            startYear        = startYear,
            description      = description,
            castList         = castList,
            onBack           = onBack
        )
    }
}
