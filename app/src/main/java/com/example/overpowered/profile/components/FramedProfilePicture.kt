package com.example.overpowered.profile.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.overpowered.data.FrameCatalog
import com.example.overpowered.data.FramePattern
import com.example.overpowered.data.ProfileFrame
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FramedProfilePicture(
    profileImageUrl: String?,
    frameId: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    iconSize: Dp = size / 2
) {
    val frame = frameId?.let { FrameCatalog.getFrameById(it) }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect (if frame has glow)
        if (frame?.glowColor != null) {
            GlowEffect(
                color = frame.glowColor,
                size = size,
                pattern = frame.pattern
            )
        }

        // Profile picture with frame
        Box(
            modifier = Modifier
                .size(size)
                .then(
                    if (frame != null) {
                        Modifier.drawFrame(frame, size)
                    } else {
                        Modifier.border(2.dp, Color(0xFFE2E8F0), CircleShape)
                    }
                )
                .padding(if (frame != null) frame.borderWidth.dp else 2.dp)
                .clip(CircleShape)
                .background(Color(0xFF667EEA)),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(profileImageUrl),
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

// Custom modifier to draw frames
fun Modifier.drawFrame(frame: ProfileFrame, size: Dp): Modifier = this.then(
    when (frame.pattern) {
        FramePattern.SOLID, FramePattern.GRADIENT -> {
            Modifier.border(
                width = frame.borderWidth.dp,
                brush = frame.borderBrush,
                shape = CircleShape
            )
        }

        FramePattern.DOUBLE -> {
            Modifier
                .border(
                    width = frame.borderWidth.dp,
                    brush = frame.borderBrush,
                    shape = CircleShape
                )
                .padding(2.dp)
                .border(
                    width = (frame.borderWidth / 2).dp,
                    brush = frame.borderBrush,
                    shape = CircleShape
                )
        }

        FramePattern.GLOW -> {
            Modifier.border(
                width = frame.borderWidth.dp,
                brush = frame.borderBrush,
                shape = CircleShape
            )
        }

        FramePattern.DASHED -> {
            Modifier.drawBehind {
                val strokeWidth = frame.borderWidth * density
                val radius = size.toPx() / 2
                val dashLength = 20f
                val gapLength = 10f

                var angle = 0f
                while (angle < 360) {
                    val startAngle = angle
                    val sweepAngle = (dashLength / (2 * Math.PI * radius) * 360).toFloat()

                    drawArc(
                        brush = frame.borderBrush,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )

                    angle += sweepAngle + (gapLength / (2 * Math.PI * radius) * 360).toFloat()
                }
            }
        }

        FramePattern.RAINBOW, FramePattern.ANIMATED_SHINE -> {
            Modifier.border(
                width = frame.borderWidth.dp,
                brush = frame.borderBrush,
                shape = CircleShape
            )
        }
    }
)

@Composable
fun GlowEffect(
    color: Color,
    size: Dp,
    pattern: FramePattern
) {
    var alpha by remember { mutableStateOf(0.3f) }

    // Animate glow for certain patterns
    if (pattern == FramePattern.GLOW || pattern == FramePattern.ANIMATED_SHINE) {
        LaunchedEffect(Unit) {
            while (true) {
                alpha = 0.3f
                delay(1000)
                alpha = 0.6f
                delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(size + 8.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}