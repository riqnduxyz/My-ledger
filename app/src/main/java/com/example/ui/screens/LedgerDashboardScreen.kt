package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseCategory
import com.example.data.model.LedgerNote
import com.example.data.repository.LedgerEntryWithCategory
import com.example.ui.viewmodel.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LedgerDashboardScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State Collectors
    val filteredEntries by viewModel.filteredEntries.collectAsStateWithLifecycle()
    val allCategories by viewModel.categories.collectAsStateWithLifecycle()
    val stats by viewModel.globalStats.collectAsStateWithLifecycle()
    val syncCode by viewModel.syncCode.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()

    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()

    // Dialog state controllers
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<LedgerNote?>(null) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<LedgerNote?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "App Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Ledger Notes",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    // Sync Status Indicator & Shortcut Button
                    IconButton(
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.testTag("sync_shortcut_button")
                    ) {
                        val iconColor = if (syncCode != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(
                            imageVector = if (syncCode != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = "Sync Center",
                            tint = iconColor
                        )
                    }
                    IconButton(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier.testTag("manage_categories_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Manage Categories"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedNoteForEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("add_transaction_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // HERO STATS BANNER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NET LEDGER BALANCE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatCurrency(stats.netBalance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (stats.netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350),
                        modifier = Modifier.testTag("net_balance_text")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Income stat card
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Income",
                                    tint = Color(0xFF66BB6A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatCurrency(stats.totalIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF66BB6A),
                                modifier = Modifier.testTag("total_income_text")
                            )
                        }

                        // Divider line
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                .align(Alignment.CenterVertically)
                        )

                        // Expense stat card
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = "Expenses",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatCurrency(stats.totalExpenses),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350),
                                modifier = Modifier.testTag("total_expenses_text")
                            )
                        }
                    }
                }
            }

            // FILTER CONTROLS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Row 1: Type selection chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        label = { Text("All Ledger") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("filter_all_type")
                    )
                    FilterChip(
                        selected = selectedTypeFilter == "INCOME",
                        onClick = { viewModel.setTypeFilter("INCOME") },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.testTag("filter_income_type")
                    )
                    FilterChip(
                        selected = selectedTypeFilter == "EXPENSE",
                        onClick = { viewModel.setTypeFilter("EXPENSE") },
                        label = { Text("Expenses") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF5350).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFEF5350)
                        ),
                        modifier = Modifier.testTag("filter_expense_type")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: Categories horizontal filter
                Text(
                    text = "FILTER BY CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("All Categories") },
                            modifier = Modifier.testTag("filter_category_all")
                        )
                    }
                    items(allCategories) { category ->
                        val isSelected = selectedCategoryFilter == category.id
                        val pillColor = remember(category.colorHex) { parseHexColor(category.colorHex) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCategoryFilter(category.id) },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = category.name,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.White else pillColor
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pillColor,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_category_${category.id}")
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // LIST OF TRANSACTIONS / EMPTY STATE
            Box(modifier = Modifier.weight(1f)) {
                if (filteredEntries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty ledger",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No ledger logs found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log your first expense or income by clicking the + button below, or sync data in the cloud center.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredEntries, key = { it.note.id }) { entry ->
                            TransactionItemRow(
                                entry = entry,
                                onEditClick = {
                                    selectedNoteForEdit = entry.note
                                    showAddEditDialog = true
                                },
                                onDeleteClick = {
                                    showDeleteConfirmDialog = entry.note
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG: ADD/EDIT TRANSACTION
    if (showAddEditDialog) {
        AddEditTransactionDialog(
            note = selectedNoteForEdit,
            categories = allCategories,
            onDismiss = { showAddEditDialog = false },
            onSave = { title, amount, type, catId, date, desc ->
                if (selectedNoteForEdit == null) {
                    viewModel.addTransaction(title, amount, type, catId, date, desc)
                    Toast.makeText(context, "Entry added", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.editTransaction(selectedNoteForEdit!!.id, title, amount, type, catId, date, desc)
                    Toast.makeText(context, "Entry updated", Toast.LENGTH_SHORT).show()
                }
                showAddEditDialog = false
            }
        )
    }

    // DIALOG: CATEGORY MANAGER
    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = allCategories,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = { name, colorHex, iconName ->
                viewModel.addCategory(name, colorHex, iconName)
                Toast.makeText(context, "Category added", Toast.LENGTH_SHORT).show()
            },
            onDeleteCategory = { category ->
                viewModel.deleteCategory(category)
                Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // DIALOG: SYNC CENTER (CLOUD SYNCING)
    if (showSyncDialog) {
        SyncCenterDialog(
            syncCode = syncCode,
            lastSyncTime = lastSyncTime,
            isSyncing = isSyncing,
            syncError = syncError,
            onDismiss = { showSyncDialog = false },
            onGenerateCode = {
                viewModel.setupNewSyncCode { success ->
                    if (success) {
                        Toast.makeText(context, "Sync bucket created & uploaded!", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onLinkCode = { code ->
                viewModel.linkAndPullSyncCode(code) { success ->
                    if (success) {
                        Toast.makeText(context, "Connected & Ledger Pulled Successfully!", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onPush = {
                viewModel.pushToCloud { success ->
                    if (success) {
                        Toast.makeText(context, "Ledger backed up successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onPull = {
                viewModel.pullFromCloud { success ->
                    if (success) {
                        Toast.makeText(context, "Ledger restored from backup!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDisconnect = {
                viewModel.disconnectSync()
                Toast.makeText(context, "Cloud sync disconnected", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // DIALOG: DELETE TRANSACTION CONFIRMATION
    showDeleteConfirmDialog?.let { noteToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete \"${noteToDelete.title}\" from your ledger notes? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(noteToDelete)
                        Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionItemRow(
    entry: LedgerEntryWithCategory,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = entry.category
    val note = entry.note
    val categoryColor = remember(category?.colorHex) {
        if (category != null) parseHexColor(category.colorHex) else Color.Gray
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEditClick() }
            .testTag("ledger_item_${note.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left block: Category Icon with custom background circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(category?.iconName ?: "MoreHoriz"),
                    contentDescription = category?.name ?: "Other",
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Center block: Title, Date, Sub-Category Pill, Notes description
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Category pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = category?.name ?: "Other",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Date
                Text(
                    text = formatDate(note.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                if (note.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right block: Price (Income/Expense style) & Delete button shortcut
            Column(horizontalAlignment = Alignment.End) {
                val isIncome = note.type == "INCOME"
                val textPrefix = if (isIncome) "+" else "-"
                val textColor = if (isIncome) Color(0xFF4CAF50) else Color(0xFFEF5350)
                Text(
                    text = "$textPrefix${formatCurrency(note.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit entry",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onEditClick() }
                            .padding(2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDeleteClick() }
                            .padding(2.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// DIALOG COMPOSABLE: ADD/EDIT
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    note: LedgerNote?,
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Int, Long, String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var amountStr by remember { mutableStateOf(note?.amount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var type by remember { mutableStateOf(note?.type ?: "EXPENSE") }
    var categoryId by remember { mutableStateOf(note?.categoryId ?: categories.firstOrNull()?.id ?: 0) }
    var description by remember { mutableStateOf(note?.description ?: "") }

    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (note == null) "Log Transaction" else "Edit Ledger Note",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle Button for Expense / Income
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = type == "EXPENSE",
                                onClick = { type = "EXPENSE" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, "Expense", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expense")
                                }
                            }
                            SegmentedButton(
                                selected = type == "INCOME",
                                onClick = { type = "INCOME" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, "Income", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Income")
                                }
                            }
                        }
                    }
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = it.trim().isEmpty()
                        },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. Weekly Groceries") },
                        isError = titleError,
                        supportingText = { if (titleError) Text("Title is required") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_title_input")
                    )
                }

                // Amount Input
                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            val clean = it.trim()
                            amountError = clean.isEmpty() || clean.toDoubleOrNull() == null || clean.toDouble() <= 0
                        },
                        label = { Text("Amount ($)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountError,
                        supportingText = { if (amountError) Text("Enter a valid amount (> 0)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_amount_input")
                    )
                }

                // Category dropdown selector
                item {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (categories.isEmpty()) {
                        Text("Please create a category first", color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { category ->
                                val selected = categoryId == category.id
                                val color = remember(category.colorHex) { parseHexColor(category.colorHex) }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selected) color else color.copy(alpha = 0.1f)
                                        )
                                        .clickable { categoryId = category.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("dialog_category_chip_${category.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getCategoryIcon(category.iconName),
                                            contentDescription = category.name,
                                            tint = if (selected) Color.White else color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes / Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Additional notes (Optional)") },
                        placeholder = { Text("e.g. Bought items at Whole Foods store") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_notes_input"),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim()
                    val finalAmount = amountStr.trim().toDoubleOrNull()
                    titleError = finalTitle.isEmpty()
                    amountError = finalAmount == null || finalAmount <= 0

                    if (!titleError && !amountError && categoryId != 0) {
                        onSave(finalTitle, finalAmount!!, type, categoryId, note?.date ?: System.currentTimeMillis(), description.trim())
                    }
                },
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// DIALOG COMPOSABLE: CATEGORIES LIST & CREATION
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerDialog(
    categories: List<ExpenseCategory>,
    onDismiss: () -> Unit,
    onAddCategory: (String, String, String) -> Unit,
    onDeleteCategory: (ExpenseCategory) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#FF7043") } // Default: orange
    var selectedIconName by remember { mutableStateOf("Restaurant") }

    val colorsList = listOf(
        "#FF7043", // Orange
        "#EC407A", // Pink
        "#42A5F5", // Blue
        "#FFCA28", // Amber
        "#AB47BC", // Purple
        "#66BB6A", // Green
        "#26A69A", // Teal
        "#78909C"  // Grey
    )

    val iconsList = listOf(
        "Restaurant" to Icons.Default.Restaurant,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "Lightbulb" to Icons.Default.Lightbulb,
        "Movie" to Icons.Default.Movie,
        "Payments" to Icons.Default.Payments,
        "MoreHoriz" to Icons.Default.MoreHoriz
    )

    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Section 1: Add new Category Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CREATE NEW CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = {
                                newCategoryName = it
                                nameError = it.trim().isEmpty()
                            },
                            label = { Text("Category Name") },
                            isError = nameError,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("category_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Color selection dots
                        Text("Select Color:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorsList.forEach { hex ->
                                val col = remember(hex) { parseHexColor(hex) }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .clickable { selectedColorHex = hex }
                                        .border(
                                            width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Icon selection
                        Text("Select Icon:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(iconsList) { (name, icon) ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selectedIconName == name) parseHexColor(selectedColorHex) else Color.Transparent
                                        )
                                        .clickable { selectedIconName = name }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = name,
                                        tint = if (selectedIconName == name) Color.White else parseHexColor(selectedColorHex)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newCategoryName.trim().isEmpty()) {
                                    nameError = true
                                } else {
                                    onAddCategory(newCategoryName.trim(), selectedColorHex, selectedIconName)
                                    newCategoryName = ""
                                    nameError = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_category_button")
                        ) {
                            Icon(Icons.Default.Check, "Save")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Category")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Active Categories Scroll list
                Text(
                    text = "EXISTING CATEGORIES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories) { category ->
                        val col = remember(category.colorHex) { parseHexColor(category.colorHex) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = category.name,
                                tint = col,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            // Protect basic categories or let users delete anything
                            IconButton(
                                onClick = { onDeleteCategory(category) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("delete_category_button_${category.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete category",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_categories_dialog")) {
                Text("Close")
            }
        }
    )
}

// DIALOG COMPOSABLE: CLOUD SYNC CENTER
@Composable
fun SyncCenterDialog(
    syncCode: String?,
    lastSyncTime: Long,
    isSyncing: Boolean,
    syncError: String?,
    onDismiss: () -> Unit,
    onGenerateCode: () -> Unit,
    onLinkCode: (String) -> Unit,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var isLinking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Cloud Sync Center",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Cloud Backup & Sync", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sync status indicator
                if (isSyncing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Connecting and synchronizing...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (syncError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (syncCode != null) {
                    // CONNECTED STATE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "CLOUD SYNC ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Your unique device Sync Code:")
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = syncCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("active_sync_code")
                                )

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("ledger_sync_code", syncCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Sync Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (lastSyncTime > 0) {
                                    "Last synced: ${formatDateTime(lastSyncTime)}"
                                } else {
                                    "Last synced: Never"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Backup and restore controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPush,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_backup_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, "Push", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Now")
                        }
                        Button(
                            onClick = onPull,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_restore_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, "Pull", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Now")
                        }
                    }

                    OutlinedButton(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_disconnect_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, "Disconnect", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect & Remove Sync")
                    }
                } else {
                    // DISCONNECTED STATE
                    Text(
                        text = "Synchronize your ledger logs securely across other Android phones or devices. Simply generate a code here, or enter a code from your other device below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onGenerateCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_sync_code_button")
                    ) {
                        Icon(Icons.Default.CloudQueue, "Cloud", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Cloud Syncing")
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "LINK TO ANOTHER DEVICE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it.trim() },
                        label = { Text("Enter Device Sync Code") },
                        placeholder = { Text("e.g. bucket_ledger_xxxx") },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_code_input")
                    )

                    Button(
                        onClick = {
                            if (inputCode.trim().isNotEmpty()) {
                                onLinkCode(inputCode.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        enabled = inputCode.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connect_sync_code_button")
                    ) {
                        Icon(Icons.Default.Link, "Link", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Link & Pull Ledger Data")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_sync_dialog")) {
                Text("Close")
            }
        }
    )
}

// FORMATTING & AUXILIARY HELPERS

fun formatCurrency(amount: Double): String {
    return String.format("$%,.2f", amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun parseHexColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun getCategoryIcon(name: String): ImageVector {
    return when (name) {
        "Restaurant" -> Icons.Default.Restaurant
        "ShoppingBag" -> Icons.Default.ShoppingBag
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "Lightbulb" -> Icons.Default.Lightbulb
        "Movie" -> Icons.Default.Movie
        "Payments" -> Icons.Default.Payments
        else -> Icons.Default.MoreHoriz
    }
}
