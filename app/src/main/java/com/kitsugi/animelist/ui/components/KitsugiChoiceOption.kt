package com.kitsugi.animelist.ui.components

import androidx.compose.ui.graphics.Color

data class KitsugiChoiceOption(
    val id: String,
    val title: String,
    val description: String,
    val color: Color? = null
)
