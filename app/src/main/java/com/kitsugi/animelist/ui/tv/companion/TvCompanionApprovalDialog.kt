package com.kitsugi.animelist.ui.tv.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kitsugi.animelist.core.companion.TvCompanionSessionManager
import com.kitsugi.animelist.ui.utils.requestFocusAfterFrames

// ── Local palette ──────────────────────────────────────────────────────────────

private val DialogBg = Color(0xFF12122A)
private val DialogCard = Color(0xFF1C1C3A)
private val DialogBorder = Color(0xFF2E2E5A)
private val DialogText = Color(0xFFE8EAF6)
private val DialogSubtext = Color(0xFF9FA8DA)
private val DialogAccent = Color(0xFF4F8EF7)
private val DialogGreen = Color(0xFF4CAF50)
private val DialogRed = Color(0xFFE53935)
private val DialogOrange = Color(0xFFFF9800)

/**
 * A TV-optimized modal overlay that asks the user to approve or reject a
 * pending companion mutation request (e.g. addon install, repo add).
 *
 * The dialog renders on top of the current screen using a semi-transparent
 * scrim and is fully D-pad navigable. Default focus lands on the **Approve**
 * button to reduce remote presses for typical confirm flows.
 *
 * @param pendingRequest  The request requiring approval. Pass `null` to hide.
 * @param onApprove       Called when the user approves the request.
 * @param onReject        Called when the user rejects the request.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCompanionApprovalDialog(
    pendingRequest: TvCompanionSessionManager.PendingRequest?,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isVisible = pendingRequest != null

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { it / 4 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 4 }
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            if (pendingRequest != null) {
                ApprovalCard(
                    request = pendingRequest,
                    onApprove = onApprove,
                    onReject = onReject
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ApprovalCard(
    request: TvCompanionSessionManager.PendingRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val approveFocusRequester = remember { FocusRequester() }

    // Auto-focus approve button when dialog appears
    LaunchedEffect(Unit) {
        approveFocusRequester.requestFocusAfterFrames(frames = 2)
    }

    // Determine visual metadata from action type
    val (actionIcon, actionLabel, accentColor) = when (request.action) {
        "INSTALL_ADDON" -> Triple(Icons.Filled.Extension, "Eklenti Kurulumu", DialogAccent)
        "ADD_REPO"      -> Triple(Icons.Filled.LibraryAdd, "Depo Ekleme", DialogOrange)
        else            -> Triple(Icons.Filled.Warning, request.action, DialogSubtext)
    }

    Column(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(DialogCard, DialogBg)
                )
            )
            .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column {
                Text(
                    text = "Onay Gerekiyor",
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = actionLabel,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DialogText
                )
            }
        }

        // ── Description ─────────────────────────────────────────────────────
        Text(
            text = "Telefonunuz aşağıdaki işlemi gerçekleştirmek istiyor. " +
                   "TV'nizden onaylayın veya reddedin.",
            fontSize = 15.sp,
            color = DialogSubtext,
            lineHeight = 22.sp
        )

        // ── Payload preview ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0A1E))
                .border(1.dp, DialogBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = request.payload,
                fontSize = 13.sp,
                color = DialogSubtext.copy(alpha = 0.8f),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Request ID (small)
        Text(
            text = "İstek #${request.id.takeLast(8)}",
            fontSize = 11.sp,
            color = DialogSubtext.copy(alpha = 0.5f)
        )

        // ── Action buttons ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
        ) {
            // Reject
            Button(
                onClick = onReject,
                colors = ButtonDefaults.colors(
                    containerColor = DialogCard,
                    contentColor = DialogRed,
                    focusedContainerColor = DialogRed,
                    focusedContentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reddet", fontWeight = FontWeight.SemiBold)
            }

            // Approve (receives default focus)
            Button(
                onClick = onApprove,
                modifier = Modifier
                    .focusRequester(approveFocusRequester)
                    .focusable(),
                colors = ButtonDefaults.colors(
                    containerColor = DialogGreen.copy(alpha = 0.25f),
                    contentColor = DialogGreen,
                    focusedContainerColor = DialogGreen,
                    focusedContentColor = Color.White
                )
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Onayla", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
