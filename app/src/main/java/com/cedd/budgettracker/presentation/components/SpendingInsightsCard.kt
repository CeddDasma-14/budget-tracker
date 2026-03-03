package com.cedd.budgettracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import com.cedd.budgettracker.domain.model.ExpenseCategory
import com.cedd.budgettracker.presentation.utils.CurrencyUtils
import com.cedd.budgettracker.presentation.utils.glowEffect

private val NeonBlue = Color(0xFF38BDF8)
private val CardBg   = Color(0x801E3252)
private val Border   = Color(0x20FFFFFF)

data class CategoryInsight(
    val category: ExpenseCategory,
    val total: Double,
    val percentage: Float
)

fun buildInsights(sessions: List<BudgetSessionWithExpenses>): List<CategoryInsight> {
    val totals = mutableMapOf<ExpenseCategory, Double>()
    sessions.flatMap { it.expenses }.forEach { expense ->
        val cat = ExpenseCategory.fromName(expense.category)
        totals[cat] = (totals[cat] ?: 0.0) + expense.amount
    }
    val grandTotal = totals.values.sum().coerceAtLeast(1.0)
    return totals.entries
        .sortedByDescending { it.value }
        .take(5)
        .map { (cat, total) ->
            CategoryInsight(cat, total, (total / grandTotal * 100).toFloat())
        }
}

@Composable
fun SpendingInsightsCard(
    sessions: List<BudgetSessionWithExpenses>,
    modifier: Modifier = Modifier
) {
    val insights = buildInsights(sessions)
    if (insights.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        border   = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "SPENDING INSIGHTS",
                style         = MaterialTheme.typography.labelSmall,
                fontWeight    = FontWeight.Bold,
                color         = NeonBlue,
                letterSpacing = 1.sp,
                fontSize      = 10.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Top categories across all sessions",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
            Spacer(Modifier.height(14.dp))

            insights.forEach { insight ->
                InsightRow(insight)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun InsightRow(insight: CategoryInsight) {
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(insight.category.color.copy(alpha = 0.15f))
                    .glowEffect(insight.category.color, glowRadius = 8.dp, cornerRadius = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(insight.category.emoji, fontSize = 15.sp)
            }

            Text(
                text       = insight.category.label,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.weight(1f),
                color      = Color(0xFFCBD5E1)
            )

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = CurrencyUtils.formatPhp(insight.total),
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFFF0F9FF),
                    fontSize   = 11.sp
                )
                Text(
                    text     = "${insight.percentage.toInt()}%",
                    fontSize = 10.sp,
                    color    = insight.category.color
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((insight.percentage / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .glowEffect(insight.category.color, glowRadius = 6.dp, cornerRadius = 2.dp)
                    .background(insight.category.color)
            )
        }
    }
}
