package com.kitsugi.animelist.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CsPluginDao {
    @Query("SELECT * FROM cs_plugins ORDER BY installedAt DESC")
    fun getAllPluginsFlow(): Flow<List<CsPluginEntity>>

    @Query("SELECT * FROM cs_plugins ORDER BY installedAt DESC")
    suspend fun getAllPlugins(): List<CsPluginEntity>

    @Query("SELECT * FROM cs_plugins WHERE enabled = 1")
    fun getEnabledPluginsFlow(): Flow<List<CsPluginEntity>>

    @Query("SELECT * FROM cs_plugins WHERE enabled = 1")
    suspend fun getEnabledPlugins(): List<CsPluginEntity>

    @Query("SELECT * FROM cs_plugins WHERE id = :id LIMIT 1")
    suspend fun getPluginById(id: String): CsPluginEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plugin: CsPluginEntity)

    @Delete
    suspend fun delete(plugin: CsPluginEntity)
}
