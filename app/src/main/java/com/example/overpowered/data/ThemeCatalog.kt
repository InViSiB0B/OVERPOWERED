package com.example.overpowered.data

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.overpowered.ui.theme.*

data class AppTheme(
    val id: String,
    val name: String,
    val price: Int,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
    val previewColor: Color,
    val description: String = ""
)

object ThemeCatalog {

    // ============= DEFAULT THEMES (FREE) =============

    private val defaultLight = AppTheme(
        id = "theme_light",
        name = "Light",
        price = 0,
        lightColorScheme = LightColors,
        darkColorScheme = LightColors,
        previewColor = OP_Slate_100,
        description = "Classic light theme"
    )

    private val defaultDark = AppTheme(
        id = "theme_dark",
        name = "Dark",
        price = 0,
        lightColorScheme = DarkColors,
        darkColorScheme = DarkColors,
        previewColor = Color(0xFF121824),
        description = "Classic dark theme"
    )

    private val followDevice = AppTheme(
        id = "theme_follow_device",
        name = "Follow Device",
        price = 0,
        lightColorScheme = LightColors,
        darkColorScheme = DarkColors,
        previewColor = OP_SlateBlue,
        description = "Automatically matches your device theme"
    )

    // ============= PURCHASABLE THEMES =============

    // FIRE THEME (Red/Orange)
    private val fireLight = lightColorScheme(
        primary = Color(0xFFFF5722),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFF8A65),
        onPrimaryContainer = Color(0xFF3E2723),

        secondary = Color(0xFFFF9800),
        onSecondary = Color(0xFF3E2723),
        secondaryContainer = Color(0xFFFFB74D),
        onSecondaryContainer = Color(0xFF5D4037),

        tertiary = Color(0xFFBF360C),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFCCBC),
        onTertiaryContainer = Color(0xFF5D4037),

        background = Color(0xFFFFF3E0),
        onBackground = Color(0xFF3E2723),

        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF3E2723),

        surfaceVariant = Color(0xFFFFE0B2),
        onSurfaceVariant = Color(0xFF5D4037),

        outline = Color(0xFFBF360C),
        outlineVariant = Color(0xFFFF8A65),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val fireDark = darkColorScheme(
        primary = Color(0xFFFF7043),
        onPrimary = Color(0xFF3E2723),
        primaryContainer = Color(0xFFBF360C),
        onPrimaryContainer = Color(0xFFFFCCBC),

        secondary = Color(0xFFFFB74D),
        onSecondary = Color(0xFF3E2723),
        secondaryContainer = Color(0xFFE65100),
        onSecondaryContainer = Color(0xFFFFE0B2),

        tertiary = Color(0xFFFF5722),
        onTertiary = Color(0xFFFFCCBC),
        tertiaryContainer = Color(0xFF5D2014),
        onTertiaryContainer = Color(0xFFFFCCBC),

        background = Color(0xFF1A0F0A),
        onBackground = Color(0xFFFFCCBC),

        surface = Color(0xFF3E2723),
        onSurface = Color(0xFFFFCCBC),

        surfaceVariant = Color(0xFF5D4037),
        onSurfaceVariant = Color(0xFFFFE0B2),

        outline = Color(0xFFBF360C),
        outlineVariant = Color(0xFF5D4037),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val fireTheme = AppTheme(
        id = "theme_fire",
        name = "Fire",
        price = 500,
        lightColorScheme = fireLight,
        darkColorScheme = fireDark,
        previewColor = Color(0xFFFF5722),
        description = "Burn bright with fiery red and orange tones"
    )

    // FOREST THEME (Green)
    private val forestLight = lightColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF81C784),
        onPrimaryContainer = Color(0xFF1B5E20),

        secondary = Color(0xFF66BB6A),
        onSecondary = Color(0xFF1B5E20),
        secondaryContainer = Color(0xFFA5D6A7),
        onSecondaryContainer = Color(0xFF2E7D32),

        tertiary = Color(0xFF388E3C),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC8E6C9),
        onTertiaryContainer = Color(0xFF2E7D32),

        background = Color(0xFFF1F8E9),
        onBackground = Color(0xFF1B5E20),

        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1B5E20),

        surfaceVariant = Color(0xFFDCEDC8),
        onSurfaceVariant = Color(0xFF2E7D32),

        outline = Color(0xFF388E3C),
        outlineVariant = Color(0xFF81C784),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val forestDark = darkColorScheme(
        primary = Color(0xFF66BB6A),
        onPrimary = Color(0xFF1B5E20),
        primaryContainer = Color(0xFF2E7D32),
        onPrimaryContainer = Color(0xFFC8E6C9),

        secondary = Color(0xFF81C784),
        onSecondary = Color(0xFF1B5E20),
        secondaryContainer = Color(0xFF1B5E20),
        onSecondaryContainer = Color(0xFFC8E6C9),

        tertiary = Color(0xFF4CAF50),
        onTertiary = Color(0xFFC8E6C9),
        tertiaryContainer = Color(0xFF1B3315),
        onTertiaryContainer = Color(0xFFC8E6C9),

        background = Color(0xFF0D1F0C),
        onBackground = Color(0xFFC8E6C9),

        surface = Color(0xFF1B3315),
        onSurface = Color(0xFFC8E6C9),

        surfaceVariant = Color(0xFF2E4A26),
        onSurfaceVariant = Color(0xFFDCEDC8),

        outline = Color(0xFF388E3C),
        outlineVariant = Color(0xFF2E7D32),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val forestTheme = AppTheme(
        id = "theme_forest",
        name = "Forest",
        price = 500,
        lightColorScheme = forestLight,
        darkColorScheme = forestDark,
        previewColor = Color(0xFF4CAF50),
        description = "Embrace nature with calming green shades"
    )

    // OCEAN THEME (Blue)
    private val oceanLight = lightColorScheme(
        primary = Color(0xFF2196F3),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF64B5F6),
        onPrimaryContainer = Color(0xFF0D47A1),

        secondary = Color(0xFF03A9F4),
        onSecondary = Color(0xFF0D47A1),
        secondaryContainer = Color(0xFF81D4FA),
        onSecondaryContainer = Color(0xFF1565C0),

        tertiary = Color(0xFF1976D2),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBBDEFB),
        onTertiaryContainer = Color(0xFF1565C0),

        background = Color(0xFFE3F2FD),
        onBackground = Color(0xFF0D47A1),

        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0D47A1),

        surfaceVariant = Color(0xFFBBDEFB),
        onSurfaceVariant = Color(0xFF1565C0),

        outline = Color(0xFF1976D2),
        outlineVariant = Color(0xFF64B5F6),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val oceanDark = darkColorScheme(
        primary = Color(0xFF42A5F5),
        onPrimary = Color(0xFF0D47A1),
        primaryContainer = Color(0xFF1565C0),
        onPrimaryContainer = Color(0xFFBBDEFB),

        secondary = Color(0xFF29B6F6),
        onSecondary = Color(0xFF0D47A1),
        secondaryContainer = Color(0xFF01579B),
        onSecondaryContainer = Color(0xFFB3E5FC),

        tertiary = Color(0xFF2196F3),
        onTertiary = Color(0xFFBBDEFB),
        tertiaryContainer = Color(0xFF0A2540),
        onTertiaryContainer = Color(0xFFBBDEFB),

        background = Color(0xFF0A1929),
        onBackground = Color(0xFFBBDEFB),

        surface = Color(0xFF0D2038),
        onSurface = Color(0xFFBBDEFB),

        surfaceVariant = Color(0xFF1A3A52),
        onSurfaceVariant = Color(0xFFB3E5FC),

        outline = Color(0xFF1976D2),
        outlineVariant = Color(0xFF1565C0),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val oceanTheme = AppTheme(
        id = "theme_ocean",
        name = "Ocean",
        price = 500,
        lightColorScheme = oceanLight,
        darkColorScheme = oceanDark,
        previewColor = Color(0xFF2196F3),
        description = "Dive deep into soothing ocean blues"
    )

    // SUNSET THEME (Purple/Pink)
    private val sunsetLight = lightColorScheme(
        primary = Color(0xFF9C27B0),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBA68C8),
        onPrimaryContainer = Color(0xFF4A148C),

        secondary = Color(0xFFEC407A),
        onSecondary = Color(0xFF4A148C),
        secondaryContainer = Color(0xFFF48FB1),
        onSecondaryContainer = Color(0xFF880E4F),

        tertiary = Color(0xFFAB47BC),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE1BEE7),
        onTertiaryContainer = Color(0xFF6A1B9A),

        background = Color(0xFFF3E5F5),
        onBackground = Color(0xFF4A148C),

        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF4A148C),

        surfaceVariant = Color(0xFFE1BEE7),
        onSurfaceVariant = Color(0xFF6A1B9A),

        outline = Color(0xFF9C27B0),
        outlineVariant = Color(0xFFBA68C8),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val sunsetDark = darkColorScheme(
        primary = Color(0xFFBA68C8),
        onPrimary = Color(0xFF4A148C),
        primaryContainer = Color(0xFF6A1B9A),
        onPrimaryContainer = Color(0xFFE1BEE7),

        secondary = Color(0xFFF48FB1),
        onSecondary = Color(0xFF4A148C),
        secondaryContainer = Color(0xFF880E4F),
        onSecondaryContainer = Color(0xFFF8BBD0),

        tertiary = Color(0xFFCE93D8),
        onTertiary = Color(0xFFE1BEE7),
        tertiaryContainer = Color(0xFF38184C),
        onTertiaryContainer = Color(0xFFE1BEE7),

        background = Color(0xFF1A0A1F),
        onBackground = Color(0xFFE1BEE7),

        surface = Color(0xFF2D1B3D),
        onSurface = Color(0xFFE1BEE7),

        surfaceVariant = Color(0xFF4A2E5C),
        onSurfaceVariant = Color(0xFFF3E5F5),

        outline = Color(0xFF9C27B0),
        outlineVariant = Color(0xFF6A1B9A),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val sunsetTheme = AppTheme(
        id = "theme_sunset",
        name = "Sunset",
        price = 600,
        lightColorScheme = sunsetLight,
        darkColorScheme = sunsetDark,
        previewColor = Color(0xFF9C27B0),
        description = "Embrace twilight with purple and pink hues"
    )

    // MIDNIGHT THEME (Deep Blue/Purple)
    private val midnightLight = lightColorScheme(
        primary = Color(0xFF3F51B5),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF7986CB),
        onPrimaryContainer = Color(0xFF1A237E),

        secondary = Color(0xFF5C6BC0),
        onSecondary = Color(0xFF1A237E),
        secondaryContainer = Color(0xFF9FA8DA),
        onSecondaryContainer = Color(0xFF283593),

        tertiary = Color(0xFF536DFE),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFC5CAE9),
        onTertiaryContainer = Color(0xFF283593),

        background = Color(0xFFE8EAF6),
        onBackground = Color(0xFF1A237E),

        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A237E),

        surfaceVariant = Color(0xFFC5CAE9),
        onSurfaceVariant = Color(0xFF283593),

        outline = Color(0xFF3F51B5),
        outlineVariant = Color(0xFF7986CB),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val midnightDark = darkColorScheme(
        primary = Color(0xFF5C6BC0),
        onPrimary = Color(0xFF1A237E),
        primaryContainer = Color(0xFF283593),
        onPrimaryContainer = Color(0xFFC5CAE9),

        secondary = Color(0xFF7986CB),
        onSecondary = Color(0xFF1A237E),
        secondaryContainer = Color(0xFF1A237E),
        onSecondaryContainer = Color(0xFFE8EAF6),

        tertiary = Color(0xFF536DFE),
        onTertiary = Color(0xFFC5CAE9),
        tertiaryContainer = Color(0xFF0D1333),
        onTertiaryContainer = Color(0xFFC5CAE9),

        background = Color(0xFF0A0E27),
        onBackground = Color(0xFFC5CAE9),

        surface = Color(0xFF151937),
        onSurface = Color(0xFFC5CAE9),

        surfaceVariant = Color(0xFF283250),
        onSurfaceVariant = Color(0xFFE8EAF6),

        outline = Color(0xFF3F51B5),
        outlineVariant = Color(0xFF283593),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val midnightTheme = AppTheme(
        id = "theme_midnight",
        name = "Midnight",
        price = 600,
        lightColorScheme = midnightLight,
        darkColorScheme = midnightDark,
        previewColor = Color(0xFF3F51B5),
        description = "Embrace the night with deep indigo tones"
    )

    // SAKURA THEME (Pink/Cherry Blossom)
    private val sakuraLight = lightColorScheme(
        primary = Color(0xFFE91E63),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF06292),
        onPrimaryContainer = Color(0xFF880E4F),

        secondary = Color(0xFFF8BBD0),
        onSecondary = Color(0xFF880E4F),
        secondaryContainer = Color(0xFFFCE4EC),
        onSecondaryContainer = Color(0xFFC2185B),

        tertiary = Color(0xFFEC407A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF8BBD0),
        onTertiaryContainer = Color(0xFFC2185B),

        background = Color(0xFFFCE4EC),
        onBackground = Color(0xFF880E4F),

        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF880E4F),

        surfaceVariant = Color(0xFFF8BBD0),
        onSurfaceVariant = Color(0xFFC2185B),

        outline = Color(0xFFE91E63),
        outlineVariant = Color(0xFFF06292),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val sakuraDark = darkColorScheme(
        primary = Color(0xFFF48FB1),
        onPrimary = Color(0xFF880E4F),
        primaryContainer = Color(0xFFC2185B),
        onPrimaryContainer = Color(0xFFFCE4EC),

        secondary = Color(0xFFF8BBD0),
        onSecondary = Color(0xFF880E4F),
        secondaryContainer = Color(0xFF880E4F),
        onSecondaryContainer = Color(0xFFFCE4EC),

        tertiary = Color(0xFFEC407A),
        onTertiary = Color(0xFFF8BBD0),
        tertiaryContainer = Color(0xFF3D0A1F),
        onTertiaryContainer = Color(0xFFF8BBD0),

        background = Color(0xFF1F0A15),
        onBackground = Color(0xFFF8BBD0),

        surface = Color(0xFF331525),
        onSurface = Color(0xFFF8BBD0),

        surfaceVariant = Color(0xFF4D2638),
        onSurfaceVariant = Color(0xFFFCE4EC),

        outline = Color(0xFFE91E63),
        outlineVariant = Color(0xFFC2185B),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val sakuraTheme = AppTheme(
        id = "theme_sakura",
        name = "Sakura",
        price = 700,
        lightColorScheme = sakuraLight,
        darkColorScheme = sakuraDark,
        previewColor = Color(0xFFE91E63),
        description = "Blossom with delicate cherry pink tones"
    )

    // GOLD THEME (Gold/Amber)
    private val goldLight = lightColorScheme(
        primary = Color(0xFFFFB300),
        onPrimary = Color(0xFF3E2723),
        primaryContainer = Color(0xFFFFCA28),
        onPrimaryContainer = Color(0xFF3E2723),

        secondary = Color(0xFFFFD54F),
        onSecondary = Color(0xFF3E2723),
        secondaryContainer = Color(0xFFFFE082),
        onSecondaryContainer = Color(0xFF5D4037),

        tertiary = Color(0xFFFFC107),
        onTertiary = Color(0xFF3E2723),
        tertiaryContainer = Color(0xFFFFECB3),
        onTertiaryContainer = Color(0xFF5D4037),

        background = Color(0xFFFFF8E1),
        onBackground = Color(0xFF3E2723),

        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF3E2723),

        surfaceVariant = Color(0xFFFFECB3),
        onSurfaceVariant = Color(0xFF5D4037),

        outline = Color(0xFFFFB300),
        outlineVariant = Color(0xFFFFCA28),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val goldDark = darkColorScheme(
        primary = Color(0xFFFFCA28),
        onPrimary = Color(0xFF3E2723),
        primaryContainer = Color(0xFFF57F17),
        onPrimaryContainer = Color(0xFFFFECB3),

        secondary = Color(0xFFFFD54F),
        onSecondary = Color(0xFF3E2723),
        secondaryContainer = Color(0xFFF57F17),
        onSecondaryContainer = Color(0xFFFFF8E1),

        tertiary = Color(0xFFFFB300),
        onTertiary = Color(0xFFFFECB3),
        tertiaryContainer = Color(0xFF3D2A0A),
        onTertiaryContainer = Color(0xFFFFECB3),

        background = Color(0xFF1F1A0A),
        onBackground = Color(0xFFFFECB3),

        surface = Color(0xFF332B15),
        onSurface = Color(0xFFFFECB3),

        surfaceVariant = Color(0xFF4D4126),
        onSurfaceVariant = Color(0xFFFFF8E1),

        outline = Color(0xFFFFB300),
        outlineVariant = Color(0xFFF57F17),

        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF)
    )

    private val goldTheme = AppTheme(
        id = "theme_gold",
        name = "Gold",
        price = 800,
        lightColorScheme = goldLight,
        darkColorScheme = goldDark,
        previewColor = Color(0xFFFFB300),
        description = "Shine bright with luxurious golden tones"
    )

    // ============= CATALOG FUNCTIONS =============

    fun getAllThemes(): List<AppTheme> = listOf(
        defaultLight,
        defaultDark,
        followDevice,
        fireTheme,
        forestTheme,
        oceanTheme,
        sunsetTheme,
        midnightTheme,
        sakuraTheme,
        goldTheme
    )

    fun getDefaultThemes(): List<AppTheme> = listOf(
        defaultLight,
        defaultDark,
        followDevice
    )

    fun getPurchasableThemes(): List<AppTheme> = getAllThemes().filter { it.price > 0 }

    fun getThemeById(id: String?): AppTheme? =
        if (id == null) null else getAllThemes().find { it.id == id }

    fun getDefaultTheme(): AppTheme = followDevice
}
