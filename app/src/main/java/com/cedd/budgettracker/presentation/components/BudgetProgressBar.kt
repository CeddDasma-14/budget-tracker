package com.cedd.budgettracker.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedd.budgettracker.presentation.utils.glowEffect

@Composable
fun BudgetProgressBar(
    spent: Double,
    total: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) (spent / total).coerceIn(0.0, 1.0).toFloat() else 0f
    val pct      = (progress * 100).toInt()

    val barColor by animateColorAsState(
        targetValue = when {
            progress >= 1f   -> Color(0xFFF87171)   // Red — over budget
            progress >= 0.8f -> Color(0xFFFBBF24)   // Amber — warning
            progress >= 0.5f -> Color(0xFFF59E0B)   // Orange — caution
            else             -> Color(0xFF38BDF8)   // Electric Blue — healthy
        },
        animationSpec = tween(400),
        label         = "bar_color"
    )

    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(800),
        label         = "progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = "BUDGET USED",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.8.sp,
                fontSize = 10.sp
            )
            Text(
                text       = "$pct%",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = barColor
            )
        }

        Spacer(Modifier.height(6.dp))

        // Glowing progress track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            // Track background
            LinearProgressIndicator(
                progress              = { 1f },
                modifier              = Modifier.fillMaxSize(),
                color                 = Color(0xFF1B3A5C),
                trackColor            = Color(0xFF1B3A5C)
            )
            // Filled bar with glow
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .glowEffect(barColor, glowRadius = 10.dp, cornerRadius = 4.dp)
            ) {
                LinearProgressIndicator(
                    progress   = { 1f },
                    modifier   = Modifier.fillMaxSize(),
                    color      = barColor,
                    trackColor = barColor
                )
            }
        }
    }
}
