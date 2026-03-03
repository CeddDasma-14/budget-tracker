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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cedd.budgettracker.data.local.entity.ExpenseEntity
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import com.cedd.budgettracker.domain.model.ExpenseCategory
import androidx.compose.foundation.clickable
import com.cedd.budgettracker.presentation.components.FullScreenImageDialog
import com.cedd.budgettracker.presentation.components.MonthlySpendingChart
import com.cedd.budgettracker.presentation.components.SpendingInsightsCard
import com.cedd.budgettracker.presentation.utils.CurrencyUtils
import com.cedd.budgettracker.presentation.utils.ThousandSeparatorTransformation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.templateSavedMessage) {
        state.templateSavedMessage?.let { msg ->
            val display = if (msg.contains("updated successfully"))
                msg  // edit-save message — show as-is
            else
                "$msg — load it from Budget screen \uD83D\uDCCB"  // template-copy message
            snackbarHostState.showSnackbar(display)
            viewModel.clearTemplateSavedMessage()
        }
    }

    // Edit session bottom sheet
    state.editTargetSession?.let { session ->
        EditSessionSheet(
            session        = session,
            editName       = state.editSessionName,
            editBudget     = state.editInitialBudget,
            onNameChange   = viewModel::updateEditName,
            onBudgetChange = viewModel::updateEditBudget,
            onSave         = viewModel::saveEditSession,
            onTogglePaid   = viewModel::toggleExpensePaidInSession,
            onDeleteExpense = viewModel::deleteExpenseInSession,
            onAddExpense   = { title, amount, category -> viewModel.addExpenseToSession(title, amount, category) },
            onDismiss      = viewModel::hideEditSheet
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "History",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            "Past Budget Sessions",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

            state.allSessions.isEmpty() -> {
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

                    // Search bar
                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::updateSearch,
                            placeholder = { Text("Search budgets by name…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (state.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.updateSearch("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }

                    // Show "no results" when search yields nothing
                    if (state.searchQuery.isNotBlank() && state.sessions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                                    Text("No budgets match \"${state.searchQuery}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {

                        // Monthly spending chart (always uses all sessions for trend)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0x801E3252))
                            ) {
                                MonthlySpendingChart(
                                    sessions = state.allSessions,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // Spending insights (always uses all sessions)
                        item {
                            SpendingInsightsCard(
                                sessions = state.allSessions,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        // ── Month filter chips ───────────────────────────────────
                        if (state.availableMonths.size > 1) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Filter by Month",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (state.selectedYearMonth != null) {
                                            TextButton(onClick = viewModel::clearMonthFilter) {
                                                Text("Show All", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        state.availableMonths.forEach { ym ->
                                            val label = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                                .format(java.util.GregorianCalendar(ym.year, ym.month - 1, 1).time)
                                            FilterChip(
                                                selected = state.selectedYearMonth == ym,
                                                onClick = { viewModel.selectMonth(ym) },
                                                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Monthly combined summary card ────────────────────────
                        state.monthlySummary?.let { summary ->
                            item {
                                MonthlySummaryCard(
                                    summary = summary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        // Session history cards
                        items(items = state.sessions, key = { it.session.id }) { sessionWithExpenses ->
                            SessionHistoryCard(
                                sessionWithExpenses = sessionWithExpenses,
                                isExpanded = sessionWithExpenses.session.id in state.expandedSessionIds,
                                onToggleExpand = { viewModel.toggleExpand(sessionWithExpenses.session.id) },
                                onDeleteRequest = { viewModel.requestDeleteSession(sessionWithExpenses.session.id) },
                                onExportCsv = { viewModel.exportCsv(sessionWithExpenses.session.id) },
                                onCopyAsTemplate = { viewModel.saveSessionAsTemplate(sessionWithExpenses.session.id) },
                                onEditRequest = { viewModel.showEditSheet(sessionWithExpenses) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
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
    onCopyAsTemplate: () -> Unit,
    onEditRequest: () -> Unit,
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
                            // Edit session
                            IconButton(onClick = onEditRequest) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Session",
                                    tint = Color(0xFF38BDF8).copy(alpha = 0.8f)
                                )
                            }
                            // Copy as template
                            IconButton(onClick = onCopyAsTemplate) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy as Template",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
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
                            valueColor = if (isOverBudget) MaterialTheme.colorScheme.error else Color(0xFF00BFA5),
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
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00BFA5))
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

                        // Category breakdown
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "By Category",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val byCat = expenses
                            .groupBy { ExpenseCategory.fromName(it.category) }
                            .map { (cat, items) -> cat to items.sumOf { it.amount } }
                            .sortedByDescending { it.second }
                        byCat.forEach { (cat, total) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(cat.emoji, fontSize = 12.sp)
                                    Text(
                                        cat.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    CurrencyUtils.formatPhp(total),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseHistoryRow(expense: ExpenseEntity) {
    var showFullScreen by remember { mutableStateOf(false) }

    if (showFullScreen && expense.receiptPath != null) {
        FullScreenImageDialog(
            path      = expense.receiptPath,
            onDismiss = { showFullScreen = false }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (expense.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (expense.isPaid) Color(0xFF00BFA5) else MaterialTheme.colorScheme.outline,
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
            if (expense.notes.isNotBlank()) {
                Text(
                    text = expense.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            } else {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
        Text(
            text = CurrencyUtils.formatPhp(expense.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )
        expense.receiptPath?.let { path ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showFullScreen = true }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(path)).crossfade(true).build(),
                    contentDescription = "Tap to view receipt",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Small expand hint overlay in bottom-right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(topStart = 4.dp))
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun MonthlySummaryCard(
    summary: MonthSummary,
    modifier: Modifier = Modifier
) {
    val isOver = summary.totalRemaining < 0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                Text(
                    "${summary.entryCount} income flow${if (summary.entryCount > 1) "s" else ""} this month",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BudgetMetric("Total Budget",  CurrencyUtils.formatPhp(summary.totalBudget))
                BudgetMetric("Total Spent",   CurrencyUtils.formatPhp(summary.totalSpent))
                BudgetMetric(
                    label      = if (isOver) "OVER" else "Remaining",
                    value      = CurrencyUtils.formatPhp(summary.totalRemaining),
                    valueColor = if (isOver) MaterialTheme.colorScheme.error else Color(0xFF00BFA5)
                )
            }
        }
    }
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

// ── Edit session bottom sheet ──────────────────────────────────────────────────

private val SheetBg     = Color(0xFF0F2035)   // Opaque — no background bleed-through
private val SheetBorder = Color(0xFF1B3A5C)
private val NeonBlue    = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSessionSheet(
    session: BudgetSessionWithExpenses,
    editName: String,
    editBudget: String,
    onNameChange: (String) -> Unit,
    onBudgetChange: (String) -> Unit,
    onSave: () -> Unit,
    onTogglePaid: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onAddExpense: (title: String, amount: Double, category: String) -> Unit,
    onDismiss: () -> Unit
) {
    var addTitle    by remember { mutableStateOf("") }
    var addAmount   by remember { mutableStateOf("") }
    var addCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var catExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = SheetBg,
        contentColor     = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                Text("Edit Session", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
            }

            // ── Session fields ─────────────────────────────────────────────
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor       = Color.White,
                unfocusedTextColor     = Color.White,
                focusedLabelColor      = Color.White.copy(alpha = 0.8f),
                unfocusedLabelColor    = Color.White.copy(alpha = 0.6f),
                focusedPlaceholderColor   = Color.White.copy(alpha = 0.4f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                focusedLeadingIconColor   = Color.White,
                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor     = NeonBlue,
                unfocusedBorderColor   = SheetBorder,
                cursorColor            = Color.White,
                focusedContainerColor  = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedPrefixColor     = Color.White,
                unfocusedPrefixColor   = Color.White,
            )

            OutlinedTextField(
                value = editName,
                onValueChange = onNameChange,
                label = { Text("Session Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            OutlinedTextField(
                value = editBudget,
                onValueChange = { onBudgetChange(CurrencyUtils.cleanAmountInput(it)) },
                label = { Text("Initial Budget") },
                singleLine = true,
                prefix = { Text("₱", fontWeight = FontWeight.Bold) },
                visualTransformation = ThousandSeparatorTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Changes", fontWeight = FontWeight.SemiBold)
            }

            // ── Expenses ───────────────────────────────────────────────────
            HorizontalDivider(color = SheetBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "EXPENSES",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = NeonBlue, letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${session.expenses.size} item(s)",
                    fontSize = 10.sp, color = Color(0xFF64748B)
                )
            }

            if (session.expenses.isEmpty()) {
                Text(
                    "No expenses yet. Add one below.",
                    fontSize = 12.sp, color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                session.expenses.forEach { expense ->
                    EditableExpenseRow(
                        expense    = expense,
                        onTogglePaid = { onTogglePaid(expense) },
                        onDelete   = { onDeleteExpense(expense) }
                    )
                }
            }

            // ── Add expense ────────────────────────────────────────────────
            HorizontalDivider(color = SheetBorder)

            Text(
                "ADD EXPENSE",
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = NeonBlue, letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = addTitle,
                onValueChange = { addTitle = it },
                label = { Text("Title") },
                placeholder = { Text("e.g. Groceries") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = addAmount,
                    onValueChange = { addAmount = CurrencyUtils.cleanAmountInput(it) },
                    label = { Text("Amount") },
                    singleLine = true,
                    prefix = { Text("₱", fontWeight = FontWeight.Bold) },
                    visualTransformation = ThousandSeparatorTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = fieldColors
                )

                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "${addCategory.emoji} ${addCategory.label}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        ExpenseCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.label}") },
                                onClick = { addCategory = cat; catExpanded = false }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val amt = addAmount.replace(",", "").toDoubleOrNull() ?: 0.0
                    if (addTitle.isNotBlank() && amt > 0) {
                        onAddExpense(addTitle, amt, addCategory.name)
                        addTitle  = ""
                        addAmount = ""
                        addCategory = ExpenseCategory.OTHER
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B3A5C),
                    contentColor   = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Expense", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun EditableExpenseRow(
    expense: ExpenseEntity,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = ExpenseCategory.fromName(expense.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C1829), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Category bubble
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(cat.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(cat.emoji, fontSize = 13.sp) }

        // Title + amount
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title.ifBlank { "(Untitled)" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (expense.isPaid) Color(0xFF64748B) else Color.White,
                textDecoration = if (expense.isPaid) TextDecoration.LineThrough else null
            )
            Text(
                text = CurrencyUtils.formatPhp(expense.amount),
                fontSize = 11.sp, color = cat.color
            )
        }

        // Paid toggle
        IconButton(onClick = onTogglePaid, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (expense.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Toggle paid",
                tint = if (expense.isPaid) Color(0xFF34D399) else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }

        // Delete
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete expense",
                tint = Color(0xFFF87171).copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
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
