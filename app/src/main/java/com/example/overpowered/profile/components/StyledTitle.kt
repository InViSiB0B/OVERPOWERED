package com.example.overpowered.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overpowered.data.Title
import com.example.overpowered.data.TitleCatalog


@Composable
fun StyledTitle(
    titleId: String?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    includeBackground: Boolean = false,
    backgroundColor: Color = Color(0xFF2A2A2A).copy(alpha = 0.8f),
    cornerRadius: Dp = 12.dp,
    padding: Dp = 6.dp
) {
    if (titleId == null) return

    val title = TitleCatalog.getTitleById(titleId) ?: return

    if (includeBackground) {
        Box(
            modifier = modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .padding(horizontal = padding, vertical = (padding / 2))
        ) {
            TitleText(title = title, fontSize = fontSize)
        }
    } else {
        TitleText(
            title = title,
            fontSize = fontSize,
            modifier = modifier
        )
    }
}

@Composable
private fun TitleText(
    title: Title,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val textStyle = TextStyle(
        fontSize = fontSize,
        fontWeight = title.fontWeight,
        shadow = if (title.glowColor != null) {
            Shadow(
                color = title.glowColor,
                offset = Offset(0f, 0f),
                blurRadius = 8f
            )
        } else null
    )

    if (title.isGradient && title.gradientColors != null && title.gradientColors.size >= 2) {
        // Gradient text effect
        Text(
            text = title.name,
            style = textStyle,
            modifier = modifier.drawBehind {
                val brush = Brush.linearGradient(
                    colors = title.gradientColors
                )
                drawRect(brush = brush)
            },
            color = Color.White // This will be overridden by the gradient
        )
    } else {
        // Solid color text
        Text(
            text = title.name,
            style = textStyle,
            color = title.color,
            modifier = modifier
        )
    }
}

@Composable
fun PlayerNameWithTitle(
    playerName: String,
    discriminator: String,
    titleId: String?,
    modifier: Modifier = Modifier,
    nameSize: TextUnit = 16.sp,
    titleSize: TextUnit = 14.sp,
    nameColor: Color? = null,
    discriminatorColor: Color? = null,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // Player name with discriminator
        Text(
            text = "$playerName#$discriminator",
            fontSize = nameSize,
            fontWeight = FontWeight.Bold,
            color = nameColor ?: MaterialTheme.colorScheme.onBackground
        )

        // Title (if equipped)
        if (titleId != null) {
            StyledTitle(
                titleId = titleId,
                fontSize = titleSize,
                includeBackground = true
            )
        }
    }
}

@Composable
fun CompactPlayerNameWithTitle(
    playerName: String,
    titleId: String?,
    modifier: Modifier = Modifier,
    nameSize: TextUnit = 14.sp,
    titleSize: TextUnit = 12.sp,
    nameColor: Color? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Just the player name (no discriminator for compact view)
        Text(
            text = playerName,
            fontSize = nameSize,
            fontWeight = FontWeight.Medium,
            color = nameColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Title (if equipped)
        if (titleId != null) {
            StyledTitle(
                titleId = titleId,
                fontSize = titleSize,
                includeBackground = true,
                cornerRadius = 8.dp,
                padding = 4.dp
            )
        }
    }
}