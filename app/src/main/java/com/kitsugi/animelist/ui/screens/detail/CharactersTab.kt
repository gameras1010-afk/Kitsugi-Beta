package com.kitsugi.animelist.ui.screens.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import com.kitsugi.animelist.ui.utils.tvClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kitsugi.animelist.data.remote.KitsugiCharacter
import com.kitsugi.animelist.data.remote.KitsugiStaff
import com.kitsugi.animelist.ui.theme.LocalKitsugiAccent
import com.kitsugi.animelist.ui.theme.KitsugiColors
import com.kitsugi.animelist.ui.components.KitsugiShimmerAvatarRow

@Composable
fun CharactersTabContent(
    state: DetailTabState<List<KitsugiCharacter>>,
    onCharacterClick: (KitsugiCharacter) -> Unit,
    onStaffClick: (Int, String, String?, String?) -> Unit
) {
    when (state) {
        is DetailTabState.Loading -> {
            KitsugiShimmerAvatarRow(avatarCount = 6)
        }
        is DetailTabState.Error -> {
            Text(
                text = "Karakterler yüklenirken hata oluştu.",
                color = KitsugiColors.AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is DetailTabState.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                Text(
                    text = "Karakter bulunamadı.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    // Yatay mod: 2 sütunlu grid
                    val chunks = list.chunked(2)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { char ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        CharacterVoiceActorCard(char, onCharacterClick, onStaffClick)
                                    }
                                }
                                // Eğer tek öğe kaldıysa boş weight doldurucu
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        list.forEach { char ->
                            CharacterVoiceActorCard(char, onCharacterClick, onStaffClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterVoiceActorCard(
    char: KitsugiCharacter,
    onCharacterClick: (KitsugiCharacter) -> Unit,
    onStaffClick: (Int, String, String?, String?) -> Unit
) {
    val accentColor = LocalKitsugiAccent.current
    // Önce Japonca seslendirici ara (Türkçe karşılığı: "Japonca"), yoksa TMDB oyuncusu, yoksa ilki
    val va = char.voiceActors.firstOrNull { it.language.equals("Japonca", ignoreCase = true) }
        ?: char.voiceActors.firstOrNull { it.language.equals("oyuncu", ignoreCase = true) }
        ?: char.voiceActors.firstOrNull()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .padding(12.dp)
    ) {
        // Character Row (Top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onCharacterClick(char) }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KitsugiColors.SurfaceSoft)
            ) {
                if (!char.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = char.imageUrl,
                        contentDescription = char.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(char.name.take(2).uppercase(), color = KitsugiColors.TextMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = char.name,
                    color = KitsugiColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Rol zaten veri katmanında Türkçeye çevrilmiş gelir ("Ana Karakter", "Yardımcı Karakter", vb.)
                val isMainRole = char.role.contains("Ana", ignoreCase = true) || char.role.equals("Main", ignoreCase = true)
                Text(
                    text = char.role,
                    color = if (isMainRole) accentColor else KitsugiColors.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        if (va != null) {
            // A subtle connecting vertical line
            Box(
                modifier = Modifier
                    .padding(start = 21.dp, top = 2.dp, bottom = 2.dp)
                    .width(2.dp)
                    .height(10.dp)
                    .background(KitsugiColors.SurfaceSoft)
            )
            
            // Voice Actor Row (Bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onStaffClick(va.id, va.source, va.name, va.imageUrl) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KitsugiColors.SurfaceSoft)
                ) {
                    if (!va.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = va.imageUrl,
                            contentDescription = va.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(va.name.take(2).uppercase(), color = KitsugiColors.TextMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = va.name,
                        color = KitsugiColors.TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Dil zaten Türkçe geliyor: "Japonca", "Korece", "oyuncu" vb.
                    val labelText = if (va.language.equals("oyuncu", ignoreCase = true)) {
                        "Oyuncu"
                    } else {
                        "Seslendirici (${va.language})"
                    }
                    Text(
                        text = labelText,
                        color = KitsugiColors.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StaffTabContent(
    state: DetailTabState<List<KitsugiStaff>>,
    onStaffClick: (Int, String, String?, String?) -> Unit
) {
    when (state) {
        is DetailTabState.Loading -> {
            KitsugiShimmerAvatarRow(avatarCount = 6)
        }
        is DetailTabState.Error -> {
            Text(
                text = "Ekip bilgisi yüklenirken hata oluştu.",
                color = KitsugiColors.AccentRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        is DetailTabState.Success -> {
            val list = state.data
            if (list.isEmpty()) {
                Text(
                    text = "Ekip bilgisi bulunamadı.",
                    color = KitsugiColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (isLandscape) {
                    // Yatay mod: 2 sütunlu grid
                    val chunks = list.chunked(2)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { staff ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        StaffRow(staff = staff, onStaffClick = onStaffClick)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        list.forEach { staff ->
                            StaffRow(staff = staff, onStaffClick = onStaffClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffRow(
    staff: KitsugiStaff,
    onStaffClick: (Int, String, String?, String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KitsugiColors.Surface)
            .tvClickable(shape = RoundedCornerShape(16.dp), onClick = { onStaffClick(staff.id, staff.source, staff.name, staff.imageUrl) })
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KitsugiColors.SurfaceSoft)
        ) {
            if (!staff.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = staff.imageUrl,
                    contentDescription = staff.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(staff.name.take(2).uppercase(), color = KitsugiColors.TextMuted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = staff.name,
                color = KitsugiColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = staff.role,
                color = KitsugiColors.TextMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
