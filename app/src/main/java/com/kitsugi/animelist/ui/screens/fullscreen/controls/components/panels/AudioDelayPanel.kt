package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.kitsugi.animelist.data.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AudioDelayPanel(
    onDismissRequest: () -> Unit,
    onDelayChanged: (Long) -> Unit,
    currentAudioDelayMs: Int,
    modifier: Modifier = Modifier,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        val delayControlCard = createRef()

        var delay by remember { mutableIntStateOf(currentAudioDelayMs) }

        LaunchedEffect(delay) {
            onDelayChanged(delay.toLong())
        }

        DelayCard(
            delay = delay,
            onDelayChange = { delay = it },
            onApply = { /* saved via onDelayChanged */ },
            onReset = { delay = 0 },
            title = { AudioDelayCardTitle(onClose = onDismissRequest) },
            delayType = DelayType.Audio,
            modifier = Modifier.constrainAs(delayControlCard) {
                linkTo(parent.top, parent.bottom, bias = 0.8f)
                end.linkTo(parent.end)
            },
        )
    }
}

@Composable
private fun AudioDelayCardTitle(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Ses Gecikmesi",
            style = MaterialTheme.typography.headlineMedium,
        )
        IconButton(onClose) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(32.dp))
        }
    }
}
