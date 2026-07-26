package com.kitsugi.animelist.ui.screens.fullscreen.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun DoubleTapSeekTriangles(isForward: Boolean) {
    val animationDuration = 750L

    val alpha1 = remember { Animatable(0f) }
    val alpha2 = remember { Animatable(0f) }
    val alpha3 = remember { Animatable(0f) }

    LaunchedEffect(animationDuration) {
        while (true) {
            alpha1.animateTo(1f, animationSpec = tween((animationDuration / 6).toInt()))
            alpha2.animateTo(1f, animationSpec = tween((animationDuration / 6).toInt()))
            alpha3.animateTo(1f, animationSpec = tween((animationDuration / 6).toInt()))
            alpha1.animateTo(0f, animationSpec = tween((animationDuration / 6).toInt()))
            alpha2.animateTo(0f, animationSpec = tween((animationDuration / 6).toInt()))
            alpha3.animateTo(0f, animationSpec = tween((animationDuration / 6).toInt()))
        }
    }

    val rotation = if (isForward) 0f else 180f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.rotate(rotation),
    ) {
        DoubleTapArrow(alpha1.value)
        DoubleTapArrow(alpha2.value)
        DoubleTapArrow(alpha3.value)
    }
}

@Composable
private fun DoubleTapArrow(
    alpha: Float,
) {
    Canvas(
        modifier = Modifier
            .size(width = 12.dp, height = 16.dp)
            .alpha(alpha)
    ) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = path,
            color = Color.White
        )
    }
}
