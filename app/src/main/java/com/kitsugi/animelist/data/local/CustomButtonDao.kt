package com.kitsugi.animelist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomButtonDao {
    @Query("SELECT * FROM custom_buttons ORDER BY sortIndex ASC")
    fun subscribeAll(): Flow<List<CustomButton>>

    @Query("SELECT * FROM custom_buttons ORDER BY sortIndex ASC")
    suspend fun getAll(): List<CustomButton>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(button: CustomButton): Long

    @Update
    suspend fun update(button: CustomButton)

    @Query("DELETE FROM custom_buttons WHERE id = :id")
    suspend fun delete(id: Long)
}
