package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.screens.fullscreen.controls.components.OutlinedNumericChooser
import kotlinx.coroutines.delay

val CARDS_MAX_WIDTH = 420.dp

@Composable
fun panelCardsColors() = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    disabledContainerColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.9f),
)

enum class DelayType { Audio, Subtitle }

@Suppress("LambdaParameterInRestartableEffect")
@Composable
fun DelayCard(
    delay: Int,
    onDelayChange: (Int) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    title: @Composable () -> Unit,
    delayType: DelayType,
    modifier: Modifier = Modifier,
    extraSettings: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = modifier
            .widthIn(max = CARDS_MAX_WIDTH)
            .animateContentSize(),
        colors = panelCardsColors(),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            title()

            OutlinedNumericChooser(
                label = { Text("Gecikme") },
                value = delay,
                onChange = onDelayChange,
                step = 50,
                min = Int.MIN_VALUE,
                max = Int.MAX_VALUE,
                suffix = { Text("ms") },
            )

            Column(modifier = Modifier.animateContentSize()) { extraSettings() }

            // True = heard→spotted (audio forward), false = spotted→heard (audio backward)
            var isDirectionPositive by remember { mutableStateOf<Boolean?>(null) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var timerStart by remember { mutableStateOf<Long?>(null) }
                var finalDelay by remember { mutableIntStateOf(delay) }

                LaunchedEffect(isDirectionPositive) {
                    if (isDirectionPositive == null) {
                        onDelayChange(finalDelay)
                        return@LaunchedEffect
                    }
                    finalDelay = delay
                    timerStart = System.currentTimeMillis()
                    val startingDelay = finalDelay
                    while (isDirectionPositive != null && timerStart != null) {
                        val elapsed = System.currentTimeMillis() - timerStart!!
                        finalDelay = startingDelay + (if (isDirectionPositive!!) elapsed else -elapsed).toInt()
                        delay(20)
                    }
                }

                val (labelA, labelB) = if (delayType == DelayType.Audio) {
                    "Ses duyuldu" to "Ses görüldü"
                } else {
                    "Ses duyuldu" to "Altyazı göründü"
                }

                Button(
                    onClick = {
                        isDirectionPositive = if (isDirectionPositive == null) delayType == DelayType.Audio else null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isDirectionPositive != (delayType == DelayType.Audio),
                ) { Text(labelA) }

                Button(
                    onClick = {
                        isDirectionPositive = if (isDirectionPositive == null) delayType != DelayType.Audio else null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isDirectionPositive != (delayType == DelayType.Subtitle),
                ) { Text(labelB) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f),
                    enabled = true,
                ) { Text("Varsayılan Yap") }
                FilledIconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        }
    }
}
