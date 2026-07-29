package com.kitsugi.animelist.data.local

import android.content.Context
import com.kitsugi.animelist.data.model.WatchHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WatchHistoryManager {

    private const val MAX_ENTRIES = 200

    private lateinit var store: WatchHistoryStore
    private val _history = MutableStateFlow<List<WatchHistoryEntry>>(emptyList())
    val history: StateFlow<List<WatchHistoryEntry>> = _history.asStateFlow()

    fun init(context: Context) {
        store = WatchHistoryStore(context.applicationContext)
        _history.value = store.getHistory()
    }

    /**
     * Bir bölümü izleme geçmişine ekler veya mevcut kaydı günceller.
     * Aynı animeId + episode kombinasyonu için en güncel kayıt tutulur.
     */
    fun record(entry: WatchHistoryEntry) {
        val current = _history.value.toMutableList()
        // Var olan kaydı sil (aynı bölüm)
        current.removeAll { it.animeId == entry.animeId && it.episode == entry.episode }
        // Başa ekle (en yeni en üstte)
        current.add(0, entry)
        // Max kayıt sınırı
        val trimmed = current.take(MAX_ENTRIES)
        _history.value = trimmed
        store.saveHistory(trimmed)
    }

    /**
     * Belirli bir kaydı sil.
     */
    fun remove(animeId: String, episode: Int) {
        val updated = _history.value.filterNot { it.animeId == animeId && it.episode == episode }
        _history.value = updated
        store.saveHistory(updated)
    }

    /**
     * Tüm geçmişi temizle.
     */
    fun clearAll() {
        _history.value = emptyList()
        store.saveHistory(emptyList())
    }
}
