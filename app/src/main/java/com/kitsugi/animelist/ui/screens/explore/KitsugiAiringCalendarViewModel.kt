package com.kitsugi.animelist.ui.screens.explore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.remote.AiringEntry
import com.kitsugi.animelist.data.remote.KitsugiAiringCalendarClient
import kotlinx.coroutines.launch
import java.util.Calendar

class KitsugiAiringCalendarViewModel : ViewModel() {

    private val client = KitsugiAiringCalendarClient()

    /** Gün → yayın listesi. Key = Calendar.DAY_OF_WEEK (1=Paz, 2=Pzt, …, 7=Cmt) */
    val weekSchedule = mutableStateMapOf<Int, List<AiringEntry>>()

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Seçili gün (Calendar.DAY_OF_WEEK). Varsayılan: bugün */
    var selectedDay by mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        private set

    private var lastLoadedSource: String? = null

    init {
        loadSchedule()
    }

    fun loadSchedule(accessToken: String? = null, preferredSource: String? = null, force: Boolean = false) {
        if (!force && lastLoadedSource == preferredSource && weekSchedule.isNotEmpty()) return
        lastLoadedSource = preferredSource
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val schedule = client.fetchWeeklySchedule(accessToken, preferredSource)
                weekSchedule.clear()
                weekSchedule.putAll(schedule)
            } catch (e: Exception) {
                errorMessage = "Takvim yüklenemedi: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun selectDay(day: Int) {
        selectedDay = day
    }

    /** Seçili gün için yayın listesi — boş liste yerine emptyList() döner */
    fun entriesForSelectedDay(): List<AiringEntry> =
        weekSchedule[selectedDay] ?: emptyList()

    /** Haftada yayın olan gün sayısı (rozet için) */
    fun totalAiringCount(): Int = weekSchedule.values.sumOf { it.size }
}
