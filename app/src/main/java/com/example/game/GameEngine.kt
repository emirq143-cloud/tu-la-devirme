package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.ui.theme.*
import kotlin.math.*

class GameEngine(
    val soundManager: SoundManager,
    val hapticManager: HapticManager,
    var onScoreChanged: (score: Int) -> Unit = {},
    var onCoinsEarned: (coins: Int) -> Unit = {},
    var onGameOver: (finalScore: Int, waves: Int) -> Unit = { _, _ -> },
    var onVictory: (stars: Int, score: Int, turnsUsed: Int) -> Unit = { _, _, _ -> }
) {
    var gameMode: GameMode = GameMode.STAGE
    var turnState: TurnState = TurnState.AIMING
    var currentLevel: Int = 1
    var currentWave: Int = 1
    var score: Int = 0
    var turnsUsed: Int = 0
    var parTurns: Int = 12

    var boardWidth: Float = 1000f
    var boardHeight: Float = 1400f
    var topMargin: Float = 120f
    var bottomBaseline: Float = 1250f
    var deadlineY: Float = 1150f

    val gridCols: Int = 7
    val gridRows: Int = 10
    var cellWidth: Float = 1000f / 7f
    var cellHeight: Float = 75f

    var launchX: Float = 500f
    var newLaunchX: Float? = null
    var totalBalls: Int = 15
    var pendingBallsToAdd: Int = 0

    val balls = mutableListOf<Ball>()
    val bricks = mutableListOf<Brick>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()

    var activeSkin: BallSkin = BallSkins.ALL.first()
    var isFastForward: Boolean = false
    var currentCombo: Int = 0
    var screenShake: Float = 0f

    // Unique Feature: Ateş Çılgınlığı / Overdrive (Ulti)
    var overdriveEnergy: Float = 0f // 0f to 100f
    var isOverdriveActive: Boolean = false
    var overdriveRemainingMs: Long = 0L

    // Aiming settings
    var releaseToShootEnabled: Boolean = false

    // Laser aiming state
    var isAiming: Boolean = false
    var aimAngleRad: Float = -PI.toFloat() / 2f // straight up
    var aimPoints: List<Offset> = emptyList()

    private var launchTimerMs: Long = 0L
    private var nextBallToLaunchIndex: Int = 0
    private val ballLaunchIntervalMs: Long = 55L

    fun setupBoardDimensions(width: Float, height: Float) {
        if (width <= 0 || height <= 0) return
        val sizeChanged = (boardWidth != width || boardHeight != height)
        boardWidth = width
        boardHeight = height
        topMargin = height * 0.08f
        bottomBaseline = height * 0.90f
        deadlineY = height * 0.82f

        cellWidth = boardWidth / gridCols
        cellHeight = (deadlineY - topMargin) / gridRows
        if (launchX <= 0 || launchX >= boardWidth || sizeChanged) {
            launchX = boardWidth / 2f
        }
        if (turnState == TurnState.AIMING && aimPoints.isEmpty()) {
            computeAimPrediction()
        }
    }

    fun startStage(levelNumber: Int) {
        gameMode = GameMode.STAGE
        currentLevel = levelNumber
        turnsUsed = 0
        score = 0
        currentCombo = 0
        isFastForward = false
        val layout = LevelData.getLevel(levelNumber)
        parTurns = layout.parTurns
        totalBalls = layout.initialBallCount
        pendingBallsToAdd = 0

        bricks.clear()
        balls.clear()
        particles.clear()
        floatingTexts.clear()

        layout.bricks.forEachIndexed { index, b ->
            bricks.add(
                Brick(
                    id = "b_${levelNumber}_${index}",
                    row = b.row,
                    col = b.col,
                    hp = b.hp,
                    maxHp = b.hp,
                    type = b.type
                )
            )
        }

        launchX = boardWidth / 2f
        newLaunchX = null
        aimAngleRad = (-PI / 2).toFloat()
        turnState = TurnState.AIMING
        computeAimPrediction()
        onScoreChanged(score)
    }

    fun startEndless() {
        gameMode = GameMode.ENDLESS
        currentWave = 1
        score = 0
        turnsUsed = 0
        totalBalls = 10
        pendingBallsToAdd = 0
        currentCombo = 0
        isFastForward = false

        bricks.clear()
        balls.clear()
        particles.clear()
        floatingTexts.clear()

        spawnEndlessRow(row = 2, wave = 1)
        spawnEndlessRow(row = 3, wave = 1)

        launchX = boardWidth / 2f
        newLaunchX = null
        aimAngleRad = (-PI / 2).toFloat()
        turnState = TurnState.AIMING
        computeAimPrediction()
        onScoreChanged(score)
    }

    private fun spawnEndlessRow(row: Int, wave: Int) {
        val baseHp = wave * 2 + 1
        var addedItemThisRow = false

        for (c in 0 until gridCols) {
            val rand = Math.random()
            when {
                rand < 0.40 -> {
                    // Normal brick
                    val hp = baseHp + (if (Math.random() < 0.25) wave else 0)
                    bricks.add(
                        Brick(
                            id = "e_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = hp,
                            maxHp = hp,
                            type = BrickType.NORMAL
                        )
                    )
                }
                rand < 0.52 && !addedItemThisRow -> {
                    // +1 Ball item
                    bricks.add(
                        Brick(
                            id = "e_add_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = 1,
                            maxHp = 1,
                            type = BrickType.ADD_BALL
                        )
                    )
                    addedItemThisRow = true
                }
                rand < 0.60 -> {
                    // Bomb
                    bricks.add(
                        Brick(
                            id = "e_bomb_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = maxOf(1, wave),
                            maxHp = maxOf(1, wave),
                            type = BrickType.BOMB
                        )
                    )
                }
                rand < 0.68 -> {
                    // Laser
                    val type = if (Math.random() < 0.5) BrickType.HORIZ_LASER else BrickType.VERT_LASER
                    bricks.add(
                        Brick(
                            id = "e_laser_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = maxOf(1, wave),
                            maxHp = maxOf(1, wave),
                            type = type
                        )
                    )
                }
                rand < 0.74 -> {
                    // Coin
                    bricks.add(
                        Brick(
                            id = "e_coin_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = 1,
                            maxHp = 1,
                            type = BrickType.COIN
                        )
                    )
                }
                rand < 0.79 -> {
                    // Nazar Bloğu (AMULET 🧿)
                    bricks.add(
                        Brick(
                            id = "e_amulet_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = 1,
                            maxHp = 1,
                            type = BrickType.AMULET
                        )
                    )
                }
                rand < 0.84 -> {
                    // Kozmik Girdap (VORTEX 🌀)
                    bricks.add(
                        Brick(
                            id = "e_vortex_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = maxOf(2, wave),
                            maxHp = maxOf(2, wave),
                            type = BrickType.VORTEX
                        )
                    )
                }
                rand < 0.88 -> {
                    // Altın Kasası (JACKPOT 💰)
                    bricks.add(
                        Brick(
                            id = "e_jackpot_${wave}_${row}_${c}",
                            row = row,
                            col = c,
                            hp = maxOf(3, wave),
                            maxHp = maxOf(3, wave),
                            type = BrickType.JACKPOT
                        )
                    )
                }
            }
        }
    }

    fun updateAim(touchX: Float, touchY: Float) {
        if (turnState != TurnState.AIMING) return

        var dirX = touchX - launchX
        var dirY = touchY - bottomBaseline

        // Support slingshot pull if touch is at or below baseline
        if (dirY >= -40f) {
            dirX = -(touchX - launchX)
            dirY = -(touchY - bottomBaseline)
        }

        // Always point upwards towards bricks
        if (dirY >= 0f) {
            dirY = -abs(dirY).coerceAtLeast(30f)
        }

        val len = sqrt(dirX * dirX + dirY * dirY)
        if (len > 8f) {
            var normX = dirX / len
            var normY = dirY / len
            // Ensure angle doesn't become completely horizontal (at least ~10 degrees upward)
            if (normY > -0.17f) {
                normY = -0.17f
                normX = if (normX >= 0) sqrt(1f - normY * normY) else -sqrt(1f - normY * normY)
            }
            aimAngleRad = atan2(normY, normX)
            computeAimPrediction()
        }
    }

    val aimAngleDegrees: Float
        get() {
            var deg = Math.toDegrees(aimAngleRad.toDouble()).toFloat()
            if (deg > 0) deg -= 360f
            return deg.coerceIn(-170f, -10f)
        }

    fun setAimAngleDegrees(deg: Float) {
        if (turnState != TurnState.AIMING) return
        val clamped = deg.coerceIn(-170f, -10f)
        aimAngleRad = Math.toRadians(clamped.toDouble()).toFloat()
        computeAimPrediction()
    }

    fun rotateAim(deltaDegrees: Float) {
        if (turnState != TurnState.AIMING) return
        val deltaRad = Math.toRadians(deltaDegrees.toDouble()).toFloat()
        val newAngle = aimAngleRad + deltaRad
        // Constrain between ~170 deg and ~10 deg upward
        val minAngle = (-170.0 * Math.PI / 180.0).toFloat()
        val maxAngle = (-10.0 * Math.PI / 180.0).toFloat()
        aimAngleRad = newAngle.coerceIn(minAngle, maxAngle)
        computeAimPrediction()
    }

    fun shootCurrentAim() {
        if (turnState != TurnState.AIMING) return
        releaseAim()
    }

    private fun computeAimPrediction() {
        val points = mutableListOf<Offset>()
        var startX = launchX
        var startY = bottomBaseline
        points.add(Offset(startX, startY))

        var curDirX = cos(aimAngleRad)
        var curDirY = sin(aimAngleRad)
        val maxBounces = 2
        var bounces = 0

        val step = 15f
        var traveled = 0f
        val maxDistance = 2500f

        while (traveled < maxDistance && bounces <= maxBounces) {
            val nextX = startX + curDirX * step
            val nextY = startY + curDirY * step
            traveled += step

            // Check left / right wall
            if (nextX <= 12f) {
                points.add(Offset(12f, nextY))
                startX = 12f
                startY = nextY
                curDirX = -curDirX
                bounces++
                continue
            }
            if (nextX >= boardWidth - 12f) {
                points.add(Offset(boardWidth - 12f, nextY))
                startX = boardWidth - 12f
                startY = nextY
                curDirX = -curDirX
                bounces++
                continue
            }
            // Check top wall
            if (nextY <= topMargin) {
                points.add(Offset(nextX, topMargin))
                startX = nextX
                startY = topMargin
                curDirY = -curDirY
                bounces++
                continue
            }

            // Check brick hit
            var hitBrick = false
            for (brick in bricks) {
                if (brick.isDestroyed) continue
                val rect = getBrickRect(brick)
                if (rect.contains(Offset(nextX, nextY))) {
                    points.add(Offset(nextX, nextY))
                    hitBrick = true
                    break
                }
            }

            if (hitBrick) break

            startX = nextX
            startY = nextY
        }
        points.add(Offset(startX, startY))
        aimPoints = points
    }

    fun releaseAim() {
        if (turnState != TurnState.AIMING) return
        turnState = TurnState.SHOOTING
        currentCombo = 0
        turnsUsed++
        nextBallToLaunchIndex = 0
        launchTimerMs = 0L
        newLaunchX = null

        val speed = 24f
        val vx = cos(aimAngleRad) * speed
        val vy = sin(aimAngleRad) * speed

        balls.clear()
        for (i in 0 until totalBalls) {
            balls.add(
                Ball(
                    id = i,
                    x = launchX,
                    y = bottomBaseline,
                    vx = vx,
                    vy = vy,
                    radius = 11f,
                    isLaunched = false,
                    isReturned = false
                )
            )
        }
        aimPoints = emptyList()
    }

    fun recallBalls() {
        if (turnState != TurnState.SHOOTING) return
        turnState = TurnState.RECALLING
        val targetX = newLaunchX ?: launchX
        for (ball in balls) {
            ball.vx = (targetX - ball.x) * 0.15f
            ball.vy = (bottomBaseline - ball.y) * 0.25f
        }
    }

    fun toggleFastForward() {
        isFastForward = !isFastForward
    }

    fun updatePhysics(deltaMs: Long) {
        if (turnState == TurnState.PAUSED || turnState == TurnState.GAME_OVER || turnState == TurnState.VICTORY) {
            return
        }

        if (screenShake > 0f) {
            screenShake = maxOf(0f, screenShake - 0.08f)
        }

        // Update particles
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.12f // slight gravity
            p.life -= p.decay
            p.alpha = maxOf(0f, p.life)
            p.radius = p.initialRadius * p.life
            if (p.life <= 0f) {
                particleIterator.remove()
            }
        }

        // Update floating texts
        val textIterator = floatingTexts.iterator()
        while (textIterator.hasNext()) {
            val t = textIterator.next()
            t.y += t.vy
            t.alpha -= 0.025f
            if (t.alpha <= 0f) {
                textIterator.remove()
            }
        }

        // Update brick hit flashes / pulse scales
        for (b in bricks) {
            if (b.pulseScale > 1.0f) {
                b.pulseScale = maxOf(1.0f, b.pulseScale - 0.05f)
            }
            if (b.hitFlashAlpha > 0f) {
                b.hitFlashAlpha = maxOf(0f, b.hitFlashAlpha - 0.08f)
            }
        }

        // Update Overdrive state
        if (isOverdriveActive) {
            overdriveRemainingMs -= deltaMs
            if (overdriveRemainingMs <= 0L) {
                isOverdriveActive = false
                floatingTexts.add(FloatingText("Çılgınlık Sona Erdi", boardWidth / 2f, boardHeight / 2f, color = TextSecondary))
            }
        }

        if (turnState == TurnState.SHOOTING || turnState == TurnState.RECALLING) {
            // Launch remaining balls with interval
            if (turnState == TurnState.SHOOTING && nextBallToLaunchIndex < balls.size) {
                launchTimerMs += deltaMs * (if (isFastForward) 2 else 1)
                val interval = if (isFastForward) ballLaunchIntervalMs / 2 else ballLaunchIntervalMs
                while (launchTimerMs >= interval && nextBallToLaunchIndex < balls.size) {
                    balls[nextBallToLaunchIndex].isLaunched = true
                    nextBallToLaunchIndex++
                    launchTimerMs -= interval
                }
            }

            val steps = if (isFastForward) 3 else 2
            val dtFactor = (if (isFastForward) 1.5f else 1.0f) / steps

            for (step in 0 until steps) {
                var activeBallsCount = 0

                for (ball in balls) {
                    if (!ball.isLaunched || ball.isReturned) continue
                    activeBallsCount++

                    if (turnState == TurnState.RECALLING) {
                        val targetX = newLaunchX ?: launchX
                        ball.x += (targetX - ball.x) * 0.12f
                        ball.y += (bottomBaseline - ball.y) * 0.15f + 10f
                        if (ball.y >= bottomBaseline) {
                            ball.y = bottomBaseline
                            ball.isReturned = true
                            if (newLaunchX == null) newLaunchX = ball.x
                        }
                        continue
                    }

                    // Move ball
                    ball.x += ball.vx * dtFactor
                    ball.y += ball.vy * dtFactor

                    // Unique Feature: Gravitational pull from active VORTEX (Kozmik Girdap) bricks
                    for (vb in bricks) {
                        if (vb.isDestroyed || vb.type != BrickType.VORTEX) continue
                        val vRect = getBrickRect(vb)
                        val vDx = vRect.center.x - ball.x
                        val vDy = vRect.center.y - ball.y
                        val vDist = sqrt(vDx * vDx + vDy * vDy)
                        if (vDist in 15f..160f) {
                            val pull = (0.35f * (1f - vDist / 160f)) * dtFactor
                            ball.vx += (vDx / vDist) * pull
                            ball.vy += (vDy / vDist) * pull
                        }
                    }

                    // Update trail
                    ball.trail.add(Offset(ball.x, ball.y))
                    if (ball.trail.size > 5) {
                        ball.trail.removeAt(0)
                    }

                    // Overdrive fiery particle bursts
                    if (isOverdriveActive && Math.random() < 0.25) {
                        particles.add(
                            Particle(
                                x = ball.x + (Math.random() * 6 - 3).toFloat(),
                                y = ball.y + (Math.random() * 6 - 3).toFloat(),
                                vx = (Math.random() * 2 - 1).toFloat(),
                                vy = (Math.random() * 2 - 1).toFloat(),
                                color = if (Math.random() < 0.5) ArcadeOrange else ArcadeTertiary,
                                initialRadius = 4f,
                                radius = 4f,
                                decay = 0.08f
                            )
                        )
                    }

                    // Wall collisions
                    if (ball.x - ball.radius <= 0f) {
                        ball.x = ball.radius
                        ball.vx = abs(ball.vx)
                        soundManager.playWallBounce()
                    } else if (ball.x + ball.radius >= boardWidth) {
                        ball.x = boardWidth - ball.radius
                        ball.vx = -abs(ball.vx)
                        soundManager.playWallBounce()
                    }

                    if (ball.y - ball.radius <= topMargin) {
                        ball.y = topMargin + ball.radius
                        ball.vy = abs(ball.vy)
                        soundManager.playWallBounce()
                    }

                    // Bottom line
                    if (ball.y + ball.radius >= bottomBaseline) {
                        ball.y = bottomBaseline
                        ball.isReturned = true
                        ball.vx = 0f
                        ball.vy = 0f
                        if (newLaunchX == null) {
                            newLaunchX = ball.x
                        }
                        continue
                    }

                    // Brick collisions
                    checkBallBrickCollisions(ball)
                }

                // Check if all balls returned
                val allReturned = balls.all { it.isReturned || !it.isLaunched } && nextBallToLaunchIndex >= balls.size
                if (allReturned && activeBallsCount == 0) {
                    finishRound()
                    break
                }
            }
        }
    }

    private fun checkBallBrickCollisions(ball: Ball) {
        for (brick in bricks) {
            if (brick.isDestroyed) continue
            val rect = getBrickRect(brick)

            // Circle-Rect collision
            val closestX = ball.x.coerceIn(rect.left, rect.right)
            val closestY = ball.y.coerceIn(rect.top, rect.bottom)
            val dx = ball.x - closestX
            val dy = ball.y - closestY
            val distSq = dx * dx + dy * dy

            if (distSq <= ball.radius * ball.radius) {
                // Collision happened!
                // Determine collision normal
                val overlapLeft = (ball.x + ball.radius) - rect.left
                val overlapRight = rect.right - (ball.x - ball.radius)
                val overlapTop = (ball.y + ball.radius) - rect.top
                val overlapBottom = rect.bottom - (ball.y - ball.radius)

                val minOverlapX = minOf(overlapLeft, overlapRight)
                val minOverlapY = minOf(overlapTop, overlapBottom)

                if (isOverdriveActive) {
                    // Overdrive pierce! Balls blast through bricks without bouncing away
                    hitBrick(brick, ball, damageMultiplier = 3)
                } else {
                    if (minOverlapX < minOverlapY) {
                        // Horizontal bounce
                        ball.vx = if (dx > 0) abs(ball.vx) else -abs(ball.vx)
                    } else {
                        // Vertical bounce
                        ball.vy = if (dy > 0) abs(ball.vy) else -abs(ball.vy)
                    }
                    hitBrick(brick, ball, damageMultiplier = 1)
                }
                break // handle one brick collision per sub-step
            }
        }
    }

    private fun hitBrick(brick: Brick, ball: Ball, damageMultiplier: Int = 1) {
        currentCombo++
        brick.hp -= damageMultiplier
        brick.pulseScale = 1.25f
        brick.hitFlashAlpha = 0.8f

        // Charge Overdrive energy
        if (!isOverdriveActive) {
            overdriveEnergy = (overdriveEnergy + 1.2f).coerceAtMost(100f)
        }

        // Jackpot brick gives coins on each hit!
        if (brick.type == BrickType.JACKPOT) {
            onCoinsEarned(5)
            soundManager.playBallPickup()
            floatingTexts.add(FloatingText("+5 🪙", ball.x, ball.y - 10f, color = ArcadeTertiary))
        }

        // Dynamic Combo Hype Announcer Callouts!
        when (currentCombo) {
            10 -> floatingTexts.add(FloatingText("HARİKA! x10", ball.x, ball.y - 20f, color = ArcadePrimary))
            25 -> {
                floatingTexts.add(FloatingText("MUHTEŞEM! x25", ball.x, ball.y - 25f, color = ArcadeTertiary))
                soundManager.playVictoryFanfare()
            }
            50 -> {
                floatingTexts.add(FloatingText("ÇILGINLIK! x50 🔥", boardWidth / 2f, boardHeight / 2f, color = ArcadeSecondary))
                screenShake = 0.8f
            }
            80 -> {
                floatingTexts.add(FloatingText("EFSANE VURUŞ! ⚡", boardWidth / 2f, boardHeight / 2f, color = Color(0xFFFFD600)))
                screenShake = 1.0f
            }
        }

        soundManager.playBrickHit(currentCombo)
        hapticManager.vibrateHit()

        val hitPoints = 10 * minOf(10, (1 + currentCombo / 5)) * damageMultiplier
        score += hitPoints
        onScoreChanged(score)

        if (brick.hp <= 0) {
            destroyBrick(brick)
        }
    }

    private fun destroyBrick(brick: Brick) {
        brick.isDestroyed = true
        hapticManager.vibrateBreak()

        val rect = getBrickRect(brick)
        val cx = rect.center.x
        val cy = rect.center.y

        // Spawn particles
        val color = brick.getBaseColor()
        for (i in 0 until 12) {
            val angle = (Math.PI * 2 * Math.random()).toFloat()
            val speed = (3f + Math.random() * 7f).toFloat()
            particles.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = color,
                    initialRadius = (4f + Math.random() * 5f).toFloat(),
                    radius = 6f
                )
            )
        }

        // Special brick actions
        when (brick.type) {
            BrickType.ADD_BALL -> {
                pendingBallsToAdd++
                soundManager.playBallPickup()
                floatingTexts.add(FloatingText("+1 Top!", cx, cy, color = ArcadeGreen))
            }
            BrickType.BOMB -> {
                detonateBomb(brick.row, brick.col)
            }
            BrickType.HORIZ_LASER -> {
                fireHorizontalLaser(brick.row, cy)
            }
            BrickType.VERT_LASER -> {
                fireVerticalLaser(brick.col, cx)
            }
            BrickType.CROSS_LASER -> {
                fireHorizontalLaser(brick.row, cy)
                fireVerticalLaser(brick.col, cx)
            }
            BrickType.COIN -> {
                val coins = 5
                onCoinsEarned(coins)
                soundManager.playBallPickup()
                floatingTexts.add(FloatingText("+$coins 🪙", cx, cy, color = ArcadeTertiary))
            }
            BrickType.SPLIT_BALL -> {
                splitBalls()
            }
            BrickType.AMULET -> {
                // Unique Feature: Nazar Boncuğu (🧿) strike
                soundManager.playVictoryFanfare()
                hapticManager.vibrateExplosion()
                screenShake = 0.8f
                floatingTexts.add(FloatingText("🧿 NAZAR KALKANI!", cx, cy, color = Color(0xFF1E88E5)))

                val otherBricks = bricks.filter { !it.isDestroyed && it.type != BrickType.ADD_BALL && it.id != brick.id }
                val targets = otherBricks.shuffled().take(3)
                for (target in targets) {
                    target.hp -= 25
                    target.pulseScale = 1.4f
                    target.hitFlashAlpha = 0.9f
                    // Cyan mystical strike particles
                    val tRect = getBrickRect(target)
                    for (k in 0 until 8) {
                        particles.add(
                            Particle(
                                x = tRect.center.x,
                                y = tRect.center.y,
                                vx = (Math.random() * 6 - 3).toFloat(),
                                vy = (Math.random() * 6 - 3).toFloat(),
                                color = Color(0xFF00E5FF),
                                initialRadius = 5f,
                                radius = 5f
                            )
                        )
                    }
                    if (target.hp <= 0) {
                        destroyBrick(target)
                    }
                }
            }
            BrickType.VORTEX -> {
                // Unique Feature: Kozmik Girdap patlaması
                soundManager.playLaserZap()
                screenShake = 0.7f
                floatingTexts.add(FloatingText("🌀 GİRDAP ŞOKU!", cx, cy, color = ArcadePurple))
                detonateBomb(brick.row, brick.col)
            }
            BrickType.JACKPOT -> {
                // Unique Feature: Altın Kasası patlaması
                val jackpotCoins = 25
                onCoinsEarned(jackpotCoins)
                soundManager.playVictoryFanfare()
                floatingTexts.add(FloatingText("💰 BÜYÜK KASA! +$jackpotCoins 🪙", cx, cy, color = ArcadeTertiary))
                for (k in 0 until 16) {
                    particles.add(
                        Particle(
                            x = cx,
                            y = cy,
                            vx = (Math.random() * 8 - 4).toFloat(),
                            vy = (Math.random() * 8 - 4).toFloat(),
                            color = ArcadeTertiary,
                            initialRadius = 6f,
                            radius = 6f
                        )
                    )
                }
            }
            BrickType.NORMAL -> {
                // Standard smash
            }
        }

        // Check victory in stage mode
        checkVictoryCondition()
    }

    private fun detonateBomb(row: Int, col: Int) {
        soundManager.playExplosion()
        hapticManager.vibrateExplosion()
        screenShake = 1.0f

        val rect = getBrickRect(row, col)
        floatingTexts.add(FloatingText("BOOM!", rect.center.x, rect.center.y, color = DangerRed))

        for (r in (row - 1)..(row + 1)) {
            for (c in (col - 1)..(col + 1)) {
                val target = bricks.find { it.row == r && it.col == c && !it.isDestroyed }
                if (target != null) {
                    target.hp -= 20
                    if (target.hp <= 0) {
                        destroyBrick(target)
                    } else {
                        target.pulseScale = 1.3f
                        target.hitFlashAlpha = 0.9f
                    }
                }
            }
        }
    }

    private fun fireHorizontalLaser(row: Int, laserY: Float) {
        soundManager.playLaserZap()
        screenShake = 0.5f
        floatingTexts.add(FloatingText("LAZER!", boardWidth / 2f, laserY, color = ArcadeSecondary))

        // Particles across the row
        for (i in 0 until 20) {
            particles.add(
                Particle(
                    x = (boardWidth * (i / 20f)),
                    y = laserY + (Math.random() * 10 - 5).toFloat(),
                    vx = (Math.random() * 8 - 4).toFloat(),
                    vy = (Math.random() * 4 - 2).toFloat(),
                    color = ArcadeSecondary,
                    initialRadius = 5f,
                    radius = 5f
                )
            )
        }

        for (brick in bricks) {
            if (brick.row == row && !brick.isDestroyed) {
                brick.hp -= 25
                if (brick.hp <= 0) {
                    destroyBrick(brick)
                } else {
                    brick.pulseScale = 1.3f
                }
            }
        }
    }

    private fun fireVerticalLaser(col: Int, laserX: Float) {
        soundManager.playLaserZap()
        screenShake = 0.5f

        for (brick in bricks) {
            if (brick.col == col && !brick.isDestroyed) {
                brick.hp -= 25
                if (brick.hp <= 0) {
                    destroyBrick(brick)
                } else {
                    brick.pulseScale = 1.3f
                }
            }
        }
    }

    private fun splitBalls() {
        soundManager.playLaserZap()
        val newBalls = mutableListOf<Ball>()
        for (b in balls.filter { it.isLaunched && !it.isReturned }.take(4)) {
            val angle = atan2(b.vy, b.vx)
            val speed = sqrt(b.vx * b.vx + b.vy * b.vy)
            val angle1 = angle + 0.35f
            val angle2 = angle - 0.35f
            newBalls.add(
                Ball(
                    id = balls.size + newBalls.size,
                    x = b.x,
                    y = b.y,
                    vx = cos(angle1) * speed,
                    vy = sin(angle1) * speed,
                    radius = b.radius,
                    isLaunched = true,
                    isReturned = false
                )
            )
            newBalls.add(
                Ball(
                    id = balls.size + newBalls.size + 1,
                    x = b.x,
                    y = b.y,
                    vx = cos(angle2) * speed,
                    vy = sin(angle2) * speed,
                    radius = b.radius,
                    isLaunched = true,
                    isReturned = false
                )
            )
        }
        balls.addAll(newBalls)
    }

    private fun finishRound() {
        turnState = TurnState.ROUND_RESOLVING
        launchX = (newLaunchX ?: launchX).coerceIn(40f, boardWidth - 40f)
        newLaunchX = null

        totalBalls += pendingBallsToAdd
        pendingBallsToAdd = 0

        if (gameMode == GameMode.ENDLESS) {
            // Shift all bricks down by 1 row
            var reachedDeadline = false
            for (brick in bricks) {
                if (!brick.isDestroyed) {
                    val nextRow = brick.row + 1
                    brick.copy() // ensure state
                    if (getBrickRect(nextRow, brick.col).bottom >= deadlineY) {
                        reachedDeadline = true
                    }
                }
            }

            if (reachedDeadline) {
                turnState = TurnState.GAME_OVER
                soundManager.playExplosion()
                hapticManager.vibrateGameOver()
                onGameOver(score, currentWave)
                return
            }

            // Apply row shift
            val updatedBricks = mutableListOf<Brick>()
            for (brick in bricks) {
                if (!brick.isDestroyed) {
                    updatedBricks.add(
                        brick.copy(row = brick.row + 1)
                    )
                }
            }
            bricks.clear()
            bricks.addAll(updatedBricks)

            // Spawn new row at row 1
            currentWave++
            spawnEndlessRow(row = 1, wave = currentWave)
            turnState = TurnState.AIMING
            computeAimPrediction()
        } else {
            // Stage mode
            if (checkVictoryCondition()) return
            turnState = TurnState.AIMING
            computeAimPrediction()
        }
    }

    private fun checkVictoryCondition(): Boolean {
        if (gameMode == GameMode.STAGE) {
            val remainingBricks = bricks.filter { !it.isDestroyed && it.type != BrickType.ADD_BALL && it.type != BrickType.COIN }
            if (remainingBricks.isEmpty()) {
                turnState = TurnState.VICTORY
                soundManager.playVictoryFanfare()
                hapticManager.vibrateBreak()

                val stars = when {
                    turnsUsed <= parTurns -> 3
                    turnsUsed <= parTurns + 3 -> 2
                    else -> 1
                }
                onVictory(stars, score, turnsUsed)
                return true
            }
        }
        return false
    }

    // Unique Feature: Ateş Çılgınlığı / Overdrive
    fun activateOverdrive(): Boolean {
        if (overdriveEnergy >= 100f && !isOverdriveActive) {
            isOverdriveActive = true
            overdriveRemainingMs = 7000L
            overdriveEnergy = 0f
            soundManager.playLaserZap()
            soundManager.playExplosion()
            hapticManager.vibrateExplosion()
            screenShake = 1.3f
            floatingTexts.add(FloatingText("🔥 ATEŞ ÇILGINLIĞI! 🔥", boardWidth / 2f, boardHeight / 2f, color = ArcadeOrange))
            return true
        }
        return false
    }

    fun setAimDegrees(degrees: Float) {
        if (turnState != TurnState.AIMING) return
        val clamped = degrees.coerceIn(10f, 170f)
        aimAngleRad = (-clamped * Math.PI / 180.0).toFloat()
        computeAimPrediction()
    }

    fun getAimDegrees(): Float {
        return (-aimAngleRad * 180.0 / Math.PI).toFloat().coerceIn(10f, 170f)
    }

    // Boosters
    fun triggerBombBooster() {
        val activeBricks = bricks.filter { !it.isDestroyed }
        if (activeBricks.isEmpty()) return
        val target = activeBricks.random()
        detonateBomb(target.row, target.col)
    }

    fun triggerLaserBooster() {
        val activeBricks = bricks.filter { !it.isDestroyed }
        if (activeBricks.isEmpty()) return
        val maxRow = activeBricks.maxOf { it.row }
        val rect = getBrickRect(maxRow, 0)
        fireHorizontalLaser(maxRow, rect.center.y)
    }

    fun triggerEarthquakeBooster() {
        soundManager.playExplosion()
        hapticManager.vibrateExplosion()
        screenShake = 1.5f
        floatingTexts.add(FloatingText("DEPREM!", boardWidth / 2f, boardHeight / 2f, color = ArcadeOrange))

        val active = bricks.filter { !it.isDestroyed }
        for (b in active) {
            b.hp -= 10
            b.pulseScale = 1.3f
            if (b.hp <= 0) {
                destroyBrick(b)
            }
        }
    }

    fun getBrickRect(brick: Brick): Rect {
        return getBrickRect(brick.row, brick.col)
    }

    fun getBrickRect(row: Int, col: Int): Rect {
        val padding = 3f
        val left = col * cellWidth + padding
        val top = topMargin + row * cellHeight + padding
        val right = (col + 1) * cellWidth - padding
        val bottom = topMargin + (row + 1) * cellHeight - padding
        return Rect(left, top, right, bottom)
    }
}
