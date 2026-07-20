package com.kitsugi.animelist.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors

@Composable
fun KitsugiCinematicLoadingScreen(
    title: String,
    imageUrl: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoUrl: String? = null
) {
    val accentColor = LocalKitsugiAccent.current

    // Subtle scale-up animation for the center cover image to make it feel alive
    val infiniteTransition = rememberInfiniteTransition(label = "cinematic_loading")
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KitsugiColors.Background)
    ) {
        // 1. Fullscreen blurred background image with premium zoom-pulse animation
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scalePulse * 1.05f
                        scaleY = scalePulse * 1.05f
                        alpha = 0.5f
                    }
                    .blur(8.dp) // Soft blur for depth and legibility
            )
        }

        // 2. High-quality dark overlay gradient for depth of field & contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // 3. Top-left back button (Premium capsule look)
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .tvClickable(shape = RoundedCornerShape(99.dp)) { onBackClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Geri",
                tint = KitsugiColors.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Geri",
                color = KitsugiColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // 4. Center Cinematic Content (Clean, Apple/Netflix TV styled)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing thumbnail icon or cover if desired, but clean typography is preferred.
            // Let's add a small, highly elevated card in the center to anchor the page
            if (!imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scalePulse
                            scaleY = scalePulse
                        }
                        .shadow(32.dp, shape = RoundedCornerShape(24.dp), spotColor = accentColor)
                        .clip(RoundedCornerShape(24.dp))
                        .background(KitsugiColors.Surface)
                        .size(width = 120.dp, height = 180.dp)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Page/Content Title or Logo
            var logoFailed by remember(logoUrl) { mutableStateOf(false) }
            val showLogo = !logoUrl.isNullOrBlank() && !logoFailed

            if (showLogo) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(70.dp),
                    onError = { logoFailed = true }
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Spinner & Loading Text Group with new neon Torii Gate logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                // Neon Torii Gate logo from assets
                AsyncImage(
                    model = "file:///android_asset/kitsugi_current_logo.png",
                    contentDescription = "Kitsugi Torii Gate Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            // Subtle pulse scale effect matching the splash
                            scaleX = scalePulse
                            scaleY = scalePulse
                        }
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(12.dp),
                            spotColor = Color(0xFFFF2020),
                            ambientColor = Color(0xFFFF2020)
                        )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Custom neon loading sweep bar instead of circular progress indicator
                val sweepTransition = rememberInfiniteTransition(label = "sweep_bar")
                val sweepOffset by sweepTransition.animateFloat(
                    initialValue = -1f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "sweep_offset"
                )

                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.5f)
                            .align(
                                when {
                                    sweepOffset < -0.3f -> Alignment.CenterStart
                                    sweepOffset > 0.3f  -> Alignment.CenterEnd
                                    else               -> Alignment.Center
                                }
                            )
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFFFF2020),
                                        Color(0xFFFF6666),
                                        Color(0xFFFF2020),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Yükleniyor...",
                    color = KitsugiColors.TextPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
