package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.AppScreen
import com.example.ui.GameViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.BrickOutTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrickOutTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val gameProgress by viewModel.gameProgress.collectAsState()
                val levels by viewModel.levels.collectAsState()
                val coinsNotification by viewModel.coinsNotification.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(coinsNotification) {
                    coinsNotification?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissNotification()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.MAIN_MENU -> {
                                    MainMenuScreen(
                                        viewModel = viewModel,
                                        gameProgress = gameProgress,
                                        levels = levels
                                    )
                                }
                                AppScreen.GAME -> {
                                    GameScreen(
                                        viewModel = viewModel,
                                        gameProgress = gameProgress
                                    )
                                }
                                AppScreen.LEVEL_SELECT -> {
                                    LevelSelectScreen(
                                        viewModel = viewModel,
                                        levels = levels
                                    )
                                }
                                AppScreen.SHOP -> {
                                    ShopScreen(
                                        viewModel = viewModel,
                                        gameProgress = gameProgress
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
