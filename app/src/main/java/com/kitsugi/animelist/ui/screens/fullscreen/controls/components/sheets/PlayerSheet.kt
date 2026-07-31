package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.sheets

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Kitsugi'ye uyarlanmış PlayerSheet sarmalayıcı.
 * Artık sürükleyici modu bozmayan ve yerel hiyerarşide çalışan özel bir bileşendir.
 */
@Composable
fun PlayerSheet(
    onDismissRequest: () -> Unit,
    dismissEvent: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismissRequest)

    LaunchedEffect(dismissEvent) {
        if (dismissEvent) {
            onDismissRequest()
        }
    }

    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            background = Color(0xFF16162A),
            onBackground = Color.White,
            surface = Color(0xFF16162A),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF25254A),
            onSurfaceVariant = Color.White,
            primary = Color(0xFF8C8CFF),
            onPrimary = Color.Black
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .widthIn(max = 600.dp), // landscape/tablet genişlik sınırı
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = Color(0xFF16162A).copy(alpha = 0.95f),
            contentColor = Color.White,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                // Sürükleme kolu taklidi (Drag Handle)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
                content()
            }
        }
    }
}
