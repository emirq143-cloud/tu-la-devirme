package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GameProgressEntity
import com.example.data.model.LevelProgressEntity
import com.example.ui.AppScreen
import com.example.ui.GameViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    gameProgress: GameProgressEntity,
    levels: List<LevelProgressEntity>
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showLuckyWheelDialog by remember { mutableStateOf(false) }

    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val canClaimDaily = remember(gameProgress.lastDailyClaimDate, currentTimeMillis) {
        viewModel.canClaimDailyReward(gameProgress.lastDailyClaimDate)
    }
    val remainingDailyTime = remember(gameProgress.lastDailyClaimDate, currentTimeMillis) {
        viewModel.getRemainingDailyClaimTime(gameProgress.lastDailyClaimDate)
    }

    val canSpinWheel = remember(gameProgress.lastWheelSpinDate, currentTimeMillis) {
        viewModel.canSpinWheel(gameProgress.lastWheelSpinDate)
    }
    val remainingWheelTime = remember(gameProgress.lastWheelSpinDate, currentTimeMillis) {
        viewModel.getRemainingWheelSpinTime(gameProgress.lastWheelSpinDate)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val completedLevelsCount = levels.count { it.isCompleted }
    val totalStars = levels.sumOf { it.stars }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ArcadeBackground,
                        Color(0xFF141324),
                        Color(0xFF1B1430)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 40.dp, bottom = 32.dp)
        ) {
            // Top Bar with Coins & Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coins Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ArcadeSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcadeTertiary.copy(alpha = 0.6f)),
                        modifier = Modifier.shadow(6.dp, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🪙", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${gameProgress.coins}",
                                color = ArcadeTertiary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Settings & Info Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ArcadeSurfaceVariant)
                                .size(42.dp)
                                .testTag("info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Nasıl Oynanır",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ArcadeSurfaceVariant)
                                .size(42.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Hero Graphic Banner
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = ArcadeSurface)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_brick_banner),
                            contentDescription = "Brick Out Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            ArcadeBackground.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "BRICK OUT",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = ArcadePrimary,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Topu Fırlat & Tuğlaları Yok Et!",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Stats Quick Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = "🏆",
                        label = "Sonsuz Rekor",
                        value = "${gameProgress.endlessHighScore}"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = "⭐",
                        label = "Yıldızlar",
                        value = "$totalStars"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = "🚩",
                        label = "Bölümler",
                        value = "$completedLevelsCount / 20"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Primary Play Buttons
            item {
                // Stage Mode Button (Highlighted)
                Button(
                    onClick = {
                        val firstIncomplete = levels.firstOrNull { it.isUnlocked && !it.isCompleted }?.levelId ?: 1
                        viewModel.startStageGame(firstIncomplete)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .scale(pulseScale)
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .testTag("play_stage_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcadeSecondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "BÖLÜM MODU",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Bölüm ${levels.firstOrNull { it.isUnlocked && !it.isCompleted }?.levelId ?: 1}'e Devam Et",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Endless Mode Button
                Button(
                    onClick = {
                        viewModel.startEndlessGame()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(6.dp, RoundedCornerShape(18.dp))
                        .testTag("play_endless_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcadePrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SONSUZ HAYATTA KALMA",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Secondary Buttons: Level Select & Shop
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.LEVEL_SELECT) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("level_select_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ArcadePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = ArcadePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bölümler",
                            color = ArcadePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.SHOP) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("shop_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ArcadeTertiary)
                    ) {
                        Text(text = "🎨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Toplar",
                            color = ArcadeTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Daily Reward Card (Fixed 24h once per day guarantee)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canClaimDaily) ArcadeSurfaceVariant else ArcadeSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = if (canClaimDaily) androidx.compose.foundation.BorderStroke(1.5.dp, ArcadeTertiary.copy(alpha = 0.8f)) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (canClaimDaily) 6.dp else 2.dp, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (canClaimDaily) "🎁" else "⏳", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (canClaimDaily) "Günlük Altın Hediyesi" else "Günün Hediyesi Alındı",
                                    color = if (canClaimDaily) Color.White else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (canClaimDaily) "+100 Altın hazır!" else "Yarın tekrar gel! (Kalan: $remainingDailyTime)",
                                    color = if (canClaimDaily) ArcadeTertiary else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (canClaimDaily) {
                                    viewModel.claimDailyReward()
                                }
                            },
                            enabled = canClaimDaily,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArcadeTertiary,
                                disabledContainerColor = ArcadeSurface.copy(alpha = 0.6f),
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("claim_daily_button")
                        ) {
                            Text(
                                text = if (canClaimDaily) "Al (+100)" else "Alındı ✓",
                                color = if (canClaimDaily) Color.Black else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Unique Feature: Lucky Fortune Wheel Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canSpinWheel) ArcadeSurfaceVariant else ArcadeSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = if (canSpinWheel) androidx.compose.foundation.BorderStroke(1.5.dp, ArcadePrimary.copy(alpha = 0.8f)) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (canSpinWheel) 6.dp else 2.dp, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "🎡", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Günlük Şans Çarkı",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (canSpinWheel) "Günde 1 kez çevir & ödül kap!" else "Kalan süre: $remainingWheelTime",
                                    color = if (canSpinWheel) ArcadePrimary else Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = { showLuckyWheelDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canSpinWheel) ArcadePrimary else ArcadeSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("lucky_wheel_button")
                        ) {
                            Text(
                                text = if (canSpinWheel) "Çevir 🎡" else "Görüntüle",
                                color = if (canSpinWheel) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            soundEnabled = gameProgress.soundEnabled,
            vibrationEnabled = gameProgress.vibrationEnabled,
            onToggleSound = { viewModel.toggleSound() },
            onToggleVibration = { viewModel.toggleVibration() },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showInfoDialog) {
        HowToPlayDialog(onDismiss = { showInfoDialog = false })
    }

    if (showLuckyWheelDialog) {
        LuckyWheelDialog(
            canSpin = canSpinWheel,
            remainingTime = remainingWheelTime,
            onSpinClaim = { coins, booster ->
                viewModel.claimWheelReward(coins, booster)
            },
            onDismiss = { showLuckyWheelDialog = false }
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ArcadeSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsDialog(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcadeSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Ayarlar",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = ArcadePrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Ses Efektleri", color = Color.White)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { onToggleSound() },
                        colors = SwitchDefaults.colors(checkedThumbColor = ArcadePrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = ArcadeSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Titreşim (Haptic)", color = Color.White)
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { onToggleVibration() },
                        colors = SwitchDefaults.colors(checkedThumbColor = ArcadeSecondary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ArcadePrimary)
            ) {
                Text(text = "Tamam", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArcadeSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Nasıl Oynanır?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoItem(
                    icon = "🎯",
                    title = "Kaydırmalı Açı & Fırlatma",
                    description = "İster ekranda parmağınla/fareyle nişan al, istersen alt kızak çubuğunu kaydırarak açıyı ayarla ve fırlat!"
                )
                InfoItem(
                    icon = "⚡",
                    title = "ÖFKE MODU (Overdrive)",
                    description = "Tuğlaları yok ettikçe öfke barı dolar. %100 olunca aktive et; delip geçen plazma ateş toplarıyla ortalığı yak!"
                )
                InfoItem(
                    icon = "🧿",
                    title = "Nazar Boncuğu Tılsımı",
                    description = "Mavi nazar bloğunu vurarak tahtadaki 3 rastgele tuğlayı anında buharlaştır ve koruma kalkanı aç!"
                )
                InfoItem(
                    icon = "🎡",
                    title = "Günlük Şans Çarkı",
                    description = "Her gün menüdeki çarkı ücretsiz çevirerek yüzlerce altın, bomba ve lazer güçlendiricisi kazan."
                )
                InfoItem(
                    icon = "➕",
                    title = "Ekstra Toplar & Lazerler",
                    description = "Yeşil +1 bloklarını vurarak top sayını artır, yatay ve dikey lazerlerle tüm sıraları temizle!"
                )
                InfoItem(
                    icon = "⚠️",
                    title = "Kırmızı Çizgiye Dikkat",
                    description = "Sonsuz modda tuğlaların kırmızı çizgiye ulaşmasına izin verme!"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ArcadeSecondary)
            ) {
                Text(text = "Harika, Başlayalım!", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

data class WheelSlice(
    val label: String,
    val coins: Int,
    val booster: String?,
    val color: Color
)

@Composable
fun LuckyWheelDialog(
    canSpin: Boolean,
    remainingTime: String,
    onSpinClaim: (coins: Int, booster: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val slices = remember {
        listOf(
            WheelSlice("+50 🪙", 50, null, Color(0xFFFBC02D)),
            WheelSlice("💣 Bomba", 0, "bomb", Color(0xFFE53935)),
            WheelSlice("+100 🪙", 100, null, Color(0xFFFFB300)),
            WheelSlice("⚡ Lazer", 0, "laser", Color(0xFF00ACC1)),
            WheelSlice("+250 🪙", 250, null, Color(0xFFFFA000)),
            WheelSlice("💥 Deprem", 0, "earthquake", Color(0xFF8E24AA)),
            WheelSlice("🧿 Nazar", 300, null, Color(0xFF1E88E5)),
            WheelSlice("+500 💎", 500, null, Color(0xFF00E676))
        )
    }

    var isSpinning by remember { mutableStateOf(false) }
    var wonSlice by remember { mutableStateOf<WheelSlice?>(null) }
    val rotationAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        containerColor = ArcadeSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GÜNLÜK ŞANS ÇARKI",
                    fontWeight = FontWeight.Black,
                    color = ArcadePrimary,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (wonSlice != null) {
                        "Tebrikler! ${wonSlice?.label} Kazandın! 🎉"
                    } else if (canSpin) {
                        "Çarkı çevirerek her gün ücretsiz ödülünü kap!"
                    } else {
                        "Bugünkü hakkını kullandın! Sonraki çevirme: $remainingTime"
                    },
                    color = if (wonSlice != null) ArcadeTertiary else Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (wonSlice != null) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Wheel graphic
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .border(4.dp, ArcadePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotationAnim.value)
                    ) {
                        val sliceAngle = 360f / slices.size
                        for (i in slices.indices) {
                            val startAngle = i * sliceAngle - 90f - sliceAngle / 2f
                            drawArc(
                                color = slices[i].color,
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true
                            )
                        }
                    }

                    // Labels on wheel
                    for (i in slices.indices) {
                        val sliceAngle = 360f / slices.size
                        val angle = rotationAnim.value + i * sliceAngle
                        val rad = Math.toRadians((angle - 90.0)).toFloat()
                        val dist = 65f
                        val x = dist * cos(rad)
                        val y = dist * sin(rad)
                        Box(
                            modifier = Modifier
                                .offset(x = x.dp, y = y.dp)
                        ) {
                            Text(
                                text = slices[i].label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }

                    // Center Hub
                    Surface(
                        shape = CircleShape,
                        color = ArcadeBackground,
                        border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "⭐", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pointer needle indicator
                Text(text = "▲ KAZANILAN DİLİM", color = ArcadeSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            if (wonSlice != null || !canSpin) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ArcadeSecondary)
                ) {
                    Text(text = "Kapat", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        if (!isSpinning && canSpin) {
                            isSpinning = true
                            scope.launch {
                                val targetIndex = slices.indices.random()
                                val sliceAngle = 360f / slices.size
                                val extraSpins = (5..8).random() * 360f
                                val targetDeg = extraSpins + (slices.size - targetIndex) * sliceAngle
                                rotationAnim.animateTo(
                                    targetValue = targetDeg,
                                    animationSpec = tween(
                                        durationMillis = 3500,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                                val chosen = slices[targetIndex]
                                wonSlice = chosen
                                isSpinning = false
                                onSpinClaim(chosen.coins, chosen.booster)
                            }
                        }
                    },
                    enabled = !isSpinning && canSpin,
                    colors = ButtonDefaults.buttonColors(containerColor = ArcadePrimary),
                    modifier = Modifier.testTag("spin_wheel_button")
                ) {
                    Text(
                        text = if (isSpinning) "Dönüyor..." else "ÇEVİR 🎡",
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    )
}

@Composable
private fun InfoItem(icon: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
