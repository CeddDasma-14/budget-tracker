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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import com.cedd.budgettracker.domain.model.ExpenseCategory
import com.cedd.budgettracker.presentation.utils.CurrencyUtils

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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Spending Insights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Top categories across all sessions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))

            insights.forEach { insight ->
                InsightRow(insight)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InsightRow(insight: CategoryInsight) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(insight.category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(insight.category.emoji, fontSize = 13.sp)
            }

            Text(
                text = insight.category.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${insight.percentage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = insight.category.color,
                fontSize = 11.sp
            )

            Text(
                text = CurrencyUtils.formatPhp(insight.total),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (insight.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = insight.category.color,
            trackColor = insight.category.color.copy(alpha = 0.15f)
        )
    }
}
