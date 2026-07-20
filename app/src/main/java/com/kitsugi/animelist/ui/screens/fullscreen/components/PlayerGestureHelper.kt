package com.kitsugi.animelist.ui.screens.fullscreen.components

import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * GestureSwipeSide — hangi tarafta dikey swipe yapıldığını izler.
 */
enum class GestureSwipeSide { NONE, LEFT, RIGHT }

/**
 * PlayerGestureState — gesture tabanlı oynatıcı kontrol durumu.
 *
 * - [volumeLevel]       : Anlık ses seviyesi (0f..1f)
 * - [brightnessLevel]   : Anlık ekran parlaklığı (0f..1f)
 * - [isHoldSpeeding]    : Long-press hız artırma aktif mi?
 * - [gestureOverlayText]: Aktif gesture sonucunda gösterilecek bilgi metni
 * - [gestureOverlayIcon]: Aktif gesture sonucunda gösterilecek ikon
 */
data class PlayerGestureState(
    val volumeLevel: Float = 1f,
    val brightnessLevel: Float = -1f,   // -1 = system brightness (default)
    val isHoldSpeeding: Boolean = false,
    val gestureOverlayText: String? = null,
    val gestureOverlayIcon: ImageVector? = null
)

/**
 * Gesture ayarları — AppSettings'ten okunarak bu nesneye dönüştürülür.
 */
data class PlayerGestureConfig(
    val volumeGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val zoomGestureEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val holdSpeedMultiplier: Float = 2.0f,
    /**
     * TASK_050 — Dikey swipe hassasiyeti.
     * 0.5 = yavaş/hassas, 1.0 = varsayılan, 2.0 = hızlı.
     * NuvioTV PlayerGestureDetector.scrollSensitivity referans alındı.
     */
    val scrollSensitivity: Float = 1.0f
)

/**
 * PlayerGestureOverlay — gesture feedback overlay composable.
 * Ses / parlaklık değişimini veya hız bilgisini gösterir.
 */
@Composable
fun PlayerGestureOverlay(
    text: String?,
    icon: ImageVector?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn(tween(120)),
        exit  = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color(0xFF050510).copy(alpha = 0.82f)
                        )
                    )
                )
                .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Text(
                    text  = text ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * VolumeProgressBar — gesture sırasında ses seviyesini gösteren dikey bar.
 */
@Composable
fun VolumeProgressBar(
    volume: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.6.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (volume > 0.5f) Icons.Rounded.VolumeUp
                          else if (volume > 0f) Icons.Rounded.VolumeDown
                          else Icons.Rounded.VolumeOff,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(volume.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )
        }
        Text(
            text  = "${(volume * 100).toInt()}%",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * BrightnessProgressBar — gesture sırasında parlaklığı gösteren dikey bar.
 */
@Composable
fun BrightnessProgressBar(
    brightness: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.6.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
            .height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (brightness > 0.5f) Icons.Rounded.BrightnessHigh
                          else Icons.Rounded.BrightnessLow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Box(
            modifier = Modifier
                .width(6.dp)
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(brightness.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFD54F))
            )
        }
        Text(
            text  = "${(brightness * 100).toInt()}%",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * rememberPlayerGestureController — gesture mantığını yöneten controller.
 *
 * @param config        Gesture ayarları (enable/disable bayrakları, seek saniyesi vs.)
 * @param onVolumeChange     Ses seviyesi değişince PlayerEngine.setVolume() çağır
 * @param onBrightnessChange Parlaklık değişince WindowManager.LayoutParams güncelle
 * @param onSeek             Seek callback (saniye cinsinden delta, pozitif/negatif)
 * @param onSpeedChange      Hız callback (1x / 2x geçiş için)
 */
@Composable
fun rememberPlayerGestureController(
    config: PlayerGestureConfig,
    initialVolume: Float = 1f,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onSeek: (deltaMs: Long) -> Unit,
    onSpeedChange: (Float) -> Unit
): PlayerGestureController {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume    = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    val scope = rememberCoroutineScope()

    return remember(config) {
        PlayerGestureController(
            config           = config,
            audioManager     = audioManager,
            maxVolume        = maxVolume,
            onVolumeChange   = onVolumeChange,
            onBrightnessChange = onBrightnessChange,
            onSeek           = onSeek,
            onSpeedChange    = onSpeedChange,
            coroutineScope   = scope
        )
    }
}

/**
 * PlayerGestureController — gesture eventlerini state'e çeviren sınıf.
 * Compose dışında kullanılabilir; ViewModel'e inject edilebilir.
 */
class PlayerGestureController(
    val config: PlayerGestureConfig,
    private val audioManager: AudioManager,
    private val maxVolume: Float,
    private val onVolumeChange: (Float) -> Unit,
    private val onBrightnessChange: (Float) -> Unit,
    private val onSeek: (deltaMs: Long) -> Unit,
    private val onSpeedChange: (Float) -> Unit,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var gestureState = mutableStateOf(PlayerGestureState())
        private set

    private var hideOverlayJob: kotlinx.coroutines.Job? = null

    /** Cihazın mevcut ses seviyesini 0..1 olarak döndürür */
    fun currentVolumeNormalized(): Float {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current / maxVolume).coerceIn(0f, 1f)
    }

    /** Cihazın mevcut parlaklık seviyesini 0..1 olarak döndürür */
    fun currentBrightnessNormalized(context: Context): Float {
        val activity = context.findActivity() ?: return 0.5f
        val b = activity.window.attributes.screenBrightness
        return if (b < 0) 0.5f else b
    }

    /** Volume değerini doğrudan/mutlak set eder ve overlay gösterir */
    fun setVolumeAbsolute(level: Float) {
        val newLevel = level.coerceIn(0f, 1f)
        val intLevel = (newLevel * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, intLevel, 0)
        onVolumeChange(newLevel)

        val icon = when {
            newLevel > 0.5f -> Icons.Rounded.VolumeUp
            newLevel > 0f   -> Icons.Rounded.VolumeDown
            else            -> Icons.Rounded.VolumeOff
        }
        showOverlay("${(newLevel * 100).toInt()}%", icon)
        gestureState.value = gestureState.value.copy(volumeLevel = newLevel)
    }

    /** Parlaklık değerini doğrudan/mutlak set eder ve overlay gösterir */
    fun setBrightnessAbsolute(context: Context, level: Float) {
        val activity = context.findActivity() ?: return
        val newBrightness = level.coerceIn(0.01f, 1f)
        val params = activity.window.attributes
        params.screenBrightness = newBrightness
        activity.window.attributes = params
        onBrightnessChange(newBrightness)

        val icon = if (newBrightness > 0.5f) Icons.Rounded.BrightnessHigh else Icons.Rounded.BrightnessLow
        showOverlay("${(newBrightness * 100).toInt()}%", icon)
        gestureState.value = gestureState.value.copy(brightnessLevel = newBrightness)
    }

    /** Mevcut ses seviyesini delta kadar artırır/azaltır (normalized 0..1) */
    fun adjustVolume(delta: Float) {
        if (!config.volumeGestureEnabled) return
        // TASK_050: scrollSensitivity katsayısı ile delta ölçeklenir
        val scaledDelta = delta * config.scrollSensitivity
        val current  = currentVolumeNormalized()
        val newLevel = (current + scaledDelta).coerceIn(0f, 1f)
        val intLevel = (newLevel * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, intLevel, 0)
        onVolumeChange(newLevel)

        val icon = when {
            newLevel > 0.5f -> Icons.Rounded.VolumeUp
            newLevel > 0f   -> Icons.Rounded.VolumeDown
            else            -> Icons.Rounded.VolumeOff
        }
        showOverlay("${(newLevel * 100).toInt()}%", icon)
        gestureState.value = gestureState.value.copy(volumeLevel = newLevel)
    }

    /** Ekran parlaklığını delta kadar artırır/azaltır */
    fun adjustBrightness(context: Context, delta: Float) {
        if (!config.brightnessGestureEnabled) return
        val activity = context.findActivity() ?: return
        // TASK_050: scrollSensitivity katsayısı ile delta ölçeklenir
        val scaledDelta = delta * config.scrollSensitivity
        val currentBrightness = activity.window.attributes.screenBrightness
            .let { if (it < 0) 0.5f else it }  // system brightness = -1, fallback to 0.5
        val newBrightness = (currentBrightness + scaledDelta).coerceIn(0.01f, 1f)
        val params = activity.window.attributes
        params.screenBrightness = newBrightness
        activity.window.attributes = params
        onBrightnessChange(newBrightness)

        val icon = if (newBrightness > 0.5f) Icons.Rounded.BrightnessHigh else Icons.Rounded.BrightnessLow
        showOverlay("${(newBrightness * 100).toInt()}%", icon)
        gestureState.value = gestureState.value.copy(brightnessLevel = newBrightness)
    }

    /** Seek işlemi — saniye cinsinden delta */
    fun seek(deltaSeconds: Int) {
        val deltaMs = deltaSeconds * 1000L
        onSeek(deltaMs)
        val text = if (deltaMs > 0) "+${deltaSeconds}s" else "${deltaSeconds}s"
        val icon = if (deltaMs > 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind
        showOverlay(text, icon)
    }

    /** Long press başlangıcı — hız artırma */
    fun startHoldSpeed() {
        if (!gestureState.value.isHoldSpeeding) {
            gestureState.value = gestureState.value.copy(
                isHoldSpeeding = true,
                gestureOverlayText = "${config.holdSpeedMultiplier}x Hız",
                gestureOverlayIcon = Icons.Rounded.Speed
            )
            onSpeedChange(config.holdSpeedMultiplier)
        }
    }

    /** Long press bitişi — normal hıza dön */
    fun stopHoldSpeed() {
        onSpeedChange(1f)
        if (gestureState.value.isHoldSpeeding) {
            gestureState.value = gestureState.value.copy(
                isHoldSpeeding = false,
                gestureOverlayText = null,
                gestureOverlayIcon = null
            )
        }
    }

    private fun showOverlay(text: String, icon: ImageVector) {
        hideOverlayJob?.cancel()
        gestureState.value = gestureState.value.copy(
            gestureOverlayText = text,
            gestureOverlayIcon = icon
        )
        hideOverlayJob = coroutineScope.launch {
            delay(1200)
            if (!gestureState.value.isHoldSpeeding) {
                gestureState.value = gestureState.value.copy(
                    gestureOverlayText = null,
                    gestureOverlayIcon = null
                )
            }
        }
    }
}

// Helper extension — Context'ten Activity bulma
private fun Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
