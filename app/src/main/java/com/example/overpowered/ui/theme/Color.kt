package com.example.overpowered.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// ===== Brand Core =====
val OP_Teal_500 = Color(0xFF5EABC2) // primary intent
val OP_Teal_900 = Color(0xFF3D6C7E) // primary intent
val OP_Teal_100 = Color(0xFFEFF0FE)
val OP_Slate_900  = Color(0xFF0F172A)
val OP_Slate_800  = Color(0xFF1F2A36) // your DeepNavy-ish
val OP_Slate_700  = Color(0xFF334155)
val OP_Slate_600  = Color(0xFF475569)
val OP_Slate_300  = Color(0xFFCBD5E1)
val OP_Slate_100  = Color(0xFFF1F5F9)
val OP_White      = Color(0xFFFFFFFF)

// ===== Accents / Feedback =====
val OP_Green_500  = Color(0xFF22C55E) // success
val OP_Amber_500  = Color(0xFFF59E0B) // warning
val OP_Red_500    = Color(0xFFEF4444) // error main
val OP_Red_100    = Color(0xFFFFE5E5)

val OP_Coral      = Color(0xFFFFA48A) // your accent
val OP_BabyBlue   = Color(0xFFB9D3EE) // your soft supporting blue
val OP_SlateBlue  = Color(0xFF5F6F8E) // your muted slate

// ===== Legacy aliases (kept for compatibility) =====
val DeepNavy   = OP_Slate_800
val SlateBlue  = OP_SlateBlue
val BabyBlue   = OP_BabyBlue
val Coral      = OP_Coral
val LightGray  = Color(0xFFDDDDDD)
val PureWhite  = OP_White
val OffWhite   = OP_Slate_100
val DarkSurface= Color(0xFF253140)
val OnDark     = OP_BabyBlue

// ================== LIGHT SCHEME ==================
val LightColors = lightColorScheme(
    primary            = OP_Teal_500,
    onPrimary          = OP_White,
    primaryContainer   = OP_Teal_500,
    onPrimaryContainer = OP_Slate_900,


    secondary          = OP_Coral,
    onSecondary        = OP_Slate_900,
    secondaryContainer = OP_Coral,
    onSecondaryContainer = OP_Slate_700,

    tertiary           = OP_SlateBlue,
    onTertiary         = OP_White,
    tertiaryContainer  = OP_Slate_100,
    onTertiaryContainer = OP_Slate_700,

    background         = OP_Slate_100,   // very light, calm
    onBackground       = OP_Slate_900,

    surface            = OP_White,
    onSurface          = OP_Slate_900,

    surfaceVariant     = Color(0xFFE6EAF2),
    onSurfaceVariant   = OP_Slate_700,

    outline            = Color(0xFFB8C2D3),
    outlineVariant     = Color(0xFFD9E0EC),

    error              = OP_Red_500,
    onError            = OP_White
)

// ================== DARK SCHEME ==================
val DarkColors = darkColorScheme(
    primary            = OP_SlateBlue, // softer pop on dark
    onPrimary          = OP_Slate_800,
    primaryContainer   = OP_Teal_900,
    onPrimaryContainer = OP_Teal_100,

    secondary          = OP_Coral,
    onSecondary        = OP_Slate_900,
    secondaryContainer = OP_Slate_700,
    onSecondaryContainer = OP_Coral,

    tertiary           = OP_SlateBlue,
    onTertiary         = OnDark,
    tertiaryContainer  = Color(0xFF2A3647),
    onTertiaryContainer = OnDark,

    background         = OP_Slate_600,
    onBackground       = OP_Slate_300,

    surface            = Color(0xFF121824),
    onSurface          = OnDark,

    surfaceVariant     = Color(0xFF28344A),
    onSurfaceVariant   = Color(0xFFC9D6EA),

    outline            = Color(0xFF5E708F),
    outlineVariant     = Color(0xFF3D4A62),

    error              = OP_Red_500,
    onError            = OP_White
)

// ================== SEMANTIC SUPPORT ==================
object OPFeedback {
    val Success = OP_Green_500
    val Warning = OP_Amber_500
    val Error   = OP_Red_500
}

object OPSurfaces {
    // Subtle tinted layers for cards/sections
    val Elev0 = { c: androidx.compose.material3.ColorScheme -> c.surface }
    val Elev1 = { c: androidx.compose.material3.ColorScheme -> c.surface.copy(alpha = 0.92f) }
    val Elev2 = { c: androidx.compose.material3.ColorScheme -> c.surface.copy(alpha = 0.86f) }
}

object OPChips {
    val PositiveBg = Color(0x1A) /* placeholder to avoid 0x1A parsing */.let { OP_Green_500.copy(alpha = 0.12f) }
    val WarningBg  = OP_Amber_500.copy(alpha = 0.12f)
    val InfoBg     = OP_Teal_500.copy(alpha = 0.10f)
}
