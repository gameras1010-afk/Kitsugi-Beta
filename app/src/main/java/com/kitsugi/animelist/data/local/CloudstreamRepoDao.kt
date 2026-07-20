package com.kitsugi.animelist.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudstreamRepoDao {
    @Query("SELECT * FROM cloudstream_repos ORDER BY addedAt ASC")
    fun getAllReposFlow(): Flow<List<CloudstreamRepoEntity>>

    @Query("SELECT * FROM cloudstream_repos ORDER BY addedAt ASC")
    suspend fun getAllRepos(): List<CloudstreamRepoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepo(repo: CloudstreamRepoEntity)

    @Delete
    suspend fun deleteRepo(repo: CloudstreamRepoEntity)
}
