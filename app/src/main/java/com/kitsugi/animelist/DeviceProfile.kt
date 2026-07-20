package com.kitsugi.animelist

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.app.UiModeManager

enum class DeviceFormFactor { PHONE, TABLET, TV }

object DeviceProfile {
    fun detect(context: Context): DeviceFormFactor {
        // 1. UiModeManager ile TV tespiti (en güvenilir)
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return DeviceFormFactor.TV
        }

        // 2. PackageManager Leanback feature kontrolü
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK_ONLY)) {
            return DeviceFormFactor.TV
        }

        // 3. Ekran genişliğiyle tablet/telefon ayrımı
        val smallestWidth = context.resources.configuration.smallestScreenWidthDp
        return if (smallestWidth >= 600) DeviceFormFactor.TABLET else DeviceFormFactor.PHONE
    }
}
