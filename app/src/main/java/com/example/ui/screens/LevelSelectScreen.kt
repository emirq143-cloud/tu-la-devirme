package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LevelProgressEntity
import com.example.game.LevelData
import com.example.ui.AppScreen
import com.example.ui.GameViewModel
import com.example.ui.theme.*

@Composable
fun LevelSelectScreen(
    viewModel: GameViewModel,
    levels: List<LevelProgressEntity>
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.MAIN_MENU)
    }

    val totalStars = levels.sumOf { it.stars }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        ArcadeBackground,
                        Color(0xFF141324),
                        ArcadeBackground
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // Top App Bar
        Surface(
            color = ArcadeSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ArcadeSurfaceVariant)
                            .testTag("level_select_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Bölüm Seç",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Total Stars Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ArcadeSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⭐", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalStars / 60",
                            color = ArcadeTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Levels Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val fullList = (1..20).map { id ->
                levels.find { it.levelId == id } ?: LevelProgressEntity(levelId = id, isUnlocked = id == 1)
            }

            items(fullList, key = { it.levelId }) { level ->
                LevelGridCard(
                    level = level,
                    onClick = {
                        if (level.isUnlocked) {
                            viewModel.startStageGame(level.levelId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LevelGridCard(
    level: LevelProgressEntity,
    onClick: () -> Unit
) {
    val layout = LevelData.getLevel(level.levelId)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (level.isUnlocked) ArcadeSurface else ArcadeSurfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (level.isUnlocked) {
            androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (level.isCompleted) ArcadeGreen else ArcadePrimary.copy(alpha = 0.8f)
            )
        } else null,
        modifier = Modifier
            .aspectRatio(0.85f)
            .shadow(if (level.isUnlocked) 4.dp else 0.dp, RoundedCornerShape(16.dp))
            .clickable(enabled = level.isUnlocked, onClick = onClick)
            .testTag("level_card_${level.levelId}")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!level.isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Kilitli",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text(
                        text = "${level.levelId}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (level.isCompleted) Color.White else ArcadePrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = layout.name,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stars
                    Row(horizontalArrangement = Arrangement.Center) {
                        for (s in 1..3) {
                            Text(
                                text = "★",
                                fontSize = 11.sp,
                                color = if (s <= level.stars) ArcadeTertiary else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
