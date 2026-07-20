package com.kitsugi.animelist.core.recommendations

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore instance - uygulamada tek bir ornek
private val Context.tvChannelDataStore by preferencesDataStore(name = "Kitsugi_tv_channel_prefs")

/**
 * B1.4 - TV Channel ID'yi DataStore'da persists eder.
 * Yeniden baslatmalar arasinda kanal ID'sinin kaybolmasini onler.
 * Kanal silinmisse clearChannelId() ile temizlenir, bir sonraki
 * ensureChannel() cagrisinda yeniden olusturulur.
 */
@Singleton
class TvChannelPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val channelIdKey = longPreferencesKey("continue_watching_channel_id")

    suspend fun getChannelId(): Long? =
        context.tvChannelDataStore.data.map { it[channelIdKey] }.first()

    suspend fun setChannelId(id: Long) {
        context.tvChannelDataStore.edit { it[channelIdKey] = id }
    }

    suspend fun clearChannelId() {
        context.tvChannelDataStore.edit { it.remove(channelIdKey) }
    }
}
