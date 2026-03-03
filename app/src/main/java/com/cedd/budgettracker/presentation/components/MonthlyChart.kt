package com.cedd.budgettracker.presentation.components

import android.graphics.BlurMaskFilter
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses

private val ElectricBlue = Color(0xFF0EA5E9)
private val NeonBlue     = Color(0xFF38BDF8)
private val OverBudget   = Color(0xFFF87171)
private val TrackColor   = Color(0xFF1B3A5C)

@Composable
fun MonthlySpendingChart(
    sessions: List<BudgetSessionWithExpenses>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) return

    val recent   = sessions.take(6).reversed()
    val maxSpent = recent.maxOf { it.expenses.sumOf { e -> e.amount } }.coerceAtLeast(1.0)

    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }

    Column(modifier = modifier) {
        Text(
            "SPENDING TREND",
            style         = MaterialTheme.typography.labelSmall,
            fontWeight    = FontWeight.Bold,
            color         = NeonBlue,
            letterSpacing = 1.sp,
            fontSize      = 10.sp
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier              = Modifier.fillMaxWidth().height(110.dp),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recent.forEachIndexed { i, session ->
                val spent  = session.expenses.sumOf { it.amount }
                val ratio  = (spent / maxSpent).toFloat()
                val isOver = spent > session.session.initialBudget
                val isTall = ratio == recent.maxOf { r ->
                    (r.expenses.sumOf { it.amount } / maxSpent).toFloat()
                }

                val animRatio by animateFloatAsState(
                    targetValue   = if (triggered) ratio else 0f,
                    animationSpec = tween(900, delayMillis = i * 80),
                    label         = "bar_${ session.session.id }"
                )

                Column(
                    modifier              = Modifier.weight(1f),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Canvas(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        val barH = size.height * animRatio
                        val top  = size.height - barH
                        val barColor = if (isOver) OverBudget else ElectricBlue

                        // Glow layer
                        drawIntoCanvas { canvas ->
                            val glowPaint = Paint().apply {
                                asFrameworkPaint().apply {
                                    isAntiAlias = true
                                    color = barColor.copy(alpha = 0.35f).toArgb()
                                    maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
                                }
                            }
                            canvas.drawRoundRect(
                                left    = -4f,
                                top     = top - 4f,
                                right   = size.width + 4f,
                                bottom  = size.height + 4f,
                                radiusX = 6f,
                                radiusY = 6f,
                                paint   = glowPaint
                            )
                        }

                        // Track (empty bar)
                        drawRoundRect(
                            color        = TrackColor,
                            topLeft      = Offset(0f, 0f),
                            size         = Size(size.width, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )

                        // Filled bar with gradient
                        if (barH > 0) {
                            drawRoundRect(
                                brush        = Brush.verticalGradient(
                                    colors = if (isOver)
                                        listOf(OverBudget, OverBudget.copy(alpha = 0.6f))
                                    else
                                        listOf(NeonBlue, ElectricBlue.copy(alpha = 0.5f)),
                                    startY = top,
                                    endY   = size.height
                                ),
                                topLeft      = Offset(0f, top),
                                size         = Size(size.width, barH),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = session.session.name.take(5),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color    = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
