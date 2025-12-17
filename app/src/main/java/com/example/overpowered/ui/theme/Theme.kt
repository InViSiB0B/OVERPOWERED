package com.example.overpowered.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography as M3Typography
import androidx.compose.ui.unit.sp
import com.example.overpowered.data.ThemeCatalog

// Shapes: calm, consistent radii
val OPShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Typography: readable and low-stimulus (two weights)
val OPTypography: Typography = M3Typography().run {
    copy(
        displaySmall = displaySmall.copy(letterSpacing = 0.sp),
        headlineSmall = headlineSmall.copy(letterSpacing = 0.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
        bodyLarge = bodyLarge.copy(lineHeight = 22.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 20.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium)
    )
}

// Spacing scale (predictable rhythm)
object OPSpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun OVERPOWEREDTheme(
    selectedThemeId: String? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Get the selected theme, defaulting to "Follow Device" if none selected
    val themeId = selectedThemeId ?: "theme_follow_device"
    val theme = ThemeCatalog.getThemeById(themeId) ?: ThemeCatalog.getDefaultTheme()

    // Determine which color scheme to use based on theme ID and system settings
    val scheme = when (themeId) {
        "theme_light" -> theme.lightColorScheme
        "theme_dark" -> theme.darkColorScheme
        "theme_follow_device" -> if (darkTheme) theme.darkColorScheme else theme.lightColorScheme
        else -> if (darkTheme) theme.darkColorScheme else theme.lightColorScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = OPTypography,
        shapes = OPShapes,
        content = content
    )
}
