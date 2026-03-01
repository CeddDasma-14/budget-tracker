package com.cedd.budgettracker.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses

@Composable
fun MonthlySpendingChart(
    sessions: List<BudgetSessionWithExpenses>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) return

    val recent = sessions.take(6).reversed()   // Show last 6 sessions oldest → newest
    val maxSpent = recent.maxOf { it.expenses.sumOf { e -> e.amount } }.coerceAtLeast(1.0)

    val barColor  = Color(0xFF2E7D32)
    val overColor = Color(0xFFD32F2F)
    val onSurface = MaterialTheme.colorScheme.onSurface

    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }

    Column(modifier = modifier) {
        Text(
            "Spending Trend",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            recent.forEach { session ->
                val spent = session.expenses.sumOf { it.amount }
                val ratio = (spent / maxSpent).toFloat()
                val isOver = spent > session.session.initialBudget

                val animRatio by animateFloatAsState(
                    targetValue = if (triggered) ratio else 0f,
                    animationSpec = tween(800),
                    label = "bar_${ session.session.id }"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val barH = size.height * animRatio
                        drawRoundRect(
                            color = if (isOver) overColor else barColor,
                            topLeft = Offset(0f, size.height - barH),
                            size = Size(size.width, barH),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = session.session.name.take(5),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
