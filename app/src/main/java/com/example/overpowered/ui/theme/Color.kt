package com.example.overpowered.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// ============== Colors ===================
val DeepNavy   = Color(0xFF1F2A36) // very dark blue-gray
val SlateBlue  = Color(0xFF5F6F8E) // muted slate
val BabyBlue   = Color(0xFFB9D3EE) // soft pastel blue
val Coral      = Color(0xFFFFA48A) // coral accent

// =========== Neutrals used for backgrounds/surfaces in light/dark ============
val LightGray  = Color(0xFFDDDDDD) // neutral surface
val PureWhite  = Color(0xFFFFFFFF)
val OffWhite   = Color(0xFFFAFAFA)
val DarkSurface= Color(0xFF253140) // lighter than DeepNavy for surfaces
val OnDark     = Color(0xFFE9EEF6) // readable text on dark

// =========== Light Scheme ===================================================
val LightColors = lightColorScheme(
    primary         = SlateBlue,     // brand blue for buttons/links
    onPrimary       = PureWhite,

    secondary       = Coral,         // accent/reward color
    onSecondary     = Color(0xFF2B2B2B),

    tertiary        = BabyBlue,      // supporting blue (chips, outlines)
    onTertiary      = Color(0xFF1E2A36),

    background      = OffWhite,
    onBackground    = Color(0xFF1F2A36),

    surface         = PureWhite,
    onSurface       = Color(0xFF1F2A36),

    surfaceVariant  = LightGray,
    onSurfaceVariant= Color(0xFF3A4656),

    outline         = Color(0xFF8A99B3),
    outlineVariant  = Color(0xFFC8D2E3),

    error           = Color(0xFFB3261E),
    onError         = PureWhite
)

// =========== Dark Scheme =================================================
val DarkColors = darkColorScheme(
    primary         = BabyBlue,      // lighter blue reads well on dark
    onPrimary       = Color(0xFF0E1721),

    secondary       = Coral,         // pop for rewards on dark
    onSecondary     = Color(0xFF25120E),

    tertiary        = SlateBlue,
    onTertiary      = OnDark,

    background      = DeepNavy,
    onBackground    = OnDark,

    surface         = DarkSurface,
    onSurface       = OnDark,

    surfaceVariant  = Color(0xFF334056),
    onSurfaceVariant= Color(0xFFC9D6EA),

    outline         = Color(0xFF6F819F),
    outlineVariant  = Color(0xFF40516C),

    error           = Color(0xFFFFB4AB),
    onError         = Color(0xFF690005)
)