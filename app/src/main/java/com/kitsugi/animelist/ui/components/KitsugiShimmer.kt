package com.kitsugi.animelist.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kitsugi.animelist.ui.theme.KitsugiColors

// ─── Merkezi Shimmer Fırçası ───────────────────────────────────────────────
// KitsugiMobile HomeSkeletonLoading.kt'den uyarlama:
// 1200ms süren sağa doğru kayan lineer gradyan efekti.

@Composable
fun rememberKitsugiShimmerBrush(): Brush {
    val shimmerColors = listOf(
        KitsugiColors.Surface,
        KitsugiColors.SurfaceSoft.copy(alpha = 0.6f),
        KitsugiColors.SurfaceStrong.copy(alpha = 0.4f),
        KitsugiColors.SurfaceSoft.copy(alpha = 0.6f),
        KitsugiColors.Surface,
    )

    val transition = rememberInfiniteTransition(label = "KitsugiShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim + 100f, 0f),
    )
}

// ─── Temel Shimmer Blok ────────────────────────────────────────────────────
// Belirli boyutlarda animasyonlu placeholder blok.

@Composable
fun KitsugiShimmerBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
    )
}

// ─── Medya Kartı Shimmer ──────────────────────────────────────────────────
// KitsugiHorizontalMediaSection'daki SkeletonMediaCard'ın animasyonlu versiyonu.

@Composable
fun KitsugiShimmerMediaCard(
    brush: Brush = rememberKitsugiShimmerBrush(),
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KitsugiColors.Surface)
            .padding(14.dp),
    ) {
        // Poster alanı
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            cornerRadius = 20.dp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Başlık satırı
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp),
            cornerRadius = 999.dp,
        )

        Spacer(modifier = Modifier.height(7.dp))

        // Alt başlık satırı
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(11.dp),
            cornerRadius = 999.dp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Puan / meta satırı
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier
                .fillMaxWidth(0.40f)
                .height(10.dp),
            cornerRadius = 999.dp,
        )
    }
}

// ─── Yatay Shimmer Satırı ─────────────────────────────────────────────────
// ExploreScreen'deki her KitsugiHorizontalMediaSection'ın yükleme durumu.

@Composable
fun KitsugiShimmerMediaRow(
    cardCount: Int = 5,
) {
    val brush = rememberKitsugiShimmerBrush()
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(cardCount) {
            KitsugiShimmerMediaCard(brush = brush)
        }
    }
}

// ─── Hero Banner Shimmer ──────────────────────────────────────────────────
// ExploreScreen'de hero verileri yüklenirken gösterilir.

@Composable
fun KitsugiShimmerHeroSection(
    heroHeight: Dp = 420.dp,
    modifier: Modifier = Modifier,
) {
    val brush = rememberKitsugiShimmerBrush()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(brush),
    ) {
        // Alt köşe içerik placeholder
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.width(60.dp).height(10.dp),
                cornerRadius = 999.dp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.fillMaxWidth(0.78f).height(28.dp),
                cornerRadius = 8.dp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.fillMaxWidth(0.55f).height(16.dp),
                cornerRadius = 8.dp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            // Buton placeholder
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.width(120.dp).height(42.dp),
                cornerRadius = 999.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Dot indicator placeholder
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    KitsugiShimmerBlock(
                        brush = brush,
                        modifier = Modifier.size(8.dp),
                        cornerRadius = 999.dp,
                    )
                }
            }
        }
    }
}

// ─── Detay Sayfası Başlık Shimmer ─────────────────────────────────────────
// ApiResultDetailPage / MediaEntryDetailPage yüklenirken gösterilir.

@Composable
fun KitsugiShimmerDetailHeader(
    modifier: Modifier = Modifier,
) {
    val brush = rememberKitsugiShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        // Cover image placeholder
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier
                .width(130.dp)
                .height(190.dp),
            cornerRadius = 18.dp,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Başlık
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier.fillMaxWidth(0.85f).height(22.dp),
            cornerRadius = 8.dp,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Alt başlık
        KitsugiShimmerBlock(
            brush = brush,
            modifier = Modifier.fillMaxWidth(0.60f).height(14.dp),
            cornerRadius = 999.dp,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Meta satırı
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.width(48.dp).height(12.dp),
                cornerRadius = 999.dp,
            )
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.width(48.dp).height(12.dp),
                cornerRadius = 999.dp,
            )
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier.width(48.dp).height(12.dp),
                cornerRadius = 999.dp,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Açıklama satırları
        repeat(4) { i ->
            KitsugiShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth(if (i == 3) 0.55f else 1f)
                    .height(13.dp),
                cornerRadius = 999.dp,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

// ─── Karakter / Staff Avatar Satırı Shimmer ───────────────────────────────
// Karakter ve oyuncu bölümleri için yükleme durumu.

@Composable
fun KitsugiShimmerAvatarRow(
    avatarCount: Int = 6,
) {
    val brush = rememberKitsugiShimmerBrush()
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(avatarCount) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                KitsugiShimmerBlock(
                    brush = brush,
                    modifier = Modifier.size(62.dp),
                    cornerRadius = 999.dp, // daire
                )
                Spacer(modifier = Modifier.height(6.dp))
                KitsugiShimmerBlock(
                    brush = brush,
                    modifier = Modifier.width(56.dp).height(10.dp),
                    cornerRadius = 999.dp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                KitsugiShimmerBlock(
                    brush = brush,
                    modifier = Modifier.width(44.dp).height(8.dp),
                    cornerRadius = 999.dp,
                )
            }
        }
    }
}

// ─── Arama Sonuçları Shimmer Listesi ───────────────────────────────────────
// SearchScreen'deki arama sonuçları yüklenirken dikey liste halinde gösterilir.
@Composable
fun KitsugiShimmerSearchResultList(
    itemCount: Int = 5,
) {
    val brush = rememberKitsugiShimmerBrush()
    val isTv = com.kitsugi.animelist.ui.theme.LocalIsTv.current
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(itemCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(if (isTv) com.kitsugi.animelist.ui.theme.KitsugiTvTokens.Shapes.posterCard else RoundedCornerShape(16.dp))
                    .background(KitsugiColors.Surface)
                    .padding(if (isTv) 8.dp else 10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val thumbWidth = if (isTv) com.kitsugi.animelist.ui.theme.KitsugiTvTokens.Cards.searchThumbWidth else 56.dp
                val thumbHeight = if (isTv) com.kitsugi.animelist.ui.theme.KitsugiTvTokens.Cards.searchThumbHeight else 80.dp
                KitsugiShimmerBlock(
                    brush = brush,
                    modifier = Modifier.size(width = thumbWidth, height = thumbHeight),
                    cornerRadius = if (isTv) 12.dp else 10.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KitsugiShimmerBlock(
                        brush = brush,
                        modifier = Modifier.fillMaxWidth(0.7f).height(14.dp),
                        cornerRadius = 999.dp
                    )
                    KitsugiShimmerBlock(
                        brush = brush,
                        modifier = Modifier.fillMaxWidth(0.4f).height(10.dp),
                        cornerRadius = 999.dp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        KitsugiShimmerBlock(
                            brush = brush,
                            modifier = Modifier.width(36.dp).height(10.dp),
                            cornerRadius = 999.dp
                        )
                        KitsugiShimmerBlock(
                            brush = brush,
                            modifier = Modifier.width(36.dp).height(10.dp),
                            cornerRadius = 999.dp
                        )
                        KitsugiShimmerBlock(
                            brush = brush,
                            modifier = Modifier.width(44.dp).height(10.dp),
                            cornerRadius = 999.dp
                        )
                    }
                }
                KitsugiShimmerBlock(
                    brush = brush,
                    modifier = Modifier.size(40.dp),
                    cornerRadius = 12.dp
                )
            }
        }
    }
}

