package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {

    @Query("SELECT * FROM app_state WHERE id = 1 LIMIT 1")
    fun getAppStateFlow(): Flow<DbAppState?>

    @Query("SELECT * FROM app_state WHERE id = 1 LIMIT 1")
    suspend fun getAppState(): DbAppState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppState(appState: DbAppState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedChallenge(completed: DbCompletedChallenge)

    @Query("SELECT * FROM completed_challenges ORDER BY timestamp DESC")
    fun getAllCompletedChallengesFlow(): Flow<List<DbCompletedChallenge>>

    @Query("DELETE FROM completed_challenges")
    suspend fun clearCompletedHistory()
}
