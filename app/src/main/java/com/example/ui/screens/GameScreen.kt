package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameProgressEntity
import com.example.game.*
import com.example.ui.AppScreen
import com.example.ui.GameViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    gameProgress: GameProgressEntity
) {
    val engine = viewModel.gameEngine
    var showPauseDialog by remember { mutableStateOf(false) }
    var frameTick by remember { mutableLongStateOf(0L) }
    val currentScore by viewModel.gameScore.collectAsState()

    BackHandler {
        showPauseDialog = true
    }

    // Continuous 60fps game loop
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { currentNanos ->
                val deltaMs = ((currentNanos - lastTime) / 1_000_000L).coerceIn(1L, 40L)
                lastTime = currentNanos
                engine.updatePhysics(deltaMs)
                frameTick++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArcadeBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD
            GameTopHud(
                gameMode = engine.gameMode,
                levelOrWave = if (engine.gameMode == GameMode.STAGE) engine.currentLevel else engine.currentWave,
                score = currentScore,
                coins = gameProgress.coins,
                turnsUsed = engine.turnsUsed,
                parTurns = engine.parTurns,
                onPauseClick = {
                    viewModel.soundManager.playClick()
                    showPauseDialog = true
                }
            )

            // Game Board Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GameBoardCanvas(
                    engine = engine,
                    frameTick = frameTick,
                    modifier = Modifier.fillMaxSize()
                )

                if (engine.turnState == TurnState.AIMING) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "🖱️ Parmağınla / fareyle tıkla veya sürükleyip bırak!",
                            color = ArcadePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Overdrive Bar & Activation (Unique original feature!)
            if (engine.isOverdriveActive) {
                Surface(
                    color = ArcadeSecondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ÖFKE MODU AKTİF! DELİP GEÇEN ATEŞ TOPLARI (${(engine.overdriveRemainingMs / 1000).coerceAtLeast(1)}s)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            } else if (engine.overdriveEnergy >= 100f) {
                Surface(
                    color = ArcadeTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.activateOverdrive() }
                        .testTag("activate_overdrive_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ÖFKE MODU HAZIR! DOKUN VE PATLAT! 💥",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            } else if (engine.overdriveEnergy > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(ArcadeSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (engine.overdriveEnergy / 100f).coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(ArcadeSecondary, ArcadeTertiary)
                                )
                            )
                    )
                }
            }

            // Aim controls & Direct Shoot button when in AIMING mode
            if (engine.turnState == TurnState.AIMING) {
                AimControlBar(
                    engine = engine,
                    onRotateLeft = {
                        viewModel.soundManager.playClick()
                        engine.rotateAim(-4f)
                    },
                    onRotateRight = {
                        viewModel.soundManager.playClick()
                        engine.rotateAim(4f)
                    },
                    onShoot = {
                        viewModel.soundManager.playClick()
                        engine.shootCurrentAim()
                    },
                    onToggleReleaseToShoot = {
                        viewModel.toggleReleaseToShoot()
                    }
                )
            }

            // Bottom Booster & Control Bar
            GameBottomBar(
                engine = engine,
                bombCount = gameProgress.bombBoosters,
                laserCount = gameProgress.laserBoosters,
                earthquakeCount = gameProgress.earthquakeBoosters,
                onToggleFastForward = {
                    viewModel.soundManager.playClick()
                    engine.toggleFastForward()
                },
                onRecall = {
                    viewModel.soundManager.playClick()
                    engine.recallBalls()
                },
                onUseBomb = { viewModel.useBombBooster() },
                onUseLaser = { viewModel.useLaserBooster() },
                onUseEarthquake = { viewModel.useEarthquakeBooster() }
            )
        }

        // Overlay: Pause Dialog
        if (showPauseDialog) {
            GamePauseDialog(
                soundEnabled = gameProgress.soundEnabled,
                vibrationEnabled = gameProgress.vibrationEnabled,
                onResume = { showPauseDialog = false },
                onRestart = {
                    showPauseDialog = false
                    viewModel.restartCurrentGame()
                },
                onExitToMenu = {
                    showPauseDialog = false
                    viewModel.navigateTo(AppScreen.MAIN_MENU)
                },
                onToggleSound = { viewModel.toggleSound() },
                onToggleVibration = { viewModel.toggleVibration() }
            )
        }

        // Overlay: Game Over Dialog
        if (engine.turnState == TurnState.GAME_OVER) {
            GameOverDialog(
                score = currentScore,
                wave = engine.currentWave,
                highScore = gameProgress.endlessHighScore,
                onRestart = { viewModel.restartCurrentGame() },
                onMenu = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
            )
        }

        // Overlay: Victory Dialog
        if (engine.turnState == TurnState.VICTORY) {
            val stars = when {
                engine.turnsUsed <= engine.parTurns -> 3
                engine.turnsUsed <= engine.parTurns + 3 -> 2
                else -> 1
            }
            VictoryDialog(
                level = engine.currentLevel,
                stars = stars,
                score = currentScore,
                coinsEarned = stars * 25 + 50,
                onNextLevel = { viewModel.nextLevel() },
                onReplay = { viewModel.restartCurrentGame() },
                onMenu = { viewModel.navigateTo(AppScreen.LEVEL_SELECT) }
            )
        }
    }
}

@Composable
private fun GameTopHud(
    gameMode: GameMode,
    levelOrWave: Int,
    score: Int,
    coins: Int,
    turnsUsed: Int,
    parTurns: Int,
    onPauseClick: () -> Unit
) {
    Surface(
        color = ArcadeSurface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pause button
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ArcadeSurfaceVariant)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Duraklat",
                    tint = Color.White
                )
            }

            // Mode & Level info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ArcadeSurfaceVariant
                ) {
                    Text(
                        text = if (gameMode == GameMode.STAGE) "BÖLÜM $levelOrWave" else "DALGA $levelOrWave",
                        color = ArcadePrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                if (gameMode == GameMode.STAGE) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val stars = when {
                            turnsUsed <= parTurns -> 3
                            turnsUsed <= parTurns + 3 -> 2
                            else -> 1
                        }
                        for (i in 1..3) {
                            Text(
                                text = "★",
                                fontSize = 14.sp,
                                color = if (i <= stars) ArcadeTertiary else Color.Gray
                            )
                        }
                    }
                }
            }

            // Score & Coins
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$score",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪙", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$coins",
                        color = ArcadeTertiary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GameBoardCanvas(
    engine: GameEngine,
    frameTick: Long,
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    val iconPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    val floatingPaint = remember {
        Paint().apply {
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    engine.isAiming = true
                    engine.updateAim(down.position.x, down.position.y)

                    while (true) {
                        val event = awaitPointerEvent()
                        val active = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull { it.pressed }
                        if (active == null || !active.pressed) {
                            engine.releaseAim()
                            break
                        } else {
                            active.consume()
                            engine.updateAim(active.position.x, active.position.y)
                        }
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        engine.setupBoardDimensions(w, h)

        // Draw background grid lines for arcade vibe
        val gridStroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f), 0f))
        for (i in 1 until engine.gridCols) {
            val gx = i * engine.cellWidth
            drawLine(
                color = Color(0x15FFFFFF),
                start = Offset(gx, engine.topMargin),
                end = Offset(gx, engine.bottomBaseline),
                strokeWidth = 1f
            )
        }

        // Draw red glowing Deadline line
        val deadlineColor = DangerRed.copy(alpha = 0.8f)
        drawLine(
            color = deadlineColor,
            start = Offset(0f, engine.deadlineY),
            end = Offset(w, engine.deadlineY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
        )

        // Draw bottom baseline
        drawLine(
            color = Color(0x40FFFFFF),
            start = Offset(0f, engine.bottomBaseline),
            end = Offset(w, engine.bottomBaseline),
            strokeWidth = 2f
        )

        // Draw Bricks
        for (brick in engine.bricks) {
            if (brick.isDestroyed) continue
            val rect = engine.getBrickRect(brick)
            val baseColor = brick.getBaseColor()

            // Scale for hit pulse
            val cx = rect.center.x
            val cy = rect.center.y
            val scale = brick.pulseScale
            val bw = rect.width * scale
            val bh = rect.height * scale
            val bLeft = cx - bw / 2f
            val bTop = cy - bh / 2f

            // Brick background rounded rect
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor,
                        baseColor.copy(alpha = 0.75f)
                    ),
                    startY = bTop,
                    endY = bTop + bh
                ),
                topLeft = Offset(bLeft, bTop),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Brick outline border
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(bLeft, bTop),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 1.5f)
            )

            // White flash on hit
            if (brick.hitFlashAlpha > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = brick.hitFlashAlpha),
                    topLeft = Offset(bLeft, bTop),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }

            // Draw Brick Content (HP or Item Icon)
            when (brick.type) {
                BrickType.NORMAL -> {
                    textPaint.textSize = (bh * 0.42f).coerceIn(16f, 32f)
                    val fontMetrics = textPaint.fontMetrics
                    val textBaselineY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
                    drawContext.canvas.nativeCanvas.drawText(
                        "${brick.hp}",
                        cx,
                        textBaselineY,
                        textPaint
                    )
                }
                BrickType.ADD_BALL -> {
                    // Glowing circle with +1
                    drawCircle(
                        color = Color.White,
                        radius = bh * 0.32f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5f)
                    )
                    textPaint.textSize = (bh * 0.35f).coerceIn(14f, 26f)
                    val fm = textPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("+1", cx, cy - (fm.ascent + fm.descent) / 2f, textPaint)
                }
                BrickType.BOMB -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("💣", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
                BrickType.HORIZ_LASER -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("⚡", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
                BrickType.VERT_LASER -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("↕", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
                BrickType.CROSS_LASER -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("✚", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
                BrickType.COIN -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("🪙", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
                BrickType.SPLIT_BALL -> {
                    textPaint.textSize = (bh * 0.35f).coerceIn(14f, 26f)
                    val fm = textPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("✕2", cx, cy - (fm.ascent + fm.descent) / 2f, textPaint)
                }
                BrickType.AMULET -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("🧿", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
                BrickType.VORTEX -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("🌀", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
                BrickType.JACKPOT -> {
                    iconPaint.textSize = (bh * 0.45f).coerceIn(18f, 32f)
                    val fm = iconPaint.fontMetrics
                    drawContext.canvas.nativeCanvas.drawText("💰", cx, cy - (fm.ascent + fm.descent) / 2f, iconPaint)
                }
            }
        }

        // Draw Aim Trajectory Line with Glowing Dotted Circles
        if (engine.aimPoints.size >= 2 && engine.turnState == TurnState.AIMING) {
            for (pIndex in 0 until engine.aimPoints.size - 1) {
                val start = engine.aimPoints[pIndex]
                val end = engine.aimPoints[pIndex + 1]

                // Draw laser line segments
                drawLine(
                    color = ArcadePrimary.copy(alpha = 0.7f),
                    start = start,
                    end = end,
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )

                // Dotted circles along the path
                val segDx = end.x - start.x
                val segDy = end.y - start.y
                val segLen = kotlin.math.sqrt(segDx * segDx + segDy * segDy)
                val dotSpacing = 30f
                var dist = 0f
                while (dist <= segLen) {
                    val frac = dist / segLen
                    val px = start.x + segDx * frac
                    val py = start.y + segDy * frac
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = 3.5f,
                        center = Offset(px, py)
                    )
                    dist += dotSpacing
                }
            }

            // Target cursor circle at the final point
            val lastPoint = engine.aimPoints.last()
            drawCircle(
                color = ArcadePrimary,
                radius = 12f,
                center = lastPoint,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = lastPoint
            )
        }

        // Draw Particles
        for (particle in engine.particles) {
            if (particle.alpha <= 0f) continue
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = Offset(particle.x, particle.y)
            )
        }

        // Draw Balls
        val skin = engine.activeSkin
        for (ball in engine.balls) {
            if (!ball.isLaunched || ball.isReturned) continue

            // Trail
            for (i in ball.trail.indices) {
                val tOffset = ball.trail[i]
                val tAlpha = (i + 1f) / (ball.trail.size + 1f) * 0.4f
                drawCircle(
                    color = skin.glowColor.copy(alpha = tAlpha),
                    radius = ball.radius * 0.7f * ((i + 1f) / ball.trail.size),
                    center = tOffset
                )
            }

            // Outer glow
            drawCircle(
                color = skin.glowColor.copy(alpha = 0.35f),
                radius = ball.radius * 1.5f,
                center = Offset(ball.x, ball.y)
            )

            // Inner sphere
            drawCircle(
                color = skin.primaryColor,
                radius = ball.radius,
                center = Offset(ball.x, ball.y)
            )

            // Highlight glint
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = ball.radius * 0.35f,
                center = Offset(ball.x - ball.radius * 0.3f, ball.y - ball.radius * 0.3f)
            )
        }

        // Draw Launcher Pad / Base indicator
        val baseLaunchX = engine.launchX
        drawCircle(
            color = skin.glowColor.copy(alpha = 0.25f),
            radius = 20f,
            center = Offset(baseLaunchX, engine.bottomBaseline)
        )
        drawCircle(
            color = skin.primaryColor,
            radius = 12f,
            center = Offset(baseLaunchX, engine.bottomBaseline)
        )

        // Ball count badge next to launch position
        if (engine.turnState == TurnState.AIMING) {
            textPaint.textSize = 24f
            textPaint.color = android.graphics.Color.WHITE
            drawContext.canvas.nativeCanvas.drawText(
                "x${engine.totalBalls}",
                baseLaunchX,
                engine.bottomBaseline + 34f,
                textPaint
            )
        }

        // Draw Floating Texts
        for (ft in engine.floatingTexts) {
            if (ft.alpha <= 0f) continue
            floatingPaint.color = ft.color.copy(alpha = ft.alpha).toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                ft.text,
                ft.x,
                ft.y,
                floatingPaint
            )
        }
    }
}

@Composable
private fun GameBottomBar(
    engine: GameEngine,
    bombCount: Int,
    laserCount: Int,
    earthquakeCount: Int,
    onToggleFastForward: () -> Unit,
    onRecall: () -> Unit,
    onUseBomb: () -> Unit,
    onUseLaser: () -> Unit,
    onUseEarthquake: () -> Unit
) {
    Surface(
        color = ArcadeSurface,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 2X Fast Forward Button
            IconButton(
                onClick = onToggleFastForward,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (engine.isFastForward) ArcadeTertiary else ArcadeSurfaceVariant)
                    .testTag("fast_forward_button")
            ) {
                Text(
                    text = if (engine.isFastForward) "2X" else "1X",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (engine.isFastForward) Color.Black else Color.White
                )
            }

            // Recall Button (Instant return)
            IconButton(
                onClick = onRecall,
                enabled = engine.turnState == TurnState.SHOOTING,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (engine.turnState == TurnState.SHOOTING) ArcadePrimary else ArcadeSurfaceVariant.copy(alpha = 0.5f)
                    )
                    .testTag("recall_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Geri Çağır",
                    tint = if (engine.turnState == TurnState.SHOOTING) Color.Black else Color.Gray
                )
            }

            // Booster: Bomb
            BoosterButton(
                icon = "💣",
                count = bombCount,
                enabled = engine.turnState == TurnState.AIMING && bombCount > 0,
                tag = "booster_bomb",
                onClick = onUseBomb
            )

            // Booster: Laser
            BoosterButton(
                icon = "⚡",
                count = laserCount,
                enabled = engine.turnState == TurnState.AIMING && laserCount > 0,
                tag = "booster_laser",
                onClick = onUseLaser
            )

            // Booster: Earthquake
            BoosterButton(
                icon = "💥",
                count = earthquakeCount,
                enabled = engine.turnState == TurnState.AIMING && earthquakeCount > 0,
                tag = "booster_earthquake",
                onClick = onUseEarthquake
            )
        }
    }
}

@Composable
private fun BoosterButton(
    icon: String,
    count: Int,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (enabled) ArcadeSurfaceVariant else ArcadeSurfaceVariant.copy(alpha = 0.4f),
            border = if (enabled) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .testTag(tag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = icon, fontSize = 20.sp)
            }
        }

        // Badge
        Surface(
            shape = CircleShape,
            color = if (count > 0) ArcadeSecondary else Color.Gray,
            modifier = Modifier
                .offset(x = 4.dp, y = (-4).dp)
                .size(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AimControlBar(
    engine: GameEngine,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onShoot: () -> Unit,
    onToggleReleaseToShoot: () -> Unit
) {
    Surface(
        color = ArcadeSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Header Row: Angle Readout & Release-To-Shoot Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎯 AÇI:",
                        fontWeight = FontWeight.Bold,
                        color = ArcadePrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(-engine.aimAngleDegrees).toInt()}°",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Kaydırma Çubuğu",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                // Quick toggle chip for Release to Shoot
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (engine.releaseToShootEnabled) ArcadeTertiary.copy(alpha = 0.25f) else ArcadeSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (engine.releaseToShootEnabled) ArcadeTertiary else Color.Gray.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clickable { onToggleReleaseToShoot() }
                        .testTag("toggle_release_to_shoot")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (engine.releaseToShootEnabled) "⚡ Bırakınca Fırlat: AÇIK" else "🖐️ Düğmeyle Fırlat",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (engine.releaseToShootEnabled) ArcadeTertiary else Color.LightGray
                        )
                    }
                }
            }

            // Slider & Micro-adjustment Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRotateLeft,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ArcadeSurface)
                        .testTag("aim_rotate_left_button")
                ) {
                    Text(text = "◀", color = ArcadePrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }

                Slider(
                    value = engine.aimAngleDegrees,
                    onValueChange = { newDeg ->
                        engine.setAimAngleDegrees(newDeg)
                    },
                    onValueChangeFinished = {
                        if (engine.releaseToShootEnabled) {
                            onShoot()
                        }
                    },
                    valueRange = -170f..-10f,
                    colors = SliderDefaults.colors(
                        thumbColor = ArcadePrimary,
                        activeTrackColor = ArcadeSecondary,
                        inactiveTrackColor = ArcadeSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .testTag("aim_angle_slider")
                )

                IconButton(
                    onClick = onRotateRight,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ArcadeSurface)
                        .testTag("aim_rotate_right_button")
                ) {
                    Text(text = "▶", color = ArcadePrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }

            // Big Action Shoot Button
            Button(
                onClick = onShoot,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArcadeGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .testTag("shoot_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "🚀", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TOPLARI FIRLAT (${engine.totalBalls} Top)",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GamePauseDialog(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onResume,
        containerColor = ArcadeSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Oyun Duraklatıldı",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sound and Haptic quick toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ArcadeSurfaceVariant)
                            .size(50.dp)
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Ses",
                            tint = if (soundEnabled) ArcadePrimary else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onToggleVibration,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ArcadeSurfaceVariant)
                            .size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Titreşim",
                            tint = if (vibrationEnabled) ArcadeSecondary else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArcadePrimary)
                ) {
                    Text(text = "Devam Et", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Text(text = "Yeniden Başlat", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                TextButton(
                    onClick = onExitToMenu,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Ana Menüye Dön", color = TextSecondary)
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun GameOverDialog(
    score: Int,
    wave: Int,
    highScore: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = ArcadeSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "💥", fontSize = 42.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "OYUN BİTTİ",
                    color = DangerRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tuğlalar kırmızı çizgiye ulaştı!",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ArcadeSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Skor", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "$score",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ulaşılan Dalga: $wave",
                            color = ArcadePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (score >= highScore && score > 0) {
                            Text(
                                text = "🏆 YENİ REKOR!",
                                color = ArcadeTertiary,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("game_over_restart_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ArcadePrimary)
                ) {
                    Text(text = "Tekrar Dene", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Ana Menü", color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun VictoryDialog(
    level: Int,
    stars: Int,
    score: Int,
    coinsEarned: Int,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = ArcadeSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎉", fontSize = 42.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BÖLÜM TAMAMLANDI!",
                    color = ArcadeGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated Stars
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 1..3) {
                        Text(
                            text = "★",
                            fontSize = 38.sp,
                            color = if (i <= stars) ArcadeTertiary else Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ArcadeSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Bölüm $level Skoru", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "$score",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Ödül: +$coinsEarned", color = ArcadeTertiary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "🪙", fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (level < 20) {
                    Button(
                        onClick = onNextLevel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("victory_next_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcadeSecondary)
                    ) {
                        Text(text = "Sonraki Bölüm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = "Tekrar", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onMenu,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = "Harita", color = ArcadePrimary)
                    }
                }
            }
        }
    )
}
