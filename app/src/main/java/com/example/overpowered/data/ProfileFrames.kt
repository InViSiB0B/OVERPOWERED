package com.example.overpowered.data

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class ProfileFrame(
    val id: String,
    val name: String,
    val price: Int,
    val borderBrush: Brush,
    val borderWidth: Float = 4f,
    val glowColor: Color? = null,
    val pattern: FramePattern = FramePattern.SOLID
)

enum class FramePattern {
    SOLID,
    GRADIENT,
    DOUBLE,
    GLOW,
    ANIMATED_SHINE,
    DASHED,
    RAINBOW
}

object FrameCatalog {
    fun getAllFrames(): List<ProfileFrame> = listOf(
        // Basic frames
        ProfileFrame(
            id = "frame_autumn",
            name = "Autumn",
            price = 100,
            borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF6B35),
                    Color(0xFFF7931E),
                    Color(0xFFFDC830)
                )
            ),
            borderWidth = 4f,
            pattern = FramePattern.GRADIENT
        ),

        ProfileFrame(
            id = "frame_ocean",
            name = "Ocean",
            price = 100,
            borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0575E6),
                    Color(0xFF021B79)
                )
            ),
            borderWidth = 4f,
            pattern = FramePattern.GRADIENT
        ),

        ProfileFrame(
            id = "frame_forest",
            name = "Forest",
            price = 100,
            borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF134E5E),
                    Color(0xFF71B280)
                )
            ),
            borderWidth = 4f,
            pattern = FramePattern.GRADIENT
        ),

        // Premium frames
        ProfileFrame(
            id = "frame_gold",
            name = "Gold",
            price = 500,
            borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFA500),
                    Color(0xFFFFD700)
                )
            ),
            borderWidth = 5f,
            glowColor = Color(0xFFFFD700),
            pattern = FramePattern.GLOW
        ),

        ProfileFrame(
            id = "frame_rainbow",
            name = "Rainbow",
            price = 1000,
            borderBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFF0000),
                    Color(0xFFFF7F00),
                    Color(0xFFFFFF00),
                    Color(0xFF00FF00),
                    Color(0xFF0000FF),
                    Color(0xFF4B0082),
                    Color(0xFF9400D3),
                    Color(0xFFFF0000)
                )
            ),
            borderWidth = 5f,
            pattern = FramePattern.RAINBOW
        ),

        ProfileFrame(
            id = "frame_fire",
            name = "Fire",
            price = 750,
            borderBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF4500),
                    Color(0xFFFF6347),
                    Color(0xFFFF8C00)
                )
            ),
            borderWidth = 5f,
            glowColor = Color(0xFFFF4500),
            pattern = FramePattern.GLOW
        ),

        ProfileFrame(
            id = "frame_ice",
            name = "Ice",
            price = 750,
            borderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4FC3F7),
                    Color(0xFF29B6F6),
                    Color(0xFF03A9F4)
                )
            ),
            borderWidth = 5f,
            glowColor = Color(0xFF4FC3F7),
            pattern = FramePattern.GLOW
        ),

        ProfileFrame(
            id = "frame_legendary",
            name = "Legendary",
            price = 2000,
            borderBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFF6B6B),
                    Color(0xFFFFD700)
                )
            ),
            borderWidth = 6f,
            glowColor = Color(0xFFFFD700),
            pattern = FramePattern.ANIMATED_SHINE
        )
    )

    fun getFrameById(id: String): ProfileFrame? {
        return getAllFrames().find { it.id == id }
    }
}