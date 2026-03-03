package com.cedd.budgettracker.presentation.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.budgettracker.data.local.relation.GoalWithContributions
import com.cedd.budgettracker.presentation.utils.CurrencyUtils
import com.cedd.budgettracker.presentation.utils.ThousandSeparatorTransformation
import com.cedd.budgettracker.presentation.utils.glowEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Design tokens ─────────────────────────────────────────────────────────────

private val BgColor      = Color.Transparent
private val CardColor    = Color(0x801E3252)
private val DialogColor  = Color(0xFF0F2035)   // Opaque — prevents bleed-through in dialogs
private val BorderColor  = Color(0xFF1B3A5C)
private val NeonBlue     = Color(0xFF38BDF8)
private val TextPrimary  = Color(0xFFF0F9FF)
private val TextMuted    = Color(0xFF94A3B8)

private val GoalColors = listOf(
    "#38BDF8", "#818CF8", "#34D399", "#FBBF24", "#F87171",
    "#A78BFA", "#FB923C", "#4ADE80", "#F472B6", "#60A5FA"
)

private val GoalEmojis = listOf(
    "🎯", "📱", "✈️", "🏠", "🚗", "💻", "🎓", "💍", "⛵", "💰",
    "🏋️", "🎸", "📷", "👜", "🌏", "🏖️", "🎮", "🐶", "💎", "🚀"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val goals   by viewModel.goals.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = BgColor,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Goals", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                        Text("Financial Targets", fontSize = 11.sp, color = NeonBlue,
                            fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgColor, titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = viewModel::showAddDialog,
                icon           = { Icon(Icons.Default.Add, null) },
                text           = { Text("New Goal", fontWeight = FontWeight.SemiBold) },
                containerColor = Color(0xFF0EA5E9),
                contentColor   = Color.White
            )
        }
    ) { paddingValues ->

        if (goals.isEmpty()) {
            GoalsEmptyState(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                onAdd    = viewModel::showAddDialog
            )
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().background(BgColor).padding(paddingValues),
                contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary header
                item {
                    GoalsSummaryCard(goals = goals)
                }

                // Active goals
                val active    = goals.filter { !it.goal.isCompleted }
                val completed = goals.filter { it.goal.isCompleted }

                if (active.isNotEmpty()) {
                    item {
                        Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = NeonBlue, letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                    items(active, key = { it.goal.id }) { goal ->
                        GoalCard(
                            goalData  = goal,
                            onContribute = { viewModel.showContribute(goal) },
                            onDelete     = { viewModel.showDeleteConfirm(goal) },
                            onToggleDone = { viewModel.markComplete(goal) }
                        )
                    }
                }

                if (completed.isNotEmpty()) {
                    item {
                        Text("COMPLETED", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399), letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp))
                    }
                    items(completed, key = { it.goal.id }) { goal ->
                        GoalCard(
                            goalData     = goal,
                            onContribute = { viewModel.showContribute(goal) },
                            onDelete     = { viewModel.showDeleteConfirm(goal) },
                            onToggleDone = { viewModel.markComplete(goal) }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────

        if (uiState.showAddDialog) {
            AddGoalDialog(
                onConfirm = { name, emoji, amount, deadline, color ->
                    viewModel.createGoal(name, emoji, amount, deadline, color)
                },
                onDismiss = viewModel::hideAddDialog
            )
        }

        uiState.contributeTarget?.let { goal ->
            ContributeDialog(
                goal      = goal,
                onConfirm = { amount, note -> viewModel.addContribution(goal.goal.id, amount, note) },
                onDismiss = viewModel::hideContribute
            )
        }

        uiState.deleteTarget?.let { goal ->
            AlertDialog(
                onDismissRequest = viewModel::hideDeleteConfirm,
                icon    = { Icon(Icons.Default.Delete, null, tint = Color(0xFFF87171)) },
                title   = { Text("Delete Goal?") },
                text    = { Text("\"${goal.goal.name}\" and all its savings history will be removed.") },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmDelete,
                        colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171))
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::hideDeleteConfirm) { Text("Cancel") }
                },
                containerColor = DialogColor
            )
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun GoalsSummaryCard(goals: List<GoalWithContributions>) {
    val totalTarget  = goals.sumOf { it.goal.targetAmount }
    val totalSaved   = goals.sumOf { it.totalSaved }
    val completedCnt = goals.count { it.isComplete }

    Card(
        modifier = Modifier.fillMaxWidth().glowEffect(NeonBlue, 12.dp, 16.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0x801E3252)),
        border   = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            SummaryMetric("Total Goals", "${goals.size}", NeonBlue)
            SummaryMetric("Completed", "$completedCnt", Color(0xFF34D399))
            SummaryMetric("Total Saved", CurrencyUtils.formatPhp(totalSaved), Color(0xFF818CF8))
            SummaryMetric("Remaining", CurrencyUtils.formatPhp((totalTarget - totalSaved).coerceAtLeast(0.0)), Color(0xFFFBBF24))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 9.sp, color = TextMuted, textAlign = TextAlign.Center)
    }
}

// ── Goal card ──────────────────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    goalData: GoalWithContributions,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
    onToggleDone: () -> Unit
) {
    val goal     = goalData.goal
    val progress = goalData.progress
    val accentColor = parseHexColor(goal.colorHex)
    var expanded by remember { mutableStateOf(false) }

    val animProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(1000),
        label         = "goal_progress_${goal.id}"
    )

    Card(
        modifier = Modifier.fillMaxWidth().glowEffect(accentColor.copy(alpha = 0.5f), 10.dp, 16.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = CardColor),
        border   = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                // Circular progress ring
                Box(
                    modifier         = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        // Track
                        drawArc(
                            color       = accentColor.copy(alpha = 0.15f),
                            startAngle  = -90f,
                            sweepAngle  = 360f,
                            useCenter   = false,
                            style       = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Fill
                        drawArc(
                            color       = accentColor,
                            startAngle  = -90f,
                            sweepAngle  = 360f * animProgress,
                            useCenter   = false,
                            style       = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(goal.emoji, fontSize = 22.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            goal.name,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 15.sp,
                            color         = TextPrimary,
                            maxLines      = 1,
                            overflow      = TextOverflow.Ellipsis,
                            modifier      = Modifier.weight(1f)
                        )
                        if (goal.isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF34D399).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("DONE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${CurrencyUtils.formatPhp(goalData.totalSaved)} of ${CurrencyUtils.formatPhp(goal.targetAmount)}",
                        fontSize = 12.sp,
                        color    = TextMuted
                    )
                    if (goal.deadline != null) {
                        Text(
                            "Due ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(goal.deadline))}",
                            fontSize = 10.sp,
                            color    = if (System.currentTimeMillis() > goal.deadline) Color(0xFFF87171) else TextMuted
                        )
                    }
                }

                // Percentage badge
                Box(
                    modifier         = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${(animProgress * 100).toInt()}%",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = accentColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Glowing progress bar
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(BorderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .glowEffect(accentColor, 8.dp, 3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor.copy(alpha = 0.7f), accentColor)
                            )
                        )
                )
            }

            Spacer(Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onContribute,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = accentColor)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Savings", fontSize = 12.sp, color = accentColor)
                }

                IconButton(
                    onClick  = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = TextMuted, modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onToggleDone, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (goal.isCompleted) Icons.Default.Undo else Icons.Default.CheckCircle,
                        null,
                        tint     = if (goal.isCompleted) TextMuted else Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFF87171).copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp))
                }
            }

            // Contributions history
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = BorderColor)
                    Spacer(Modifier.height(8.dp))
                    if (goalData.contributions.isEmpty()) {
                        Text("No contributions yet", fontSize = 11.sp, color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        goalData.contributions.sortedByDescending { it.date }.take(5).forEach { c ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (c.note.isNotBlank()) c.note else "Contribution",
                                        fontSize = 12.sp, color = TextPrimary, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(c.date)),
                                        fontSize = 10.sp, color = TextMuted
                                    )
                                }
                                Text(
                                    "+${CurrencyUtils.formatPhp(c.amount)}",
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun GoalsEmptyState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Box(modifier = modifier.background(BgColor), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🎯", fontSize = 56.sp)
            Text("No Goals Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Set a financial target and track your\nprogress toward achieving it.",
                fontSize = 13.sp, color = TextMuted, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick        = onAdd,
                colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                shape          = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create First Goal", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Add Goal dialog ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    onConfirm: (name: String, emoji: String, amount: Double, deadline: Long?, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name       by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(GoalEmojis[0]) }
    var selectedColor by remember { mutableStateOf(GoalColors[0]) }
    var showDeadline  by remember { mutableStateOf(false) }
    var deadlineMs    by remember { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState()

    if (showDeadline) {
        DatePickerDialog(
            onDismissRequest = { showDeadline = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineMs = datePickerState.selectedDateMillis
                    showDeadline = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeadline = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DialogColor,
        title = {
            Text("New Goal", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Emoji picker row
                Text("Pick an icon", fontSize = 11.sp, color = TextMuted)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GoalEmojis.size) { i ->
                        val e = GoalEmojis[i]
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (e == selectedEmoji) NeonBlue.copy(0.2f) else BorderColor)
                                .border(if (e == selectedEmoji) 1.5.dp else 0.dp, NeonBlue, CircleShape)
                                .clickable { selectedEmoji = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 16.sp) }
                    }
                }

                // Goal name
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Goal Name") },
                    placeholder   = { Text("e.g. New iPhone") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = darkFieldColors()
                )

                // Target amount
                OutlinedTextField(
                    value         = amountText,
                    onValueChange = { amountText = CurrencyUtils.cleanAmountInput(it) },
                    label         = { Text("Target Amount") },
                    prefix        = { Text("₱") },
                    singleLine    = true,
                    visualTransformation = ThousandSeparatorTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = darkFieldColors()
                )

                // Deadline (optional)
                OutlinedCard(
                    onClick = { showDeadline = true },
                    border  = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    colors  = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Event, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Text(
                            deadlineMs?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) }
                                ?: "Set deadline (optional)",
                            fontSize = 13.sp,
                            color    = if (deadlineMs != null) TextPrimary else TextMuted
                        )
                        Spacer(Modifier.weight(1f))
                        if (deadlineMs != null) {
                            IconButton(onClick = { deadlineMs = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Color picker row
                Text("Accent color", fontSize = 11.sp, color = TextMuted)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(GoalColors.size) { i ->
                        val c = GoalColors[i]
                        val col = parseHexColor(c)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(if (c == selectedColor) 2.5.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedColor = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                    onConfirm(name, selectedEmoji, amount, deadlineMs, selectedColor)
                },
                enabled = name.isNotBlank() && (amountText.replace(",", "").toDoubleOrNull() ?: 0.0) > 0,
                colors  = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) { Text("Create Goal", color = Color(0xFF001829), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

// ── Contribute dialog ─────────────────────────────────────────────────────────

@Composable
private fun ContributeDialog(
    goal: GoalWithContributions,
    onConfirm: (amount: Double, note: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var note       by remember { mutableStateOf("") }
    val accentColor = parseHexColor(goal.goal.colorHex)
    val remaining = (goal.goal.targetAmount - goal.totalSaved).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DialogColor,
        icon = { Text(goal.goal.emoji, fontSize = 28.sp) },
        title = {
            Text("Add Savings", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Goal: ${goal.goal.name}",
                    fontSize = 13.sp, color = TextMuted
                )
                Text(
                    "Still needed: ${CurrencyUtils.formatPhp(remaining)}",
                    fontSize = 12.sp, color = accentColor
                )
                OutlinedTextField(
                    value         = amountText,
                    onValueChange = { amountText = CurrencyUtils.cleanAmountInput(it) },
                    label         = { Text("Amount") },
                    prefix        = { Text("₱") },
                    singleLine    = true,
                    visualTransformation = ThousandSeparatorTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = darkFieldColors()
                )
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("Note (optional)") },
                    placeholder   = { Text("e.g. From March savings") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = darkFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
                    onConfirm(amount, note)
                },
                enabled = (amountText.replace(",", "").toDoubleOrNull() ?: 0.0) > 0,
                colors  = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color(0xFF38BDF8) }

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor      = TextPrimary,
    unfocusedTextColor    = TextPrimary,
    focusedLabelColor     = NeonBlue,
    unfocusedLabelColor   = TextMuted,
    focusedBorderColor    = NeonBlue,
    unfocusedBorderColor  = BorderColor,
    cursorColor           = NeonBlue,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)
