package com.kitsugi.animelist.ui.screens.stream

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.data.repository.StreamSource
import com.kitsugi.animelist.ui.theme.KitsugiColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight

/**
 * Renders stream result items directly into an existing [LazyListScope] (e.g. for sticky header integration).
 */
fun LazyListScope.renderStreamResultsItems(
    addonStates: List<AddonFetchState>,
    accentColor: Color,
    onStreamSelected: (StreamSource) -> Unit,
    onVerifyPlugin: ((addonName: String) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    addonStates.forEachIndexed { addonIdx, addonState ->
        if (addonState.isLoading) {
            item(key = "skeleton_${addonIdx}_${addonState.addonName}") {
                StreamSkeletonCard(addonName = addonState.addonName)
            }
        } else if (addonState.streams.isNotEmpty()) {
            addonState.streams.forEachIndexed { idx, source ->
                item(key = "${addonIdx}_${addonState.addonName}_$idx") {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(
                            tween(300),
                            initialOffsetY = { it / 3 }
                        )
                    ) {
                        StreamCard(
                            source      = source,
                            accentColor = accentColor,
                            onClick     = { onStreamSelected(source) }
                        )
                    }
                }
            }
        } else if (addonState.error != null) {
            item(key = "err_${addonIdx}_${addonState.addonName}") {
                StreamErrorCard(addonState = addonState, accentColor = accentColor, onVerify = onVerifyPlugin?.let { cb -> { cb(addonState.addonName) } })
            }
        }
    }

    if (onOpenSettings != null) {
        item(key = "settings_shortcut") {
            SettingsShortcutCard(accentColor = accentColor, onClick = onOpenSettings)
        }
    }
}

/**
 * Standalone LazyColumn wrapper for stream results list.
 */
@Composable
fun StreamResultsList(
    addonStates: List<AddonFetchState>,
    accentColor: Color,
    onStreamSelected: (StreamSource) -> Unit,
    onVerifyPlugin: ((addonName: String) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        renderStreamResultsItems(
            addonStates = addonStates,
            accentColor = accentColor,
            onStreamSelected = onStreamSelected,
            onVerifyPlugin = onVerifyPlugin,
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun StreamErrorCard(
    addonState: AddonFetchState,
    accentColor: Color = Color.Unspecified,
    onVerify: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Text(
                    text = addonState.addonName,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = addonState.error ?: "Bilinmeyen hata oluştu",
                color = KitsugiColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )

            if (onVerify != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Button(
                    onClick = onVerify,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Eklentiyi / Captcha Doğrula",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsShortcutCard(
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceStrong.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, KitsugiColors.SurfaceStrong.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Daha Fazla Kaynak Ekleyin",
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stremio ve Cloudstream eklentilerini yönetmek için Ayarlar'a gidin.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StreamSkeletonCard(addonName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "skel")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue  = 0.5f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "skelAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = KitsugiColors.SurfaceStrong.copy(alpha = alpha)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp, 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(KitsugiColors.Surface.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(80.dp, 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(KitsugiColors.Surface.copy(alpha = 0.3f))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(KitsugiColors.Surface.copy(alpha = 0.5f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(KitsugiColors.Surface.copy(alpha = 0.3f))
            )
        }
    }
}
