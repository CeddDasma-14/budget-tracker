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

@Composable
fun BudgetProgressBar(
    spent: Double,
    total: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) (spent / total).coerceIn(0.0, 1.0).toFloat() else 0f
    val pct = (progress * 100).toInt()

    val barColor by animateColorAsState(
        targetValue = when {
            progress >= 1f   -> Color(0xFFD32F2F)  // Red — over budget
            progress >= 0.8f -> Color(0xFFFF6F00)  // Amber — warning
            progress >= 0.5f -> Color(0xFFF9A825)  // Yellow — caution
            else             -> Color(0xFF2E7D32)  // Green — healthy
        },
        animationSpec = tween(400),
        label = "bar_color"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budget Used",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = barColor,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
        )
    }
}
