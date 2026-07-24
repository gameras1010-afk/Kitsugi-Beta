package com.kitsugi.animelist.ui.screens.detail

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.kitsugi.animelist.ui.screens.detail.components.SharedTrailerOverlay
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.kitsugi.animelist.ui.screens.fullscreen.KitsugiFullscreenPlayerActivity
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.core.player.engine.PlayerEngine
import com.kitsugi.animelist.core.player.engine.PlayerEngineType
import com.kitsugi.animelist.core.player.engine.Media3PlayerEngine
import com.kitsugi.animelist.core.player.engine.MpvPlayerEngine
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay


// ---------------------------------------------------------------------------
//  Fragman Kartı (tıklanınca inline player açılır; tam ekran butonu ile overlay)
// ---------------------------------------------------------------------------
@Composable
fun KitsugiTrailerCard(trailerUrl: String, mediaTitle: String = "") {
    val accentColor = LocalKitsugiAccent.current
    val uriHandler = LocalUriHandler.current
    val videoId = remember(trailerUrl) { extractYouTubeId(trailerUrl) }
    var showPlayer by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }

    // Full-screen overlay
    if (showOverlay) {
        SharedTrailerOverlay(
            title = if (mediaTitle.isNotBlank()) "$mediaTitle — Fragman" else "Fragman",
            trailerUrl = trailerUrl,
            onDismiss = { showOverlay = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Ön izleme",
            color = KitsugiColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = showPlayer && videoId != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            if (videoId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    KitsugiYouTubePlayer(
                        videoId = videoId,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (!showPlayer || videoId == null) {
            // Oynat butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .tvClickable(shape = RoundedCornerShape(16.dp)) {
                        if (videoId != null) {
                            showPlayer = true
                        } else {
                            runCatching { uriHandler.openUri(trailerUrl) }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Oynat",
                        tint = KitsugiColors.Background,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Fragmanı İzle",
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (videoId != null) "Uygulama içinde oynat" else "YouTube'da aç",
                        color = KitsugiColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // Kapat butonu
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .tvClickable(shape = RoundedCornerShape(12.dp)) { showPlayer = false }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    tint = KitsugiColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Kapat",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  Anime Temaları & TMDB Müzik Videoları / Fragmanlar Listesi
// ---------------------------------------------------------------------------
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ThemePlayerContainer(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalKitsugiAccent.current
    val uriHandler = LocalUriHandler.current

    var isPlayerError by remember(url) { mutableStateOf(false) }
    var isPlayerLoading by remember(url) { mutableStateOf(false) }
    var retryTrigger by remember(url) { mutableStateOf(0) }

    val videoId = remember(url) { extractYouTubeId(url) }

    if (videoId != null) {
        KitsugiYouTubePlayer(videoId = videoId, modifier = modifier)
    } else {
        val dataStore = remember { com.kitsugi.animelist.data.settings.SettingsDataStore(context.applicationContext) }
        val appSettingsState = dataStore.settingsFlow.collectAsState(initial = null)
        val appSettings = appSettingsState.value
        val safeSettings = remember(appSettings) { appSettings ?: com.kitsugi.animelist.data.settings.AppSettings() }

        var activeEngineType by remember(url) { mutableStateOf(PlayerEngineType.MEDIA3) }
        val playerEngine = remember(context, activeEngineType, url) {
            when (activeEngineType) {
                PlayerEngineType.MPV -> MpvPlayerEngine(context, safeSettings)
                else -> Media3PlayerEngine(context, safeSettings)
            }
        }

        var isPlayingState by remember { mutableStateOf(true) }
        var currentPosition by remember { mutableStateOf(0L) }
        var duration by remember { mutableStateOf(0L) }

        DisposableEffect(playerEngine) {
            val listener = object : PlayerEngine.Listener {
                override fun onStateChanged(state: PlayerEngine.State) {
                    isPlayingState = playerEngine.isPlaying
                    isPlayerLoading = state == PlayerEngine.State.BUFFERING
                }

                override fun onPlaybackError(errorCode: Int, errorMsg: String, cause: Throwable?) {
                    isPlayerLoading = false
                    Log.e("ThemePlayerContainer", "Oynatma hatası: $errorMsg (kod: $errorCode)")
                    if (activeEngineType == PlayerEngineType.MEDIA3) {
                        Log.d("ThemePlayerContainer", "ExoPlayer failed, switching to MPV")
                        activeEngineType = PlayerEngineType.MPV
                    } else {
                        isPlayerError = true
                    }
                }

                override fun onPositionChanged(positionMs: Long, durationMs: Long) {
                    currentPosition = positionMs
                    duration = durationMs
                }

                override fun onTracksChanged(
                    audioTracks: List<com.kitsugi.animelist.ui.screens.fullscreen.components.TrackOption>,
                    subtitleTracks: List<com.kitsugi.animelist.ui.screens.fullscreen.components.TrackOption>
                ) {}
            }
            playerEngine.addListener(listener)
            onDispose {
                playerEngine.removeListener(listener)
                playerEngine.release()
            }
        }

        LaunchedEffect(url, retryTrigger, activeEngineType) {
            isPlayerError = false
            isPlayerLoading = true
            try {
                playerEngine.prepare(
                    videoUrl = url,
                    startPositionMs = currentPosition
                )
                playerEngine.play()
            } catch (e: Exception) {
                Log.e("ThemePlayerContainer", "Prepare error", e)
                if (activeEngineType == PlayerEngineType.MEDIA3) {
                    activeEngineType = PlayerEngineType.MPV
                } else {
                    isPlayerError = true
                    isPlayerLoading = false
                }
            }
        }

        LaunchedEffect(playerEngine, isPlayingState) {
            if (isPlayingState) {
                while (true) {
                    currentPosition = playerEngine.currentPosition
                    duration = playerEngine.duration.coerceAtLeast(0L)
                    delay(1000)
                }
            }
        }

        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isPlayerError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Video oynatılamadı",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tekrar denemek için ▶ düğmesine basın.",
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { retryTrigger++ },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Tekrar Dene", color = KitsugiColors.Background)
                        }
                        Button(
                            onClick = {
                                val q = java.net.URLEncoder.encode(url, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$q") }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Text("YouTube", color = Color.White)
                        }
                    }
                }
            } else {
                KitsugiPlayerGestureWrapper(
                    isPlaying = isPlayingState,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPause = {
                        if (isPlayingState) playerEngine.pause() else playerEngine.play()
                    },
                    onSeek = { target ->
                        playerEngine.seekTo(target)
                    },
                    modifier = Modifier.fillMaxSize(),
                    onFullscreen = {
                        playerEngine.pause()
                        KitsugiFullscreenPlayerActivity.startWithStreamUrls(
                            context = context,
                            videoUrl = url
                        )
                    }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            playerEngine.createVideoView(ctx)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (isPlayerLoading) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        }
    }
}

@Composable
fun KitsugiThemesList(
    openings: List<com.kitsugi.animelist.data.remote.KitsugiTheme>,
    endings: List<com.kitsugi.animelist.data.remote.KitsugiTheme>
) {
    if (openings.isEmpty() && endings.isEmpty()) return

    val accentColor = LocalKitsugiAccent.current
    val uriHandler = LocalUriHandler.current

    // URL state: hangi tema seçili (null = kapalı)
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }

    // Collapse state — varsayılan 3 öğe gösterilir
    val INITIAL_COUNT = 3
    var openingsExpanded by remember { mutableStateOf(false) }
    var endingsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(KitsugiColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Açılış Müzikleri
        if (openings.isNotEmpty()) {
            val visibleOpenings = if (openingsExpanded) openings else openings.take(INITIAL_COUNT)
            val hasMore = openings.size > INITIAL_COUNT

            // Başlık + toggle satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Açılış Müzikleri / Videolar",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (hasMore) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .tvClickable(shape = RoundedCornerShape(8.dp)) { openingsExpanded = !openingsExpanded }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (openingsExpanded) "Daha Az" else "+${openings.size - INITIAL_COUNT} Daha",
                            color = accentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (openingsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            visibleOpenings.forEach { theme ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    ThemeRow(
                        theme = theme,
                        isActive = activeVideoUrl == theme.videoUrl && activeVideoUrl != null,
                        accentColor = accentColor,
                        onClick = {
                            if (theme.videoUrl != null) {
                                activeVideoUrl = if (activeVideoUrl == theme.videoUrl) null else theme.videoUrl
                            } else {
                                val q = java.net.URLEncoder.encode(theme.label, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$q") }
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = activeVideoUrl == theme.videoUrl && activeVideoUrl != null,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        theme.videoUrl?.let { url ->
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                ThemePlayerContainer(
                                    url = url,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .tvClickable(shape = RoundedCornerShape(12.dp)) { activeVideoUrl = null }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = KitsugiColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Kapat",
                                        color = KitsugiColors.TextSecondary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Kapanış Müzikleri
        if (endings.isNotEmpty()) {
            if (openings.isNotEmpty()) {
                androidx.compose.material3.HorizontalDivider(
                    color = KitsugiColors.Background.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }

            val visibleEndings = if (endingsExpanded) endings else endings.take(INITIAL_COUNT)
            val hasMore = endings.size > INITIAL_COUNT

            // Başlık + toggle satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kapanış Müzikleri",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (hasMore) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .tvClickable(shape = RoundedCornerShape(8.dp)) { endingsExpanded = !endingsExpanded }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (endingsExpanded) "Daha Az" else "+${endings.size - INITIAL_COUNT} Daha",
                            color = accentColor,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (endingsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            visibleEndings.forEach { theme ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    ThemeRow(
                        theme = theme,
                        isActive = activeVideoUrl == theme.videoUrl && activeVideoUrl != null,
                        accentColor = accentColor,
                        onClick = {
                            if (theme.videoUrl != null) {
                                activeVideoUrl = if (activeVideoUrl == theme.videoUrl) null else theme.videoUrl
                            } else {
                                val q = java.net.URLEncoder.encode(theme.label, "UTF-8")
                                runCatching { uriHandler.openUri("https://www.youtube.com/results?search_query=$q") }
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = activeVideoUrl == theme.videoUrl && activeVideoUrl != null,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        theme.videoUrl?.let { url ->
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                ThemePlayerContainer(
                                    url = url,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .tvClickable(shape = RoundedCornerShape(12.dp)) { activeVideoUrl = null }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = KitsugiColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Kapat",
                                        color = KitsugiColors.TextSecondary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(
    theme: com.kitsugi.animelist.data.remote.KitsugiTheme,
    isActive: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) accentColor.copy(alpha = 0.15f) else KitsugiColors.SurfaceSoft)
            .tvClickable(shape = RoundedCornerShape(10.dp), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isActive) accentColor else accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (isActive) KitsugiColors.Background else accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.label,
                color = if (isActive) accentColor else KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = if (theme.videoUrl != null) "Uygulama içinde oynat" else "YouTube'da ara",
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
