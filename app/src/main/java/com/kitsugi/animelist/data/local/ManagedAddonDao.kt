package com.kitsugi.animelist.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAddonDao {
    @Query("SELECT * FROM managed_addons ORDER BY orderIndex ASC")
    fun getAllAddonsFlow(): Flow<List<ManagedAddonEntity>>

    @Query("SELECT * FROM managed_addons ORDER BY orderIndex ASC")
    suspend fun getAllAddons(): List<ManagedAddonEntity>

    @Query("SELECT * FROM managed_addons WHERE isEnabled = 1 ORDER BY orderIndex ASC")
    suspend fun getEnabledAddons(): List<ManagedAddonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddon(addon: ManagedAddonEntity)

    @Update
    suspend fun updateAddon(addon: ManagedAddonEntity)

    @Delete
    suspend fun deleteAddon(addon: ManagedAddonEntity)

    @Query("DELETE FROM managed_addons")
    suspend fun deleteAllAddons()
}
