package com.kitsugi.animelist.ui.screens.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * V2-D01 – CompanyLogosSection
 *
 * Anime/manga yapım şirketleri / stüdyoları listesi.
 * NuvioTV CompanyLogosSection.kt referans alındı.
 */

data class CompanyInfo(
    val id: Int,
    val name: String,
    val role: String, // "Production", "Licensor", "Studio" vb.
    val logoUrl: String? = null
)

@Composable
fun CompanyLogosSection(
    companies: List<CompanyInfo>,
    modifier: Modifier = Modifier
) {
    if (companies.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Yapım Şirketleri",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(companies, key = { it.id }) { company ->
                CompanyChip(company = company)
            }
        }
    }
}

@Composable
private fun CompanyChip(
    company: CompanyInfo
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = company.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = company.role,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
