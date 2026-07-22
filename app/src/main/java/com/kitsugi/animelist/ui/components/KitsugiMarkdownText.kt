package com.kitsugi.animelist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.utils.KitsugiMarkdownUtils.formatAniListMarkdown
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

/**
 * Rich markdown renderer for AniList/MAL bio and comment text.
 *
 * Features:
 *  - Coil3 image transformer: renders images & animated GIFs inline
 *  - Interactive spoiler links → tapped → [InteractiveSpoilerSheet] overlay
 *  - Image links → tapped → gallery opened via [onImageGalleryRequest]
 *  - External links open the system browser
 *  - Styled links (accent color + underline) for visual clarity
 *  - Strikethrough, bold, italic, blockquote, code block support
 *
 * @param text              Raw AniList/MAL markdown or BBCode string.
 * @param modifier          Modifier for the root composable.
 * @param fontSize          Body text font size.
 * @param lineHeight        Body text line height.
 * @param onImageGalleryRequest  Called when an inline image is tapped.
 *                              Receives the ordered URL list and the tapped index.
 */
@Composable
fun KitsugiMarkdownText(
    text: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
    onImageGalleryRequest: ((urls: List<String>, index: Int) -> Unit)? = null,
) {
    val context  = LocalContext.current
    val accent   = LocalKitsugiAccent.current

    // Spoiler sheet state
    var activeSpoiler by remember { mutableStateOf<String?>(null) }

    val rendered = remember(text) { text?.formatAniListMarkdown().orEmpty() }

    // Build our URI handler — routes spoilers to local sheet, images to gallery
    val uriHandler = remember(context, accent, onImageGalleryRequest) {
        KitsugiMarkdownUriHandler(
            context = context,
            onSpoilerClicked = { spoilerText ->
                activeSpoiler = spoilerText
            },
            onImageClicked = { urls, index ->
                onImageGalleryRequest?.invoke(urls, index)
            },
        )
    }

    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        val parts = remember(rendered) { rendered.split("~~~") }
        if (parts.size <= 1) {
            Markdown(
                content = rendered,
                colors = markdownColor(
                    text             = KitsugiColors.TextPrimary,
                    codeBackground   = KitsugiColors.SurfaceStrong,
                    inlineCodeBackground = KitsugiColors.Surface,
                    dividerColor     = KitsugiColors.Border,
                    tableBackground  = KitsugiColors.Surface,
                ),
                typography = markdownTypography(
                    text = MaterialTheme.typography.bodyMedium.copy(
                        fontSize   = fontSize,
                        lineHeight = lineHeight,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    link = MaterialTheme.typography.bodyMedium.copy(
                        fontSize       = fontSize,
                        lineHeight     = lineHeight,
                        color          = accent,
                        fontWeight     = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                    ),
                    code = TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize   = (fontSize.value * 0.88f).sp,
                        color      = KitsugiColors.TextSecondary,
                    ),
                    h1 = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    h2 = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    h3 = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    quote = MaterialTheme.typography.bodyMedium.copy(
                        fontSize   = fontSize,
                        color      = KitsugiColors.TextSecondary,
                        fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                ),
                imageTransformer = Coil3ImageTransformerImpl,
                modifier = modifier,
            )
        } else {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val normalTypography = markdownTypography(
                    text = MaterialTheme.typography.bodyMedium.copy(
                        fontSize   = fontSize,
                        lineHeight = lineHeight,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    link = MaterialTheme.typography.bodyMedium.copy(
                        fontSize       = fontSize,
                        lineHeight     = lineHeight,
                        color          = accent,
                        fontWeight     = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                    ),
                    code = TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize   = (fontSize.value * 0.88f).sp,
                        color      = KitsugiColors.TextSecondary,
                    ),
                    h1 = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    h2 = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    h3 = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = KitsugiColors.TextPrimary,
                    ),
                    quote = MaterialTheme.typography.bodyMedium.copy(
                        fontSize   = fontSize,
                        color      = KitsugiColors.TextSecondary,
                        fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic,
                    ),
                )

                val centeredTypography = markdownTypography(
                    text = MaterialTheme.typography.bodyMedium.copy(
                        fontSize   = fontSize,
                        lineHeight = lineHeight,
                        color      = KitsugiColors.TextPrimary,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    link = MaterialTheme.typography.bodyMedium.copy(
                        fontSize       = fontSize,
                        lineHeight     = lineHeight,
                        color          = accent,
                        fontWeight     = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                        textAlign      = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    code = TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize   = (fontSize.value * 0.88f).sp,
                        color      = KitsugiColors.TextSecondary,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    h1 = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color      = KitsugiColors.TextPrimary,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    h2 = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = KitsugiColors.TextPrimary,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    h3 = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = KitsugiColors.TextPrimary,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                    quote = MaterialTheme.typography.bodyMedium.copy(
                        fontSize   = fontSize,
                        color      = KitsugiColors.TextSecondary,
                        fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                    ),
                )

                parts.forEachIndexed { index, part ->
                    if (part.isNotBlank()) {
                        val isCentered = index % 2 == 1
                        Markdown(
                            content = part,
                            colors = markdownColor(
                                text             = KitsugiColors.TextPrimary,
                                codeBackground   = KitsugiColors.SurfaceStrong,
                                inlineCodeBackground = KitsugiColors.Surface,
                                dividerColor     = KitsugiColors.Border,
                                tableBackground  = KitsugiColors.Surface,
                            ),
                            typography = if (isCentered) centeredTypography else normalTypography,
                            imageTransformer = Coil3ImageTransformerImpl,
                            modifier = if (isCentered) Modifier.fillMaxWidth() else Modifier,
                        )
                    }
                }
            }
        }
    }

    // Spoiler overlay sheet
    activeSpoiler?.let { spoilerContent ->
        InteractiveSpoilerSheet(
            spoilerText = spoilerContent,
            onDismiss   = { activeSpoiler = null },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Spoiler overlay composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen-aware spoiler reveal sheet that renders the spoiler content
 * with the same markdown renderer (so images/links inside spoilers also work).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveSpoilerSheet(
    spoilerText: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KitsugiColors.Surface,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(
                        color = KitsugiColors.Border,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            val accent = LocalKitsugiAccent.current
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Spoiler İçerik",
                    color = accent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            HorizontalDivider(color = KitsugiColors.Border)
            Spacer(Modifier.height(16.dp))
            // Render spoiler content itself with full markdown support
            KitsugiMarkdownText(
                text = spoilerText,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Inline spoiler box — kept for backwards-compat with any code that still
 * instantiates it directly. New code should prefer [InteractiveSpoilerSheet].
 */
@Composable
fun InteractiveSpoilerBox(
    spoilerText: String,
    modifier: Modifier = Modifier,
) {
    var isRevealed by remember { mutableStateOf(false) }
    val accentColor = LocalKitsugiAccent.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .border(
                width = 1.dp,
                color = if (isRevealed) accentColor.copy(alpha = 0.5f)
                        else KitsugiColors.AccentOrange.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { isRevealed = !isRevealed }
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = if (isRevealed) accentColor else KitsugiColors.AccentOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isRevealed) "Spoiler (Gizlemek için dokunun)"
                           else "⚠️ Spoiler İçerik (Görmek için dokunun)",
                    color = if (isRevealed) accentColor else KitsugiColors.AccentOrange,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (isRevealed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = spoilerText,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}
