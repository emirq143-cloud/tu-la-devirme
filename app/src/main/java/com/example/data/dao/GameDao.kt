package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GameProgressEntity
import com.example.data.model.LevelProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_progress WHERE id = 1")
    fun getGameProgress(): Flow<GameProgressEntity?>

    @Query("SELECT * FROM game_progress WHERE id = 1")
    suspend fun getGameProgressSync(): GameProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: GameProgressEntity)

    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllLevels(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    suspend fun getLevel(levelId: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLevel(level: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLevels(levels: List<LevelProgressEntity>)
}
