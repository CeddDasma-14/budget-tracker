package com.cedd.budgettracker.presentation.utils

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simulates a frosted-glass card: lighter-navy-to-dark vertical gradient + white top-edge border.
 * Apply to the Card modifier and set containerColor = Color.Transparent.
 */
fun Modifier.frostedGlass(cornerRadius: Dp = 14.dp): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E3252).copy(alpha = 0.82f),
                    Color(0xFF0C1829).copy(alpha = 0.90f)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
}

/**
 * Draws a soft colored glow behind the composable using BlurMaskFilter (API 26+).
 */
fun Modifier.glowEffect(
    glowColor: Color,
    glowRadius: Dp = 16.dp,
    cornerRadius: Dp = 16.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val glowPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = glowColor.copy(alpha = 0.45f).toArgb()
                maskFilter = BlurMaskFilter(glowRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        }
        val r = glowRadius.toPx() * 0.4f
        canvas.drawRoundRect(
            left   = -r,
            top    = -r,
            right  = size.width + r,
            bottom = size.height + r,
            radiusX = cornerRadius.toPx(),
            radiusY = cornerRadius.toPx(),
            paint  = glowPaint
        )
    }
}
