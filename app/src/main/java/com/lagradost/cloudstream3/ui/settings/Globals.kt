package com.lagradost.cloudstream3.ui.settings

object Globals {
    const val PHONE = 0
    const val TV = 1
    const val EMULATOR = 2

    @JvmField
    var isLayout: Int = PHONE

    fun isLayout(layout: Int): Boolean {
        return isLayout == layout
    }

    var showGoogleHeader: Boolean = true
}
