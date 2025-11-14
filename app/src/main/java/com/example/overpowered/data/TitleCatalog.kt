package com.example.overpowered.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

data class Title(
    val id: String,
    val name: String,
    val price: Int,
    val color: Color = Color.White,
    val fontWeight: FontWeight = FontWeight.Bold,
    val glowColor: Color? = null, // Optional glow effect
    val isGradient: Boolean = false,
    val gradientColors: List<Color>? = null
)

object TitleCatalog {
    fun getAllTitles(): List<Title> = listOf(
        // Basic Titles
        Title(
            id = "title_1",
            name = "Overpowered",
            price = 100,
            color = Color(0xFFFFD700), // Gold
            fontWeight = FontWeight.ExtraBold,
            glowColor = Color(0xFFFFD700).copy(alpha = 0.3f)
        ),

        Title(
            id = "title_2",
            name = "Legendary",
            price = 250,
            color = Color(0xFFFF6B35), // Orange/Red
            fontWeight = FontWeight.ExtraBold,
            glowColor = Color(0xFFFF6B35).copy(alpha = 0.3f)
        ),

        Title(
            id = "title_3",
            name = "Epic",
            price = 150,
            color = Color(0xFF9B59B6), // Purple
            fontWeight = FontWeight.Bold
        ),

        Title(
            id = "title_4",
            name = "Master",
            price = 200,
            color = Color(0xFF3498DB), // Blue
            fontWeight = FontWeight.ExtraBold
        ),

        Title(
            id = "title_5",
            name = "Champion",
            price = 300,
            color = Color(0xFFE74C3C), // Red
            fontWeight = FontWeight.ExtraBold,
            glowColor = Color(0xFFE74C3C).copy(alpha = 0.3f)
        ),

        Title(
            id = "title_6",
            name = "Elite",
            price = 175,
            color = Color(0xFF1ABC9C), // Teal
            fontWeight = FontWeight.Bold
        ),

        Title(
            id = "title_7",
            name = "Supreme",
            price = 350,
            color = Color(0xFFFDD835), // Bright Yellow
            fontWeight = FontWeight.ExtraBold,
            glowColor = Color(0xFFFDD835).copy(alpha = 0.4f)
        ),

        Title(
            id = "title_8",
            name = "Divine",
            price = 500,
            color = Color(0xFFFFFFFF), // White with gradient
            fontWeight = FontWeight.ExtraBold,
            isGradient = true,
            gradientColors = listOf(
                Color(0xFFFFD700),
                Color(0xFFFFFFFF),
                Color(0xFFFFD700)
            ),
            glowColor = Color(0xFFFFD700).copy(alpha = 0.5f)
        ),

        // Premium Titles
        Title(
            id = "title_9",
            name = "Unstoppable",
            price = 400,
            color = Color(0xFFFF1744), // Deep Red
            fontWeight = FontWeight.ExtraBold,
            glowColor = Color(0xFFFF1744).copy(alpha = 0.4f)
        ),

        Title(
            id = "title_10",
            name = "Immortal",
            price = 600,
            color = Color(0xFF00E5FF), // Cyan
            fontWeight = FontWeight.ExtraBold,
            isGradient = true,
            gradientColors = listOf(
                Color(0xFF00E5FF),
                Color(0xFFFFFFFF),
                Color(0xFF00E5FF)
            ),
            glowColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
        ),

        Title(
            id = "title_11",
            name = "Mythic",
            price = 450,
            color = Color(0xFFAB47BC), // Deep Purple
            fontWeight = FontWeight.ExtraBold,
            glowColor = Color(0xFFAB47BC).copy(alpha = 0.4f)
        ),

        Title(
            id = "title_12",
            name = "Celestial",
            price = 550,
            color = Color(0xFFB388FF), // Light Purple
            fontWeight = FontWeight.ExtraBold,
            isGradient = true,
            gradientColors = listOf(
                Color(0xFFB388FF),
                Color(0xFFFFFFFF),
                Color(0xFFB388FF)
            ),
            glowColor = Color(0xFFB388FF).copy(alpha = 0.5f)
        )
    )

    fun getTitleById(id: String): Title? {
        return getAllTitles().find { it.id == id }
    }
}