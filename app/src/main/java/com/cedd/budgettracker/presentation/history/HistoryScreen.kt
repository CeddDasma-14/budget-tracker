package com.cedd.budgettracker.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cedd.budgettracker.data.local.entity.ExpenseEntity
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import com.cedd.budgettracker.presentation.components.MonthlySpendingChart
import com.cedd.budgettracker.presentation.components.SpendingInsightsCard
import com.cedd.budgettracker.presentation.utils.CurrencyUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            state.sessions.isEmpty() -> {
                EmptyHistoryPlaceholder(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Monthly spending chart
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            MonthlySpendingChart(
                                sessions = state.sessions,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Spending insights
                    item {
                        SpendingInsightsCard(
                            sessions = state.sessions,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Session history cards
                    items(items = state.sessions, key = { it.session.id }) { sessionWithExpenses ->
                        SessionHistoryCard(
                            sessionWithExpenses = sessionWithExpenses,
                            isExpanded = sessionWithExpenses.session.id in state.expandedSessionIds,
                            onToggleExpand = { viewModel.toggleExpand(sessionWithExpenses.session.id) },
                            onDeleteRequest = { viewModel.requestDeleteSession(sessionWithExpenses.session.id) },
                            onExportCsv = { viewModel.exportCsv(sessionWithExpenses.session.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (state.deleteConfirmSessionId != null) {
            AlertDialog(
                onDismissRequest = viewModel::cancelDelete,
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
                title = { Text("Delete Budget?") },
                text = { Text("This will permanently remove the session and all its expenses and receipts. This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmDeleteSession,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDelete) { Text("Cancel") }
                }
            )
        }
    }
}

// ── Session card ───────────────────────────────────────────────────────────────

@Composable
private fun SessionHistoryCard(
    sessionWithExpenses: BudgetSessionWithExpenses,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteRequest: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = sessionWithExpenses.session
    val expenses = sessionWithExpenses.expenses
    val totalSpent = expenses.sumOf { it.amount }
    val remaining = session.initialBudget - totalSpent
    val isOverBudget = remaining < 0
    val dateFormat = SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = if (isExpanded) RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                else RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = dateFormat.format(Date(session.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Row {
                            // CSV Export
                            IconButton(onClick = onExportCsv) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Export CSV",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            IconButton(onClick = onDeleteRequest) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = onToggleExpand) {
                                Icon(
                                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BudgetMetric(label = "Initial", value = CurrencyUtils.formatPhp(session.initialBudget))
                        BudgetMetric(label = "Spent", value = CurrencyUtils.formatPhp(totalSpent), valueColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        BudgetMetric(
                            label = if (isOverBudget) "OVER" else "Left",
                            value = CurrencyUtils.formatPhp(remaining),
                            valueColor = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF00B7B5),
                            labelColor = if (isOverBudget) MaterialTheme.colorScheme.error else null
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("${expenses.size} expenses") }, leadingIcon = {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        })
                        val paidCount = expenses.count { it.isPaid }
                        if (paidCount > 0) {
                            AssistChip(onClick = {}, label = { Text("$paidCount paid") }, leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00B7B5))
                            })
                        }
                        val receiptCount = expenses.count { it.receiptPath != null }
                        if (receiptCount > 0) {
                            AssistChip(onClick = {}, label = { Text("$receiptCount receipts") }, leadingIcon = {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            })
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (expenses.isEmpty()) {
                        Text(
                            "No expenses recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        expenses.forEach { expense -> ExpenseHistoryRow(expense = expense) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseHistoryRow(expense: ExpenseEntity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (expense.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (expense.isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title.ifBlank { "(Untitled)" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textDecoration = if (expense.isPaid) TextDecoration.LineThrough else null,
                color = if (expense.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = expense.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
        Text(
            text = CurrencyUtils.formatPhp(expense.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )
        expense.receiptPath?.let { path ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun BudgetMetric(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    labelColor: Color? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor ?: MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun EmptyHistoryPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text("No saved budgets yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("Go back and save your first budget!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}
