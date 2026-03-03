package com.cedd.budgettracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cedd.budgettracker.data.local.dao.BudgetSessionDao
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.NumberFormat
import java.util.Locale

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BudgetWidgetEntryPoint {
    fun sessionDao(): BudgetSessionDao
}

/**
 * Home screen widget — shows the most recent session's remaining balance.
 * Uses Glance (Jetpack Compose for widgets).
 */
class BudgetWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, BudgetWidgetEntryPoint::class.java)
            .sessionDao()
        val session = dao.getLatestSessionWithExpenses()
        provideContent { BudgetWidgetContent(session) }
    }
}

private fun formatPhp(amount: Double): String {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    fmt.minimumFractionDigits = 0
    fmt.maximumFractionDigits = 2
    return "₱${fmt.format(amount)}"
}

@Composable
private fun BudgetWidgetContent(session: BudgetSessionWithExpenses?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF0C1829)))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (session == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CeddFlow",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = "No budget saved yet",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Open app to get started",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF64748B)),
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            val totalSpent = session.expenses.sumOf { it.amount }
            val remaining = session.session.initialBudget - totalSpent
            val isOverBudget = remaining < 0
            val balanceColor = if (isOverBudget) Color(0xFFF87171) else Color(0xFF34D399)

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Header row: app name + session name
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CeddFlow",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF38BDF8)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = session.session.name.take(18),
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF64748B)),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(GlanceModifier.height(8.dp))

                // Balance label
                Text(
                    text = if (isOverBudget) "OVER BUDGET" else "REMAINING",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF64748B)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(GlanceModifier.height(2.dp))

                // Large balance
                Text(
                    text = formatPhp(if (isOverBudget) -remaining else remaining),
                    style = TextStyle(
                        color = ColorProvider(balanceColor),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(GlanceModifier.height(4.dp))

                // Budget total and item count
                Text(
                    text = "Budget ${formatPhp(session.session.initialBudget)} · ${session.expenses.size} items",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF64748B)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
