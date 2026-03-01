package com.cedd.budgettracker.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cedd.budgettracker.domain.model.ExpenseCategory
import com.cedd.budgettracker.domain.model.ExpenseUiModel
import com.cedd.budgettracker.presentation.utils.CurrencyUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseRowItem(
    expense: ExpenseUiModel,
    rowIndex: Int,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPaidToggle: () -> Unit,
    onRemoveRow: () -> Unit,
    onToggleLock: () -> Unit,
    onReceiptPicked: (Uri) -> Unit,
    onRemoveReceipt: () -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onToggleRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onReceiptPicked(it) } }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemoveRow()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        ExpenseCard(
            expense = expense,
            rowIndex = rowIndex,
            onTitleChange = onTitleChange,
            onAmountChange = onAmountChange,
            onPaidToggle = onPaidToggle,
            onRemoveRow = onRemoveRow,
            onToggleLock = onToggleLock,
            onAttachReceipt = {
                rowPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemoveReceipt = onRemoveReceipt,
            onCategoryChange = onCategoryChange,
            onToggleRecurring = onToggleRecurring
        )
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseUiModel,
    rowIndex: Int,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPaidToggle: () -> Unit,
    onRemoveRow: () -> Unit,
    onToggleLock: () -> Unit,
    onAttachReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onToggleRecurring: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = when {
            expense.isPaid && expense.isLocked -> Color(0xFFE3F2FD)
            expense.isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(350),
        label = "card_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (expense.isLocked)
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        animationSpec = tween(350),
        label = "card_border"
    )

    val elevation by animateDpAsState(
        targetValue = if (expense.isLocked) 0.dp else 3.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        if (expense.isLocked) {
            LockedRow(
                expense = expense,
                onPaidToggle = onPaidToggle,
                onEdit = onToggleLock,
                onDelete = onRemoveRow
            )
        } else {
            EditableRow(
                expense = expense,
                rowIndex = rowIndex,
                onTitleChange = onTitleChange,
                onAmountChange = onAmountChange,
                onPaidToggle = onPaidToggle,
                onRemoveRow = onRemoveRow,
                onLock = onToggleLock,
                onAttachReceipt = onAttachReceipt,
                onRemoveReceipt = onRemoveReceipt,
                onCategoryChange = onCategoryChange,
                onToggleRecurring = onToggleRecurring
            )
        }
    }
}

// ── Full-screen image preview dialog ──────────────────────────────────────────

@Composable
private fun FullScreenImageDialog(path: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(path)).crossfade(true).build(),
                contentDescription = "Receipt full screen",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Locked (read-only summary) row ────────────────────────────────────────────

@Composable
private fun LockedRow(
    expense: ExpenseUiModel,
    onPaidToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showFullScreen by remember { mutableStateOf(false) }

    if (showFullScreen && expense.receiptPath != null) {
        FullScreenImageDialog(path = expense.receiptPath, onDismiss = { showFullScreen = false })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(
            checked = expense.isPaid,
            onCheckedChange = { onPaidToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF00BFA5),
                uncheckedColor = Color(0xFF90A4AE)
            ),
            modifier = Modifier.size(36.dp)
        )

        // Category emoji badge
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(expense.category.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(expense.category.emoji, fontSize = 12.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = expense.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (expense.isPaid) TextDecoration.LineThrough else null,
                    color = if (expense.isPaid)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                if (expense.isRecurring) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Recurring",
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
            if (expense.receiptPath != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Receipt attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Inline receipt thumbnail — tap to preview full screen
        expense.receiptPath?.let { path ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(path)).crossfade(true).build(),
                contentDescription = "Receipt",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showFullScreen = true }
            )
        }

        // Amount pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (expense.isPaid)
                Color(0xFF00BFA5).copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = CurrencyUtils.formatPhp(expense.amountAsDouble),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (expense.isPaid)
                    Color(0xFF00BFA5).copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onPrimaryContainer,
                textDecoration = if (expense.isPaid) TextDecoration.LineThrough else null
            )
        }

        // Edit
        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit expense",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }

        // Delete
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

// ── Editable (active input) row ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableRow(
    expense: ExpenseUiModel,
    rowIndex: Int,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPaidToggle: () -> Unit,
    onRemoveRow: () -> Unit,
    onLock: () -> Unit,
    onAttachReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onToggleRecurring: () -> Unit
) {
    var showFullScreen by remember { mutableStateOf(false) }

    if (showFullScreen && expense.receiptPath != null) {
        FullScreenImageDialog(path = expense.receiptPath, onDismiss = { showFullScreen = false })
    }

    Column(modifier = Modifier.padding(12.dp)) {

        // Header: label + done + delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Expense #${rowIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onLock,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("Done", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onRemoveRow, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Title field
        OutlinedTextField(
            value = expense.title,
            onValueChange = onTitleChange,
            placeholder = { Text("e.g. Rent, Groceries…") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Category picker
        var categoryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = "${expense.category.emoji}  ${expense.category.label}",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                ExpenseCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(cat.emoji)
                                Text(cat.label)
                            }
                        },
                        onClick = {
                            onCategoryChange(cat)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Amount + Paid checkbox + Camera
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expense.amount,
                onValueChange = onAmountChange,
                placeholder = { Text("0.00") },
                singleLine = true,
                prefix = { Text("₱", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Paid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Checkbox(
                    checked = expense.isPaid,
                    onCheckedChange = { onPaidToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00BFA5), uncheckedColor = Color(0xFF90A4AE))
                )
            }

            // Camera / receipt button
            IconButton(
                onClick = onAttachReceipt,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (expense.receiptPath != null)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "Attach receipt",
                    tint = if (expense.receiptPath != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Recurring toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = expense.isRecurring,
                onClick = onToggleRecurring,
                label = { Text("Recurring", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        if (expense.receiptPath != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReceiptThumbnail(
                    receiptPath = expense.receiptPath,
                    onRemove = onRemoveReceipt,
                    onClick = { showFullScreen = true }
                )
                Text(
                    "Receipt attached",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
