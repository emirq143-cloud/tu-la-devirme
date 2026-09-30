package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameProgressEntity
import com.example.game.BallSkin
import com.example.game.BallSkins
import com.example.ui.AppScreen
import com.example.ui.GameViewModel
import com.example.ui.theme.*

@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    gameProgress: GameProgressEntity
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.MAIN_MENU)
    }

    val unlockedList = gameProgress.unlockedSkins.split(",").toSet()

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
        // Top Bar
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
                            .testTag("shop_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Top Mağazası",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Balance Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ArcadeSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArcadeTertiary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🪙", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${gameProgress.coins}",
                            color = ArcadeTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Skins List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(BallSkins.ALL, key = { it.id }) { skin ->
                val isUnlocked = unlockedList.contains(skin.id)
                val isSelected = gameProgress.selectedBallSkin == skin.id

                SkinItemCard(
                    skin = skin,
                    isUnlocked = isUnlocked,
                    isSelected = isSelected,
                    canAfford = gameProgress.coins >= skin.cost,
                    onSelect = { viewModel.selectSkin(skin.id) },
                    onBuy = { viewModel.buySkin(skin.id, skin.cost) }
                )
            }
        }
    }
}

@Composable
private fun SkinItemCard(
    skin: BallSkin,
    isUnlocked: Boolean,
    isSelected: Boolean,
    canAfford: Boolean,
    onSelect: () -> Unit,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArcadeSurface),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, ArcadePrimary)
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(20.dp))
            .testTag("skin_card_${skin.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Ball graphic preview & info
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ball Preview Canvas
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ArcadeSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(36.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        // Outer glow
                        drawCircle(
                            color = skin.glowColor.copy(alpha = 0.4f),
                            radius = size.width * 0.48f,
                            center = Offset(cx, cy)
                        )
                        // Core
                        drawCircle(
                            color = skin.primaryColor,
                            radius = size.width * 0.35f,
                            center = Offset(cx, cy)
                        )
                        // Glint
                        drawCircle(
                            color = Color.White.copy(alpha = 0.8f),
                            radius = size.width * 0.12f,
                            center = Offset(cx - size.width * 0.1f, cy - size.height * 0.1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = skin.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = skin.iconSymbol, fontSize = 14.sp)
                    }
                    Text(
                        text = skin.description,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Action: Selected / Select / Buy
            when {
                isSelected -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ArcadePrimary.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcadePrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ArcadePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kullanımda",
                                color = ArcadePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                isUnlocked -> {
                    Button(
                        onClick = onSelect,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcadeSurfaceVariant),
                        modifier = Modifier.testTag("select_skin_${skin.id}")
                    ) {
                        Text(
                            text = "Kullan",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onBuy,
                        enabled = canAfford,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ArcadeTertiary,
                            disabledContainerColor = ArcadeSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("buy_skin_${skin.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${skin.cost}",
                                color = if (canAfford) Color.Black else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
