package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgressEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 200,
    val endlessHighScore: Int = 0,
    val currentEndlessWave: Int = 1,
    val selectedBallSkin: String = "classic",
    val unlockedSkins: String = "classic",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val bombBoosters: Int = 3,
    val laserBoosters: Int = 3,
    val earthquakeBoosters: Int = 2,
    val lastDailyClaimDate: Long = 0L,
    val lastWheelSpinDate: Long = 0L,
    val releaseToShootEnabled: Boolean = false
)
