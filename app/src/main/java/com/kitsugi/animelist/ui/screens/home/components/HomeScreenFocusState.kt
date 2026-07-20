package com.kitsugi.animelist.ui.screens.home.components

import androidx.compose.runtime.Stable

/**
 * FP-22 – Holds and manages D-pad focus state coordinates on the Home Screen.
 */
@Stable
class HomeScreenFocusState(
    initialRow: Int = 0,
    initialColumn: Int = 0
) {
    var currentRow: Int = initialRow
        private set
        
    var currentColumn: Int = initialColumn
        private set

    fun moveFocus(rowOffset: Int, columnOffset: Int) {
        currentRow = (currentRow + rowOffset).coerceAtLeast(0)
        currentColumn = (currentColumn + columnOffset).coerceAtLeast(0)
    }

    fun resetFocus() {
        currentRow = 0
        currentColumn = 0
    }
}
