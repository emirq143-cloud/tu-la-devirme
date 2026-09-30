package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class BrickType {
    NORMAL,
    ADD_BALL,
    BOMB,
    HORIZ_LASER,
    VERT_LASER,
    CROSS_LASER,
    COIN,
    SPLIT_BALL,
    AMULET,   // Nazar Bloğu (🧿) - Destroys 3 random bricks
    VORTEX,   // Kozmik Girdap (🌀) - Curves ball trajectories
    JACKPOT   // Altın Kasası (💰) - Multi-hit coin jackpot
}

enum class GameMode {
    STAGE,
    ENDLESS
}

enum class TurnState {
    AIMING,
    SHOOTING,
    RECALLING,
    ROUND_RESOLVING,
    GAME_OVER,
    VICTORY,
    PAUSED
}

data class Brick(
    val id: String,
    val row: Int,
    val col: Int,
    var hp: Int,
    val maxHp: Int,
    val type: BrickType,
    var isDestroyed: Boolean = false,
    var pulseScale: Float = 1.0f,
    var hitFlashAlpha: Float = 0f
) {
    fun getBaseColor(): Color {
        return when (type) {
            BrickType.ADD_BALL -> ArcadeGreen
            BrickType.BOMB -> DangerRed
            BrickType.HORIZ_LASER -> ArcadeSecondary
            BrickType.VERT_LASER -> ArcadePrimary
            BrickType.CROSS_LASER -> ArcadePurple
            BrickType.COIN -> ArcadeTertiary
            BrickType.SPLIT_BALL -> Color(0xFF00E5FF)
            BrickType.AMULET -> Color(0xFF1E88E5) // Mavi Nazar Boncuğu rengi
            BrickType.VORTEX -> Color(0xFF7C4DFF) // Mor Kozmik Girdap
            BrickType.JACKPOT -> Color(0xFFFFD600) // Altın Kasası
            BrickType.NORMAL -> {
                when {
                    hp <= 3 -> Color(0xFF4CAF50) // Green
                    hp <= 8 -> Color(0xFF00BCD4) // Cyan
                    hp <= 15 -> Color(0xFF2196F3) // Blue
                    hp <= 25 -> Color(0xFF9C27B0) // Purple
                    hp <= 40 -> Color(0xFFFF9800) // Orange
                    hp <= 70 -> Color(0xFFFF5722) // Deep Orange
                    else -> Color(0xFFF44336) // Red
                }
            }
        }
    }
}

data class Ball(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float = 12f,
    var isLaunched: Boolean = false,
    var isReturned: Boolean = false,
    var trail: MutableList<Offset> = mutableListOf()
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val initialRadius: Float,
    var radius: Float,
    var alpha: Float = 1.0f,
    var life: Float = 1.0f,
    val decay: Float = 0.035f
)

data class FloatingText(
    val text: String,
    var x: Float,
    var y: Float,
    var vy: Float = -2.5f,
    var alpha: Float = 1.0f,
    val color: Color = Color.White
)

data class BallSkin(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val primaryColor: Color,
    val glowColor: Color,
    val iconSymbol: String
)

object BallSkins {
    val ALL = listOf(
        BallSkin("classic", "Klasik Küre", "Pürüzsüz parlayan beyaz top", 0, Color.White, Color(0xFFB0E0E6), "⚪"),
        BallSkin("nazar_boncugu", "Nazar Boncuğu", "Nazar değmesin! Koruyucu mavi göz enerjisi", 200, Color(0xFF1E88E5), Color(0xFF80D8FF), "🧿"),
        BallSkin("neon_cyan", "Neon Kıvılcım", "Parlak fütüristik siber top", 150, Color(0xFF00E5FF), Color(0xFF80F3FF), "💠"),
        BallSkin("fire_ember", "Ateş Topu", "Kızgın akkor alev saçan küre", 300, Color(0xFFFF5722), Color(0xFFFFAB91), "🔥"),
        BallSkin("meteor_core", "Ateş Meteoru", "Kavurucu plazma çekirdekli meteor", 400, Color(0xFFFF3D00), Color(0xFFFFD600), "☄️"),
        BallSkin("plasma_pink", "Plazma Pembe", "Yüksek voltajlı neon pembe enerji", 450, Color(0xFFFF2A85), Color(0xFFFF80AB), "⚡"),
        BallSkin("golden_star", "Altın Yıldız", "Saf altın parıltılı şampiyon topu", 600, Color(0xFFFFD600), Color(0xFFFFF9C4), "⭐"),
        BallSkin("emerald_core", "Zümrüt Kristal", "Gizemli parlayan yeşil cevher", 800, Color(0xFF00E676), Color(0xFFB9F6CA), "💎"),
        BallSkin("cosmic_void", "Kozmik Boşluk", "Mor kuantum çekirdeği", 1000, Color(0xFF9C27B0), Color(0xFFE1BEE7), "🔮")
    )

    fun getById(id: String): BallSkin {
        return ALL.find { it.id == id } ?: ALL.first()
    }
}
