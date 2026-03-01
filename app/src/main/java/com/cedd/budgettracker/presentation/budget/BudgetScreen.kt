package com.cedd.budgettracker.presentation.budget

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.budgettracker.data.local.relation.TemplateWithExpenses
import com.cedd.budgettracker.domain.model.BudgetUiState
import com.cedd.budgettracker.presentation.components.BudgetProgressBar
import com.cedd.budgettracker.presentation.components.ExpenseRowItem
import com.cedd.budgettracker.presentation.utils.CurrencyUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()

    val globalPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.handleGlobalReceiptPicked(it) } }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            snackbarHostState.showSnackbar("Budget saved successfully!")
            viewModel.clearSavedFlag()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar("Error: $it") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = viewModel::showTemplateDialog) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Load Template")
                    }
                    IconButton(onClick = viewModel::showSaveTemplateDialog) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Save as Template")
                    }
                    IconButton(onClick = {
                        globalPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Add Receipt")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "View History")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.addExpenseRow() },
                icon = { Icon(Icons.Default.Add, "Add Expense") },
                text = { Text("Add Expense") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                BudgetHeaderCard(
                    state = state,
                    onSessionNameChange = viewModel::updateSessionName,
                    onInitialBudgetChange = viewModel::updateInitialBudget
                )
            }

            // OCR running indicator
            if (state.isOcrRunning) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "EXPENSES",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${state.expenses.size} item(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(
                items = state.expenses,
                key = { index, expense -> if (expense.id != 0L) expense.id else "new_$index" }
            ) { index, expense ->
                ExpenseRowItem(
                    expense = expense,
                    rowIndex = index,
                    onTitleChange = { viewModel.updateExpenseTitle(index, it) },
                    onAmountChange = { viewModel.updateExpenseAmount(index, it) },
                    onPaidToggle = { viewModel.togglePaid(index) },
                    onRemoveRow = { viewModel.removeExpenseRow(index) },
                    onToggleLock = { viewModel.toggleExpenseLock(index) },
                    onReceiptPicked = { uri -> viewModel.handleRowReceiptPicked(index, uri) },
                    onRemoveReceipt = { viewModel.removeReceiptFromExpense(index) },
                    onCategoryChange = { cat -> viewModel.updateExpenseCategory(index, cat) },
                    onToggleRecurring = { viewModel.toggleRecurring(index) },
                    onTriggerOcr = {
                        expense.receiptPath?.let { path ->
                            viewModel.triggerOcr(index, Uri.fromFile(File(path)))
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem(
                            fadeInSpec = tween(300),
                            placementSpec = spring()
                        )
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveSession() },
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Saving…")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Budget", fontWeight = FontWeight.SemiBold)
                    }
                }

                TextButton(
                    onClick = { viewModel.resetSession() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start New Budget")
                }
            }
        }

        if (state.pendingReceiptPath != null) {
            ReceiptAssignmentDialog(
                expenseTitles = state.expenses.mapIndexed { i, e ->
                    if (e.title.isNotBlank()) e.title else "Expense #${i + 1}"
                },
                onAssign = { index -> viewModel.assignPendingReceiptToExpense(index) },
                onDismiss = { viewModel.dismissReceiptAssignment() }
            )
        }

        if (state.showTemplateDialog) {
            TemplateLoadDialog(
                templates = templates,
                onLoad = { viewModel.loadTemplate(it) },
                onDismiss = viewModel::hideTemplateDialog
            )
        }

        if (state.showSaveTemplateDialog) {
            SaveTemplateDialog(
                onConfirm = { name -> viewModel.saveCurrentAsTemplate(name) },
                onDismiss = viewModel::hideSaveTemplateDialog
            )
        }
    }
}

// ── Budget header card ─────────────────────────────────────────────────────────

@Composable
private fun BudgetHeaderCard(
    state: BudgetUiState,
    onSessionNameChange: (String) -> Unit,
    onInitialBudgetChange: (String) -> Unit
) {
    val isOverBudget = state.remainingBalance < 0
    val budget = state.initialBudget.replace(",", "").toDoubleOrNull() ?: 0.0
    val totalExpenses = state.expenses.sumOf { it.amountAsDouble }

    val balanceColor by animateColorAsState(
        targetValue = when {
            isOverBudget -> MaterialTheme.colorScheme.error
            state.remainingBalance < budget * 0.1 -> Color(0xFFFF6F00)
            else -> Color(0xFF00B7B5)
        },
        animationSpec = tween(300),
        label = "balance_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = state.sessionName,
                onValueChange = onSessionNameChange,
                label = { Text("Budget Period Name") },
                placeholder = { Text("e.g. March 2026") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            )

            OutlinedTextField(
                value = state.initialBudget,
                onValueChange = onInitialBudgetChange,
                label = { Text("Initial Budget") },
                placeholder = { Text("0.00") },
                singleLine = true,
                prefix = { Text("₱", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Budget progress bar
            if (budget > 0) {
                BudgetProgressBar(spent = totalExpenses, total = budget)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

            val animatedBalance by animateFloatAsState(
                targetValue = state.remainingBalance.toFloat(),
                animationSpec = spring(stiffness = 200f),
                label = "balance_anim"
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isOverBudget) "OVER BUDGET" else "Remaining Balance",
                    style = MaterialTheme.typography.labelMedium,
                    color = balanceColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = CurrencyUtils.formatPhp(animatedBalance.toDouble()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = balanceColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Expenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = CurrencyUtils.formatPhp(totalExpenses),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ── Receipt assignment dialog ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptAssignmentDialog(
    expenseTitles: List<String>,
    onAssign: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedLabel by remember(expenseTitles) {
        mutableStateOf(expenseTitles.firstOrNull() ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
        title = { Text("Assign Receipt To") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select which expense this receipt belongs to:", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Expense") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        expenseTitles.forEachIndexed { index, title ->
                            DropdownMenuItem(
                                text = { Text(title) },
                                onClick = { selectedIndex = index; selectedLabel = title; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAssign(selectedIndex) }) { Text("Attach") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Template load dialog ───────────────────────────────────────────────────────

@Composable
private fun TemplateLoadDialog(
    templates: List<TemplateWithExpenses>,
    onLoad: (TemplateWithExpenses) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
        title = { Text("Load Template") },
        text = {
            if (templates.isEmpty()) {
                Text(
                    "No saved templates yet. Save your current budget as a template first.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { template ->
                        OutlinedCard(
                            onClick = { onLoad(template) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.template.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${template.expenses.size} expenses · ${CurrencyUtils.formatPhp(template.template.initialBudget)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ── Save as template dialog ────────────────────────────────────────────────────

@Composable
private fun SaveTemplateDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
        title = { Text("Save as Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Give your template a name to reuse these expenses later.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template Name") },
                    placeholder = { Text("e.g. Monthly Fixed Costs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
