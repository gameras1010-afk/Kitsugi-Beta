package com.kitsugi.animelist.ui.screens.fullscreen.controls.components.panels

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.kitsugi.animelist.ui.screens.fullscreen.components.SubtitleStyleSettings

@Composable
fun SubtitleSettingsPanel(
    subtitleStyle: SubtitleStyleSettings,
    onStyleChange: (SubtitleStyleSettings) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismissRequest)
    val orientation = LocalConfiguration.current.orientation
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    ConstraintLayout(modifier = modifier.fillMaxSize()) {
        val subSettingsCards = createRef()

        val cards: @Composable (Int, Modifier) -> Unit = { page, cardModifier ->
            when (page) {
                0 -> SubtitleTypographyCard(subtitleStyle, onStyleChange, cardModifier)
                1 -> SubtitleMiscCard(subtitleStyle, onStyleChange, cardModifier)
                else -> {}
            }
        }

        val pagerState = rememberPagerState { 2 }

        if (orientation == ORIENTATION_PORTRAIT) {
            Column(
                modifier = Modifier.constrainAs(subSettingsCards) {
                    top.linkTo(parent.top, 32.dp)
                    start.linkTo(parent.start)
                },
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Altyazı Ayarları",
                            style = MaterialTheme.typography.headlineMedium.copy(shadow = Shadow(blurRadius = 20f)),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
                )
                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fixed(screenWidthDp.dp * 0.9f),
                    verticalAlignment = Alignment.Top,
                    pageSpacing = 8.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    beyondViewportPageCount = 1,
                ) { page -> cards(page, Modifier.fillMaxWidth()) }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .constrainAs(subSettingsCards) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end, 32.dp)
                    }
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.width(CARDS_MAX_WIDTH),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Altyazı Ayarları",
                        style = MaterialTheme.typography.headlineMedium.copy(shadow = Shadow(blurRadius = 20f)),
                    )
                    IconButton(onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }
                repeat(2) { cards(it, Modifier) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SubtitleTypographyCard(
    style: SubtitleStyleSettings,
    onStyleChange: (SubtitleStyleSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(colors = panelCardsColors(), modifier = modifier.width(CARDS_MAX_WIDTH)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Yazı Tipi", style = MaterialTheme.typography.titleMedium)

            Text("Boyut: ${style.size}sp", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = style.size.toFloat(),
                onValueChange = { onStyleChange(style.copy(size = it.toInt())) },
                valueRange = 10f..60f,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Kalın (Bold)")
                Switch(
                    checked = style.bold,
                    onCheckedChange = { onStyleChange(style.copy(bold = it)) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Kenarlık")
                Switch(
                    checked = style.outlineEnabled,
                    onCheckedChange = { onStyleChange(style.copy(outlineEnabled = it)) },
                )
            }
        }
    }
}

@Composable
private fun SubtitleMiscCard(
    style: SubtitleStyleSettings,
    onStyleChange: (SubtitleStyleSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(colors = panelCardsColors(), modifier = modifier.width(CARDS_MAX_WIDTH)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Çeşitli", style = MaterialTheme.typography.titleMedium)

            Text("Dikey Konum: ${style.verticalOffset}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = style.verticalOffset.toFloat(),
                onValueChange = { onStyleChange(style.copy(verticalOffset = it.toInt())) },
                valueRange = -200f..200f,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Sadece tercih edilen dil")
                Switch(
                    checked = style.showOnlyPreferredLanguages,
                    onCheckedChange = { onStyleChange(style.copy(showOnlyPreferredLanguages = it)) },
                )
            }
        }
    }
}
