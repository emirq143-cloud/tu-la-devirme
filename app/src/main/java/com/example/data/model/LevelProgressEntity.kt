package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelId: Int,
    val stars: Int = 0,
    val highScore: Int = 0,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false
)
