package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.model.GameProgressEntity
import com.example.data.model.LevelProgressEntity
import com.example.game.BallSkins
import com.example.game.GameEngine
import com.example.game.GameMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    GAME,
    LEVEL_SELECT,
    SHOP
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val soundManager = SoundManager()
    val hapticManager = HapticManager(application)

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedLevelToPlay = MutableStateFlow(1)
    val selectedLevelToPlay: StateFlow<Int> = _selectedLevelToPlay.asStateFlow()

    private val _currentGameMode = MutableStateFlow(GameMode.STAGE)
    val currentGameMode: StateFlow<GameMode> = _currentGameMode.asStateFlow()

    private val _gameScore = MutableStateFlow(0)
    val gameScore: StateFlow<Int> = _gameScore.asStateFlow()

    private val _coinsNotification = MutableStateFlow<String?>(null)
    val coinsNotification: StateFlow<String?> = _coinsNotification.asStateFlow()

    val gameEngine: GameEngine

    val gameProgress: StateFlow<GameProgressEntity>
    val levels: StateFlow<List<LevelProgressEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        gameProgress = repository.gameProgress.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            GameProgressEntity()
        )

        levels = repository.levels.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        gameEngine = GameEngine(
            soundManager = soundManager,
            hapticManager = hapticManager,
            onScoreChanged = { newScore ->
                _gameScore.value = newScore
            },
            onCoinsEarned = { coins ->
                viewModelScope.launch {
                    repository.addCoins(coins)
                }
            },
            onGameOver = { score, waves ->
                viewModelScope.launch {
                    if (gameEngine.gameMode == GameMode.ENDLESS) {
                        repository.updateEndlessHighScore(score, waves)
                    }
                }
            },
            onVictory = { stars, score, _ ->
                viewModelScope.launch {
                    val rewardCoins = stars * 25 + 50
                    repository.completeLevel(
                        levelId = gameEngine.currentLevel,
                        starsEarned = stars,
                        score = score,
                        rewardCoins = rewardCoins
                    )
                }
            }
        )

        viewModelScope.launch {
            repository.initializeDefaultDataIfNeeded()
        }

        viewModelScope.launch {
            gameProgress.collect { progress ->
                soundManager.isEnabled = progress.soundEnabled
                hapticManager.isEnabled = progress.vibrationEnabled
                gameEngine.activeSkin = BallSkins.getById(progress.selectedBallSkin)
                gameEngine.releaseToShootEnabled = progress.releaseToShootEnabled
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        soundManager.playClick()
        _currentScreen.value = screen
    }

    fun startStageGame(levelNumber: Int) {
        _selectedLevelToPlay.value = levelNumber
        _currentGameMode.value = GameMode.STAGE
        _currentScreen.value = AppScreen.GAME
        gameEngine.startStage(levelNumber)
    }

    fun startEndlessGame() {
        _currentGameMode.value = GameMode.ENDLESS
        _currentScreen.value = AppScreen.GAME
        gameEngine.startEndless()
    }

    fun restartCurrentGame() {
        soundManager.playClick()
        if (gameEngine.gameMode == GameMode.STAGE) {
            gameEngine.startStage(_selectedLevelToPlay.value)
        } else {
            gameEngine.startEndless()
        }
    }

    fun nextLevel() {
        soundManager.playClick()
        val next = _selectedLevelToPlay.value + 1
        if (next <= 20) {
            startStageGame(next)
        } else {
            navigateTo(AppScreen.LEVEL_SELECT)
        }
    }

    fun toggleSound() {
        soundManager.playClick()
        viewModelScope.launch {
            repository.toggleSound()
        }
    }

    fun toggleVibration() {
        soundManager.playClick()
        viewModelScope.launch {
            repository.toggleVibration()
        }
    }

    fun toggleReleaseToShoot() {
        soundManager.playClick()
        viewModelScope.launch {
            repository.toggleReleaseToShoot()
        }
    }

    private var isClaimingDaily = false

    fun canClaimDailyReward(lastDate: Long): Boolean {
        return repository.canClaimDailyReward(lastDate)
    }

    fun getRemainingDailyClaimTime(lastDate: Long): String {
        val remainingMs = repository.getMillisUntilNextDailyClaim(lastDate)
        if (remainingMs <= 0L) return ""
        val hours = (remainingMs / (1000 * 60 * 60))
        val minutes = (remainingMs / (1000 * 60)) % 60
        val seconds = (remainingMs / 1000) % 60
        return if (hours > 0) "${hours}s ${minutes}d" else "${minutes}d ${seconds}sn"
    }

    fun claimDailyReward() {
        if (isClaimingDaily) return
        isClaimingDaily = true
        viewModelScope.launch {
            try {
                val amount = repository.claimDailyReward()
                if (amount > 0) {
                    soundManager.playVictoryFanfare()
                    _coinsNotification.value = "+$amount Altın Kazanıldı! 🎉"
                } else {
                    val remaining = getRemainingDailyClaimTime(gameProgress.value.lastDailyClaimDate)
                    _coinsNotification.value = "Bugünkü ödülünü zaten aldın! Kalan: $remaining"
                }
            } finally {
                isClaimingDaily = false
            }
        }
    }

    fun canSpinWheel(lastDate: Long): Boolean {
        return repository.canSpinWheel(lastDate)
    }

    fun getRemainingWheelSpinTime(lastDate: Long): String {
        val remainingMs = repository.getMillisUntilNextWheelSpin(lastDate)
        if (remainingMs <= 0L) return ""
        val hours = (remainingMs / (1000 * 60 * 60))
        val minutes = (remainingMs / (1000 * 60)) % 60
        val seconds = (remainingMs / 1000) % 60
        return if (hours > 0) "${hours}s ${minutes}d" else "${minutes}d ${seconds}sn"
    }

    fun claimWheelReward(coinsWon: Int, boosterWon: String?) {
        viewModelScope.launch {
            repository.claimWheelReward(coinsWon, boosterWon)
            soundManager.playVictoryFanfare()
            val desc = if (boosterWon != null) {
                val boosterName = when (boosterWon) {
                    "bomb" -> "1x Bomba"
                    "laser" -> "1x Lazer"
                    else -> "1x Deprem"
                }
                "+$coinsWon Altın ve $boosterName Kazandın!"
            } else {
                "+$coinsWon Altın Kazandın!"
            }
            _coinsNotification.value = desc
        }
    }

    fun activateOverdrive(): Boolean {
        return gameEngine.activateOverdrive()
    }

    fun dismissNotification() {
        _coinsNotification.value = null
    }

    fun selectSkin(skinId: String) {
        soundManager.playClick()
        viewModelScope.launch {
            repository.selectSkin(skinId)
        }
    }

    fun buySkin(skinId: String, cost: Int) {
        viewModelScope.launch {
            val success = repository.unlockSkin(skinId, cost)
            if (success) {
                soundManager.playVictoryFanfare()
            }
        }
    }

    fun useBombBooster(): Boolean {
        var used = false
        viewModelScope.launch {
            if (repository.useBooster("bomb")) {
                used = true
                gameEngine.triggerBombBooster()
            }
        }
        return used
    }

    fun useLaserBooster(): Boolean {
        var used = false
        viewModelScope.launch {
            if (repository.useBooster("laser")) {
                used = true
                gameEngine.triggerLaserBooster()
            }
        }
        return used
    }

    fun useEarthquakeBooster(): Boolean {
        var used = false
        viewModelScope.launch {
            if (repository.useBooster("earthquake")) {
                used = true
                gameEngine.triggerEarthquakeBooster()
            }
        }
        return used
    }
}
