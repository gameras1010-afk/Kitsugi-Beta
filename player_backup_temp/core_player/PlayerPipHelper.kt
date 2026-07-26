package com.kitsugi.animelist.core.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.annotation.RequiresApi
import com.kitsugi.animelist.core.player.engine.PlayerEngine

/**
 * T2.3 – PlayerPipHelper (güncellenmiş)
 *
 * Picture-in-Picture (API 26+) yardımcısı.
 * API 26: PiP modu.
 * API 31+: RemoteActions (Play/Pause/Skip Next) ile PiP kontrolü.
 *
 * Kullanım:
 * 1. `KitsugiFullscreenPlayerActivity.onUserLeaveHint()` → `enterPipSafe()` çağır
 * 2. `onPictureInPictureModeChanged()` → `onPipModeChanged()` ilet
 * 3. İzleme sırasında state değişince → `updatePipActions()` çağır
 */
object PlayerPipHelper {

    private const val TAG = "PlayerPipHelper"

    // Broadcast action'ları — Activity'nin BroadcastReceiver'ı bu action'ları dinler
    const val ACTION_PLAY      = "com.kitsugi.animelist.pip.PLAY"
    const val ACTION_PAUSE     = "com.kitsugi.animelist.pip.PAUSE"
    const val ACTION_SKIP_NEXT = "com.kitsugi.animelist.pip.SKIP_NEXT"

    private const val REQUEST_PLAY      = 100
    private const val REQUEST_PAUSE     = 101
    private const val REQUEST_SKIP_NEXT = 102

    /**
     * Cihazın PiP destekleyip desteklemediğini kontrol eder.
     */
    fun isPipSupported(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    /**
     * Uygulamayı PiP moduna geçirir.
     *
     * @param activity     Fullscreen player Activity
     * @param playerEngine Mevcut oynatıcı engine'i — video boyutu için
     * @param isPlaying    Şu an oynatılıyor mu? RemoteActions buna göre ayarlanır.
     * @param hasNext      Sonraki bölüm var mı?
     * @return `true` eğer PiP başlatıldıysa
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPip(
        activity: Activity,
        playerEngine: PlayerEngine?,
        isPlaying: Boolean = true,
        hasNext: Boolean = false
    ): Boolean {
        return try {
            val builder = PictureInPictureParams.Builder()

            // Video aspect ratio
            val rational = buildRational(playerEngine)
            builder.setAspectRatio(rational)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(false)
                builder.setSeamlessResizeEnabled(true)
            }

            // API 26+: RemoteActions ekle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val actions = buildRemoteActions(activity, isPlaying, hasNext)
                builder.setActions(actions)
            }

            activity.enterPictureInPictureMode(builder.build())
            Log.d(TAG, "PiP modu başlatıldı: aspect=$rational isPlaying=$isPlaying hasNext=$hasNext")
            true
        } catch (e: Exception) {
            Log.e(TAG, "PiP başlatılamadı", e)
            false
        }
    }

    /**
     * API 26'dan önce çağrılabilmesi için güvenli versiyon.
     */
    fun enterPipSafe(
        activity: Activity,
        playerEngine: PlayerEngine?,
        isPlaying: Boolean = true,
        hasNext: Boolean = false
    ): Boolean {
        if (!isPipSupported(activity)) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPip(activity, playerEngine, isPlaying, hasNext)
        } else false
    }

    /**
     * PiP modu aktifken oynatma state'i değiştiğinde RemoteActions butonlarını günceller.
     * Örn: kullanıcı pause/play'e bastığında butonu değiştirmek için.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePipActions(
        activity: Activity,
        playerEngine: PlayerEngine?,
        isPlaying: Boolean,
        hasNext: Boolean = false
    ) {
        try {
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(buildRational(playerEngine))
                .setActions(buildRemoteActions(activity, isPlaying, hasNext))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setSeamlessResizeEnabled(true)
            }

            activity.setPictureInPictureParams(builder.build())
            Log.d(TAG, "PiP actions updated: isPlaying=$isPlaying hasNext=$hasNext")
        } catch (e: Exception) {
            Log.w(TAG, "PiP params güncellenemedi", e)
        }
    }

    /**
     * Safe wrapper for updatePipActions — API kontrolü yapar.
     */
    fun updatePipActionsSafe(
        activity: Activity,
        playerEngine: PlayerEngine?,
        isPlaying: Boolean,
        hasNext: Boolean = false
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePipActions(activity, playerEngine, isPlaying, hasNext)
        }
    }

    /**
     * PiP modundan çıkarken yapılacak işlem.
     */
    fun onPipModeChanged(isInPipMode: Boolean, onPipChanged: (Boolean) -> Unit) {
        onPipChanged(isInPipMode)
        Log.d(TAG, "PiP mode değişti: $isInPipMode")
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun buildRational(playerEngine: PlayerEngine?): Rational {
        // İlerleyen fazlarda StreamInfoData'dan videoWidth/Height okunacak
        return Rational(16, 9)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildRemoteActions(
        activity: Activity,
        isPlaying: Boolean,
        hasNext: Boolean
    ): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()

        // Play / Pause aksiyonu
        if (isPlaying) {
            val pauseIntent = PendingIntent.getBroadcast(
                activity,
                REQUEST_PAUSE,
                Intent(ACTION_PAUSE).setPackage(activity.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(activity, android.R.drawable.ic_media_pause),
                    "Duraklat",
                    "Videoyu duraklat",
                    pauseIntent
                ).apply { isEnabled = true }
            )
        } else {
            val playIntent = PendingIntent.getBroadcast(
                activity,
                REQUEST_PLAY,
                Intent(ACTION_PLAY).setPackage(activity.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(activity, android.R.drawable.ic_media_play),
                    "Oynat",
                    "Videoyu oynat",
                    playIntent
                ).apply { isEnabled = true }
            )
        }

        // Sonraki bölüm aksiyonu (sadece varsa)
        if (hasNext) {
            val nextIntent = PendingIntent.getBroadcast(
                activity,
                REQUEST_SKIP_NEXT,
                Intent(ACTION_SKIP_NEXT).setPackage(activity.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                RemoteAction(
                    Icon.createWithResource(activity, android.R.drawable.ic_media_next),
                    "Sonraki",
                    "Sonraki bölüme geç",
                    nextIntent
                ).apply { isEnabled = true }
            )
        }

        return actions
    }
}
