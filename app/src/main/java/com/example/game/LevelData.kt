package com.example.game

data class LevelLayout(
    val levelNumber: Int,
    val name: String,
    val initialBallCount: Int,
    val parTurns: Int, // turns to get 3 stars
    val bricks: List<LevelBrickTemplate>
)

data class LevelBrickTemplate(
    val row: Int,
    val col: Int,
    val hp: Int,
    val type: BrickType
)

object LevelData {
    const val GRID_COLS = 7
    const val GRID_ROWS = 10

    fun getLevel(levelNumber: Int): LevelLayout {
        val bricks = mutableListOf<LevelBrickTemplate>()
        val ballCount = 10 + (levelNumber * 2)
        val parTurns = 8 + (levelNumber / 2)

        when (levelNumber) {
            1 -> {
                // Easy warm-up: 2 rows of soft bricks, +1 balls, and custom Nazar Boncuğu!
                for (c in 0 until GRID_COLS) {
                    bricks.add(LevelBrickTemplate(2, c, 3, BrickType.NORMAL))
                }
                bricks.add(LevelBrickTemplate(3, 1, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(3, 3, 5, BrickType.BOMB))
                bricks.add(LevelBrickTemplate(3, 5, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(4, 2, 2, BrickType.COIN))
                bricks.add(LevelBrickTemplate(4, 3, 1, BrickType.AMULET)) // 🧿 Nazar Bloğu!
                bricks.add(LevelBrickTemplate(4, 4, 2, BrickType.COIN))
            }
            2 -> {
                // Diamond pattern with laser in center and Jackpot block
                bricks.add(LevelBrickTemplate(1, 3, 6, BrickType.HORIZ_LASER))
                bricks.add(LevelBrickTemplate(2, 2, 5, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(2, 4, 5, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(3, 1, 6, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(3, 3, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(3, 5, 6, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(4, 2, 5, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(4, 4, 5, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(5, 3, 8, BrickType.VERT_LASER))
                bricks.add(LevelBrickTemplate(2, 3, 1, BrickType.COIN))
                bricks.add(LevelBrickTemplate(3, 0, 3, BrickType.JACKPOT)) // 💰 Altın Kasası!
                bricks.add(LevelBrickTemplate(4, 3, 1, BrickType.COIN))
            }
            3 -> {
                // Smiley Face with Cosmic Vortex
                // Eyes
                bricks.add(LevelBrickTemplate(2, 2, 8, BrickType.BOMB))
                bricks.add(LevelBrickTemplate(2, 4, 8, BrickType.BOMB))
                // Nose with Vortex gravity!
                bricks.add(LevelBrickTemplate(3, 3, 6, BrickType.VORTEX)) // 🌀 Kozmik Girdap!
                // Smile
                bricks.add(LevelBrickTemplate(5, 1, 10, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(6, 2, 10, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(6, 3, 12, BrickType.HORIZ_LASER))
                bricks.add(LevelBrickTemplate(6, 4, 10, BrickType.NORMAL))
                bricks.add(LevelBrickTemplate(5, 5, 10, BrickType.NORMAL))
                // Extras
                bricks.add(LevelBrickTemplate(4, 0, 1, BrickType.COIN))
                bricks.add(LevelBrickTemplate(4, 3, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(4, 6, 1, BrickType.COIN))
            }
            4 -> {
                // Heart shape
                val heartCoords = listOf(
                    Pair(1, 1), Pair(1, 2), Pair(1, 4), Pair(1, 5),
                    Pair(2, 0), Pair(2, 3), Pair(2, 6),
                    Pair(3, 0), Pair(3, 6),
                    Pair(4, 1), Pair(4, 5),
                    Pair(5, 2), Pair(5, 4),
                    Pair(6, 3)
                )
                heartCoords.forEach { (r, c) ->
                    bricks.add(LevelBrickTemplate(r, c, 12, BrickType.NORMAL))
                }
                bricks.add(LevelBrickTemplate(2, 3, 1, BrickType.CROSS_LASER))
                bricks.add(LevelBrickTemplate(3, 2, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(3, 4, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(4, 3, 1, BrickType.BOMB))
            }
            5 -> {
                // Pyramid Fortress
                for (row in 1..5) {
                    val startCol = 3 - (row - 1)
                    val endCol = 3 + (row - 1)
                    for (col in startCol..endCol) {
                        if (row == 1) {
                            bricks.add(LevelBrickTemplate(row, col, 20, BrickType.BOMB))
                        } else if (col == startCol || col == endCol) {
                            bricks.add(LevelBrickTemplate(row, col, 14, BrickType.NORMAL))
                        } else if (row == 3 && col == 3) {
                            bricks.add(LevelBrickTemplate(row, col, 1, BrickType.SPLIT_BALL))
                        } else if (row == 4 && col == 3) {
                            bricks.add(LevelBrickTemplate(row, col, 1, BrickType.ADD_BALL))
                        } else {
                            bricks.add(LevelBrickTemplate(row, col, 10, BrickType.NORMAL))
                        }
                    }
                }
            }
            6 -> {
                // Laser Grid Crossfire
                for (r in 1..5) {
                    for (c in 0 until GRID_COLS) {
                        if ((r + c) % 3 == 0) {
                            bricks.add(LevelBrickTemplate(r, c, 15, if (r % 2 == 0) BrickType.HORIZ_LASER else BrickType.VERT_LASER))
                        } else if ((r + c) % 4 == 0) {
                            bricks.add(LevelBrickTemplate(r, c, 1, BrickType.ADD_BALL))
                        } else {
                            bricks.add(LevelBrickTemplate(r, c, 18, BrickType.NORMAL))
                        }
                    }
                }
            }
            7 -> {
                // Castle Battlements
                // Tops
                listOf(0, 2, 4, 6).forEach { c ->
                    bricks.add(LevelBrickTemplate(1, c, 15, BrickType.NORMAL))
                }
                // Wall
                for (c in 0 until GRID_COLS) {
                    bricks.add(LevelBrickTemplate(2, c, 20, BrickType.NORMAL))
                    bricks.add(LevelBrickTemplate(3, c, 20, BrickType.NORMAL))
                }
                bricks.add(LevelBrickTemplate(4, 1, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(4, 3, 1, BrickType.SPLIT_BALL))
                bricks.add(LevelBrickTemplate(4, 5, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(5, 3, 1, BrickType.BOMB))
            }
            8 -> {
                // Space Invader Alien
                val alien = listOf(
                    Pair(1, 2), Pair(1, 4),
                    Pair(2, 3),
                    Pair(3, 1), Pair(3, 2), Pair(3, 3), Pair(3, 4), Pair(3, 5),
                    Pair(4, 0), Pair(4, 1), Pair(4, 3), Pair(4, 5), Pair(4, 6),
                    Pair(5, 0), Pair(5, 6),
                    Pair(6, 1), Pair(6, 5)
                )
                alien.forEach { (r, c) ->
                    val type = when {
                        r == 3 && c == 3 -> BrickType.CROSS_LASER
                        r == 2 && c == 3 -> BrickType.BOMB
                        else -> BrickType.NORMAL
                    }
                    bricks.add(LevelBrickTemplate(r, c, 22, type))
                }
                bricks.add(LevelBrickTemplate(2, 2, 1, BrickType.ADD_BALL))
                bricks.add(LevelBrickTemplate(2, 4, 1, BrickType.ADD_BALL))
            }
            else -> {
                // Procedural high-energy challenge for levels 9 to 20
                val baseHp = 15 + (levelNumber * 4)
                for (r in 1..6) {
                    for (c in 0 until GRID_COLS) {
                        val rand = (r * 11 + c * 7 + levelNumber * 13) % 100
                        when {
                            rand < 45 -> {
                                val hp = baseHp + (r * 3) - (c % 3)
                                bricks.add(LevelBrickTemplate(r, c, hp, BrickType.NORMAL))
                            }
                            rand < 55 -> {
                                bricks.add(LevelBrickTemplate(r, c, 1, BrickType.ADD_BALL))
                            }
                            rand < 65 -> {
                                bricks.add(LevelBrickTemplate(r, c, baseHp / 2, BrickType.BOMB))
                            }
                            rand < 80 -> {
                                val type = if (rand % 2 == 0) BrickType.HORIZ_LASER else BrickType.VERT_LASER
                                bricks.add(LevelBrickTemplate(r, c, baseHp / 2, type))
                            }
                            rand < 85 -> {
                                bricks.add(LevelBrickTemplate(r, c, 1, BrickType.SPLIT_BALL))
                            }
                            rand < 90 -> {
                                bricks.add(LevelBrickTemplate(r, c, 1, BrickType.COIN))
                            }
                            rand < 94 -> {
                                bricks.add(LevelBrickTemplate(r, c, 1, BrickType.AMULET))
                            }
                            rand < 97 -> {
                                bricks.add(LevelBrickTemplate(r, c, baseHp / 2, BrickType.VORTEX))
                            }
                            else -> {
                                bricks.add(LevelBrickTemplate(r, c, 3, BrickType.JACKPOT))
                            }
                        }
                    }
                }
            }
        }

        val levelNames = listOf(
            "Başlangıç", "Elmas Geçidi", "Gülen Yüz", "Kalp Çarpıntısı", "Piramit Kalesi",
            "Lazer Ağı", "Kale Surları", "Uzay İstilacısı", "Girdap Sarmalı", "Mega Bomba",
            "Kristal Labirent", "Ateş Çemberi", "Yıldız Yağmuru", "Gökkuşağı Kalkanı", "Zümrüt Tapınak",
            "Titan Kuleleri", "Sonsuzluk Döngüsü", "Gölge Kalesi", "Volkan Krateri", "Büyük Şampiyon"
        )
        val name = levelNames.getOrElse(levelNumber - 1) { "Bölüm $levelNumber" }

        return LevelLayout(
            levelNumber = levelNumber,
            name = name,
            initialBallCount = ballCount,
            parTurns = parTurns,
            bricks = bricks
        )
    }
}
