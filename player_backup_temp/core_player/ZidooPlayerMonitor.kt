package com.kitsugi.animelist.core.player

import android.content.Context
import android.os.Build

/**
 * FP-01 – ZidooPlayerMonitor
 *
 * Checks if the running device is a Zidoo media box, and manages the Zidoo native player package integration.
 */
object ZidooPlayerMonitor {

    const val ZIDOO_PACKAGE = "com.zidoo.player"
    const val ZIDOO_LEGACY_PACKAGE = "com.android.gallery3d"

    /**
     * Checks whether the device is a Zidoo media box based on system build information.
     */
    fun isZidooDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
        val model = Build.MODEL?.lowercase().orEmpty()
        val device = Build.DEVICE?.lowercase().orEmpty()
        val board = Build.BOARD?.lowercase().orEmpty()
        val brand = Build.BRAND?.lowercase().orEmpty()

        return manufacturer.contains("zidoo") ||
                model.contains("zidoo") ||
                device.contains("zidoo") ||
                board.contains("zidoo") ||
                brand.contains("zidoo")
    }

    /**
     * Resolves the package name of the Zidoo player if it is installed or if the device is a Zidoo box.
     */
    fun resolveZidooPackage(context: Context): String? {
        if (!isZidooDevice()) return null

        return try {
            context.packageManager.getPackageInfo(ZIDOO_PACKAGE, 0)
            ZIDOO_PACKAGE
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo(ZIDOO_LEGACY_PACKAGE, 0)
                ZIDOO_LEGACY_PACKAGE
            } catch (ex: Exception) {
                ZIDOO_PACKAGE // Default fallback
            }
        }
    }
}
