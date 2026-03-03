package com.cedd.budgettracker.presentation.components

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cedd.budgettracker.domain.model.ExpenseCategory
import com.cedd.budgettracker.domain.model.ExpenseUiModel
import com.cedd.budgettracker.presentation.utils.CurrencyUtils
import com.cedd.budgettracker.presentation.utils.ThousandSeparatorTransformation
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseRowItem(
    expense: ExpenseUiModel,
    rowIndex: Int,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
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
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemoveRow()
            // Reset in case removal was rejected (e.g. last remaining row)
            dismissState.reset()
        }
    }

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
            onNotesChange = onNotesChange,
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
    onNotesChange: (String) -> Unit,
    onPaidToggle: () -> Unit,
    onRemoveRow: () -> Unit,
    onToggleLock: () -> Unit,
    onAttachReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit,
    onCategoryChange: (ExpenseCategory) -> Unit,
    onToggleRecurring: () -> Unit
) {
    val catColor by animateColorAsState(
        targetValue = if (expense.isPaid)
            expense.category.color.copy(alpha = 0.3f)
        else
            expense.category.color.copy(alpha = 0.75f),
        animationSpec = tween(350),
        label = "cat_strip_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xCC1B3D5A),  // Dark teal-blue — prosperity, frosted glass
                        Color(0xAA0C2236),  // Deep navy
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x7038BDF8),  // Electric blue top edge — glass highlight
                        Color(0x2038BDF8),  // Faded at bottom
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Colored left accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        catColor,
                        RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                    )
            )
            Box(modifier = Modifier.weight(1f)) {
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
                        onNotesChange = onNotesChange,
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
    }
}

// ── Full-screen image preview dialog ──────────────────────────────────────────

@Composable
internal fun FullScreenImageDialog(path: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image — tap anywhere to close
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(path)).crossfade(true).build(),
                contentDescription = "Receipt full screen",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
            )

            // Top bar: close on left, title center, download on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Text("Receipt", color = Color.White, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val saved = saveReceiptToGallery(context, path)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                if (saved) "Saved to gallery" else "Failed to save image",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Save to gallery", tint = Color.White)
                }
            }
        }
    }
}

private fun saveReceiptToGallery(context: Context, imagePath: String): Boolean {
    return try {
        val source   = File(imagePath)
        val fileName = "CeddFlow_receipt_${System.currentTimeMillis()}.jpg"
        val values   = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CeddFlow")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            source.inputStream().use { it.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (e: Exception) {
        false
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
                    letterSpacing = 0.2.sp,
                    textDecoration = if (expense.isPaid) TextDecoration.LineThrough else null,
                    color = if (expense.isPaid)
                        Color(0xFFBAE6FD).copy(alpha = 0.35f)
                    else
                        Color(0xFFE2F4FF)  // Crisp blue-white — pairs with teal card
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
            if (expense.notes.isNotBlank()) {
                Text(
                    text = expense.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
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
                Color(0xFF1E4060)  // Deep teal — harmonises with card background
        ) {
            Text(
                text = CurrencyUtils.formatPhp(expense.amountAsDouble),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = if (expense.isPaid)
                    Color(0xFF00BFA5).copy(alpha = 0.5f)
                else
                    Color(0xFFBAE6FD),  // Light sky-blue — readable on teal pill
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
    onNotesChange: (String) -> Unit,
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

        // Notes field
        OutlinedTextField(
            value = expense.notes,
            onValueChange = onNotesChange,
            placeholder = { Text("Add a note… (optional)") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
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
                onValueChange = { onAmountChange(CurrencyUtils.cleanAmountInput(it)) },
                placeholder = { Text("0.00") },
                singleLine = true,
                prefix = { Text("₱", fontWeight = FontWeight.Bold) },
                visualTransformation = ThousandSeparatorTransformation,
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
