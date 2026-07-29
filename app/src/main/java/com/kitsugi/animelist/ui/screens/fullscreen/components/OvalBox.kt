package com.kitsugi.animelist.ui.screens.fullscreen.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

object RightSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(size.width, size.height)
            lineTo(size.width, 0f)
            lineTo(size.width / 10, 0f)
            cubicTo(
                size.width / 10,
                0f,
                -30f,
                size.height / 2,
                size.width / 10,
                size.height,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

object LeftSideOvalShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height)
            lineTo(size.width - size.width / 10, size.height)
            cubicTo(
                size.width - size.width / 10,
                size.height,
                size.width,
                size.height / 2,
                size.width - size.width / 10,
                0f,
            )
            close()
        }
        return Outline.Generic(path)
    }
}
