package com.kitsugi.animelist.ui.screens.mylist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitsugi.animelist.data.local.MediaEntryRepository
import com.kitsugi.animelist.data.local.KitsugiDatabase
import com.kitsugi.animelist.model.MediaEntry
import com.kitsugi.animelist.model.WatchStatus
import com.kitsugi.animelist.model.MediaType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyListViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // ─── Sync mesajları için Channel ─────────────────────────────────────────
    // Screen, bu channel'dan mesajları toplayarak kullanıcıya gösterir.
    private val _syncMessages = Channel<String>(Channel.BUFFERED)
    val syncMessages = _syncMessages.receiveAsFlow()

    // ─── Repository & Flow ───────────────────────────────────────────────────
    // Repository hemen oluşturuluyor; entriesFlow her zaman non-null ve hazır.
    private val repository: MediaEntryRepository = run {
        val dao = KitsugiDatabase.getDatabase(context).mediaEntryDao()
        MediaEntryRepository(
            dao = dao,
            context = context,
            onExternalSyncMessage = { msg ->
                viewModelScope.launch { _syncMessages.send(msg) }
            }
        )
    }

    val entriesFlow: StateFlow<List<MediaEntry>> = repository.entriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Bozuk Simkl subtitle'larını tek seferlik onar
        viewModelScope.launch {
            val dao = KitsugiDatabase.getDatabase(context).mediaEntryDao()
            repairBrokenSimklSubtitles(dao)
        }
    }

    /**
     * Eski Simkl importlarından kalan bozuk subtitle'ları düzeltir.
     * Örn: "movies.uppercase() â€¢ 2011" → "Film • 2011"
     */
    private suspend fun repairBrokenSimklSubtitles(
        dao: com.kitsugi.animelist.data.local.MediaEntryDao
    ) {
        val brokenPatterns = listOf("uppercase()", "â€¢", "â€")
        val all = dao.getAll()
        val broken = all.filter { entity ->
            entity.source == "simkl" &&
                brokenPatterns.any { entity.subtitle.contains(it) }
        }
        broken.forEach { entity ->
            val typeLabel = when (entity.type) {
                "Movie" -> "Film"
                "TvShow" -> "Dizi"
                "Anime" -> "Anime"
                else -> "Manga"
            }
            val fixedSubtitle = if (entity.year != null && entity.year > 0) {
                "$typeLabel • ${entity.year}"
            } else {
                typeLabel
            }
            dao.update(entity.copy(subtitle = fixedSubtitle))
        }
    }

    fun incrementEntryProgress(entry: MediaEntry) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

        val nextProgress = if (entry.total != null) {
            (entry.progress + 1).coerceAtMost(entry.total)
        } else {
            entry.progress + 1
        }

        val nextStatus = if (entry.total != null && nextProgress >= entry.total) {
            WatchStatus.Completed
        } else if (entry.status == WatchStatus.Planned) {
            WatchStatus.Watching
        } else {
            entry.status
        }

        val autoStartDate = if (entry.status == WatchStatus.Planned && entry.startDate.isNullOrBlank()) {
            today
        } else {
            entry.startDate
        }

        val autoEndDate = if (nextStatus == WatchStatus.Completed && entry.endDate.isNullOrBlank()) {
            today
        } else {
            entry.endDate
        }

        val updatedEntry = entry.copy(
            progress = nextProgress,
            status = nextStatus,
            startDate = autoStartDate,
            endDate = autoEndDate
        )

        viewModelScope.launch {
            repository.update(updatedEntry)
        }
    }

    fun insertEntry(entry: MediaEntry) {
        viewModelScope.launch {
            repository.insert(entry)
        }
    }

    fun updateEntry(entry: MediaEntry) {
        viewModelScope.launch {
            repository.update(entry)
        }
    }

    fun deleteEntryById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}
