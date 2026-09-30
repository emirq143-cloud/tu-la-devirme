package com.example.data

import com.example.data.dao.GameDao
import com.example.data.model.GameProgressEntity
import com.example.data.model.LevelProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GameRepository(private val gameDao: GameDao) {

    private val claimMutex = Mutex()

    val gameProgress: Flow<GameProgressEntity> = gameDao.getGameProgress().map {
        it ?: GameProgressEntity()
    }

    val levels: Flow<List<LevelProgressEntity>> = gameDao.getAllLevels()

    suspend fun initializeDefaultDataIfNeeded() {
        val currentProgress = gameDao.getGameProgressSync()
        if (currentProgress == null) {
            gameDao.insertOrUpdateProgress(GameProgressEntity())
        }

        // Initialize 20 levels
        val defaultLevels = (1..20).map { id ->
            LevelProgressEntity(
                levelId = id,
                stars = 0,
                highScore = 0,
                isUnlocked = id == 1,
                isCompleted = false
            )
        }
        gameDao.insertLevels(defaultLevels)
    }

    suspend fun updateEndlessHighScore(score: Int, wave: Int) {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        val newHigh = maxOf(current.endlessHighScore, score)
        gameDao.insertOrUpdateProgress(
            current.copy(
                endlessHighScore = newHigh,
                currentEndlessWave = wave
            )
        )
    }

    suspend fun addCoins(amount: Int) {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        gameDao.insertOrUpdateProgress(
            current.copy(coins = current.coins + amount)
        )
    }

    suspend fun spendCoins(amount: Int): Boolean {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        if (current.coins >= amount) {
            gameDao.insertOrUpdateProgress(
                current.copy(coins = current.coins - amount)
            )
            return true
        }
        return false
    }

    suspend fun unlockSkin(skinId: String, cost: Int): Boolean {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        val skins = current.unlockedSkins.split(",").toMutableSet()
        if (skins.contains(skinId)) return true

        if (current.coins >= cost) {
            skins.add(skinId)
            gameDao.insertOrUpdateProgress(
                current.copy(
                    coins = current.coins - cost,
                    unlockedSkins = skins.joinToString(","),
                    selectedBallSkin = skinId
                )
            )
            return true
        }
        return false
    }

    suspend fun selectSkin(skinId: String) {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        gameDao.insertOrUpdateProgress(
            current.copy(selectedBallSkin = skinId)
        )
    }

    suspend fun toggleSound() {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        gameDao.insertOrUpdateProgress(
            current.copy(soundEnabled = !current.soundEnabled)
        )
    }

    suspend fun toggleVibration() {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        gameDao.insertOrUpdateProgress(
            current.copy(vibrationEnabled = !current.vibrationEnabled)
        )
    }

    suspend fun useBooster(boosterType: String): Boolean {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        when (boosterType) {
            "bomb" -> {
                if (current.bombBoosters > 0) {
                    gameDao.insertOrUpdateProgress(current.copy(bombBoosters = current.bombBoosters - 1))
                    return true
                }
            }
            "laser" -> {
                if (current.laserBoosters > 0) {
                    gameDao.insertOrUpdateProgress(current.copy(laserBoosters = current.laserBoosters - 1))
                    return true
                }
            }
            "earthquake" -> {
                if (current.earthquakeBoosters > 0) {
                    gameDao.insertOrUpdateProgress(current.copy(earthquakeBoosters = current.earthquakeBoosters - 1))
                    return true
                }
            }
        }
        return false
    }

    suspend fun addBooster(boosterType: String, count: Int = 1) {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        when (boosterType) {
            "bomb" -> gameDao.insertOrUpdateProgress(current.copy(bombBoosters = current.bombBoosters + count))
            "laser" -> gameDao.insertOrUpdateProgress(current.copy(laserBoosters = current.laserBoosters + count))
            "earthquake" -> gameDao.insertOrUpdateProgress(current.copy(earthquakeBoosters = current.earthquakeBoosters + count))
        }
    }

    suspend fun completeLevel(levelId: Int, starsEarned: Int, score: Int, rewardCoins: Int) {
        val existing = gameDao.getLevel(levelId)
        val bestStars = maxOf(existing?.stars ?: 0, starsEarned)
        val bestScore = maxOf(existing?.highScore ?: 0, score)

        gameDao.insertOrUpdateLevel(
            LevelProgressEntity(
                levelId = levelId,
                stars = bestStars,
                highScore = bestScore,
                isUnlocked = true,
                isCompleted = true
            )
        )

        // Unlock next level
        if (levelId < 20) {
            val nextLevel = gameDao.getLevel(levelId + 1)
            if (nextLevel == null || !nextLevel.isUnlocked) {
                gameDao.insertOrUpdateLevel(
                    LevelProgressEntity(
                        levelId = levelId + 1,
                        stars = nextLevel?.stars ?: 0,
                        highScore = nextLevel?.highScore ?: 0,
                        isUnlocked = true,
                        isCompleted = nextLevel?.isCompleted ?: false
                    )
                )
            }
        }

        addCoins(rewardCoins)
    }

    fun canClaimDailyReward(lastDate: Long): Boolean {
        if (lastDate == 0L) return true
        val now = System.currentTimeMillis()
        return (now - lastDate) >= 24 * 60 * 60 * 1000L
    }

    fun getMillisUntilNextDailyClaim(lastDate: Long): Long {
        if (lastDate == 0L) return 0L
        val now = System.currentTimeMillis()
        val target = lastDate + 24 * 60 * 60 * 1000L
        return maxOf(0L, target - now)
    }

    suspend fun claimDailyReward(): Int = claimMutex.withLock {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        val now = System.currentTimeMillis()
        if (!canClaimDailyReward(current.lastDailyClaimDate)) {
            return@withLock 0 // Already claimed today!
        }
        val reward = 100
        gameDao.insertOrUpdateProgress(
            current.copy(
                coins = current.coins + reward,
                lastDailyClaimDate = now
            )
        )
        return@withLock reward
    }

    fun canSpinWheel(lastDate: Long): Boolean {
        if (lastDate == 0L) return true
        val now = System.currentTimeMillis()
        return (now - lastDate) >= 24 * 60 * 60 * 1000L
    }

    fun getMillisUntilNextWheelSpin(lastDate: Long): Long {
        if (lastDate == 0L) return 0L
        val now = System.currentTimeMillis()
        val target = lastDate + 24 * 60 * 60 * 1000L
        return maxOf(0L, target - now)
    }

    suspend fun claimWheelReward(coinsWon: Int, boosterWon: String?): Boolean = claimMutex.withLock {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        val now = System.currentTimeMillis()
        if (!canSpinWheel(current.lastWheelSpinDate)) {
            return@withLock false
        }
        var bomb = current.bombBoosters
        var laser = current.laserBoosters
        var earth = current.earthquakeBoosters

        when (boosterWon) {
            "bomb" -> bomb += 1
            "laser" -> laser += 1
            "earthquake" -> earth += 1
        }

        gameDao.insertOrUpdateProgress(
            current.copy(
                coins = current.coins + coinsWon,
                bombBoosters = bomb,
                laserBoosters = laser,
                earthquakeBoosters = earth,
                lastWheelSpinDate = now
            )
        )
        return@withLock true
    }

    suspend fun toggleReleaseToShoot() {
        val current = gameDao.getGameProgressSync() ?: GameProgressEntity()
        gameDao.insertOrUpdateProgress(
            current.copy(releaseToShootEnabled = !current.releaseToShootEnabled)
        )
    }
}
