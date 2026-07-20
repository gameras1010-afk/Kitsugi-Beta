package com.kitsugi.animelist.core.player

import android.content.Context
import android.content.pm.PackageManager

/**
 * T2.5 - Model for representing supported external players with package name, name, and store link.
 */
data class ExternalPlayerDef(
    val name: String,
    val packageName: String,
    val storeUrl: String,
    val supportsSubs: Boolean = true,
    val supportsPosition: Boolean = true
) {
    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

object ExternalPlayerPackages {
    val players = listOf(
        ExternalPlayerDef(
            name = "Sistem Varsayılanı",
            packageName = "",
            storeUrl = "",
            supportsSubs = false,
            supportsPosition = false
        ),
        ExternalPlayerDef(
            name = "Just Player",
            packageName = "just.player",
            storeUrl = "https://play.google.com/store/apps/details?id=just.player"
        ),
        ExternalPlayerDef(
            name = "VLC",
            packageName = "org.videolan.vlc",
            storeUrl = "https://play.google.com/store/apps/details?id=org.videolan.vlc"
        ),
        ExternalPlayerDef(
            name = "MX Player",
            packageName = "com.mxtech.videoplayer.ad",
            storeUrl = "https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad"
        ),
        ExternalPlayerDef(
            name = "MX Player Pro",
            packageName = "com.mxtech.videoplayer.pro",
            storeUrl = "https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.pro"
        ),
        ExternalPlayerDef(
            name = "mpv-android",
            packageName = "is.xyz.mpv",
            storeUrl = "https://play.google.com/store/apps/details?id=is.xyz.mpv"
        ),
        ExternalPlayerDef(
            name = "Nova Video Player",
            packageName = "org.courville.nova",
            storeUrl = "https://play.google.com/store/apps/details?id=org.courville.nova"
        ),
        ExternalPlayerDef(
            name = "Vimu Media Player",
            packageName = "net.gtvbox.videoplayer",
            storeUrl = "https://play.google.com/store/apps/details?id=net.gtvbox.videoplayer"
        )
    )

    fun getPlayerByPackage(packageName: String): ExternalPlayerDef? {
        return players.firstOrNull { it.packageName == packageName }
    }
}
