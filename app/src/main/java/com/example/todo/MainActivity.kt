package com.example.todo

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todo.data.TaskEntity
import com.example.todo.data.TaskPriority
import com.example.todo.ui.components.AddTaskBottomSheet
import com.example.todo.ui.components.StatisticsDialog
import com.example.todo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val toDoViewModel: ToDoViewModel = viewModel(factory = ToDoViewModelFactory(applicationContext as Application))
            val isDarkMode = toDoViewModel.isDarkMode
            
            ToDoTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToDoApp(
                        viewModel = toDoViewModel,
                        requestSmsPermission = {
                            val permissions = mutableListOf(Manifest.permission.SEND_SMS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                permissions.add(Manifest.permission.READ_PHONE_STATE)
                            }
                            
                            val missingPermissions = permissions.filter {
                                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                            }
                            
                            if (missingPermissions.isNotEmpty()) {
                                smsPermissionLauncher.launch(missingPermissions.first()) // Simplified for now
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoApp(
    viewModel: ToDoViewModel,
    requestSmsPermission: () -> Unit
) {
    val tasks by viewModel.taskList.collectAsState()
    val stats by viewModel.taskStats.collectAsState()
    val context = LocalContext.current
    
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "My Tasks",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${stats.pending} pending • ${stats.completed} completed",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (viewModel.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(Icons.Default.Info, "Statistics")
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
                onClick = { showAddTaskSheet = true },
                icon = { Icon(Icons.Default.Add, "Add Task") },
                text = { Text("New Task") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = viewModel.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Filter Chips
            FilterChipsRow(viewModel = viewModel)

            // Task List
            if (tasks.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleDone = { viewModel.toggleTaskDone(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onEdit = {
                                viewModel.startEditing(task)
                                showAddTaskSheet = true
                            },
                            onSendSMS = {
                                if (task.phoneNumber.isNotBlank()) {
                                    requestSmsPermission()
                                    sendSMS(context, task.phoneNumber, task.title)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Task Bottom Sheet
    if (showAddTaskSheet) {
        AddTaskBottomSheet(
            viewModel = viewModel,
            onDismiss = {
                viewModel.cancelEditing()
                showAddTaskSheet = false
            },
            onSave = {
                viewModel.addTask()
                showAddTaskSheet = false
            }
        )
    }

    // Statistics Dialog
    if (showStatsDialog) {
        StatisticsDialog(
            stats = stats,
            onDismiss = { showStatsDialog = false }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search tasks...") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipsRow(viewModel: ToDoViewModel) {
    val selectedPriority by viewModel.filterPriority.collectAsState()
    
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Priority filters
        TaskPriority.entries.forEach { priority ->
            FilterChip(
                selected = selectedPriority == priority,
                onClick = { viewModel.setFilterPriority(priority) },
                label = { Text(priority.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(priority.color).copy(alpha = 0.2f),
                    selectedLabelColor = Color(priority.color)
                )
            )
        }
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSendSMS: () -> Unit
) {
    val priority = TaskPriority.fromValue(task.priority)
    val isOverdue = task.dueDate != null && task.dueDate < System.currentTimeMillis() && !task.done

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.done) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator
            Box(
                modifier = Modifier
                    .size(4.dp, 60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(priority.color))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggleDone() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TaskCompleted
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Task Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Due Date
                task.dueDate?.let { dueDate ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isOverdue) TaskOverdue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(dueDate),
                            fontSize = 12.sp,
                            color = if (isOverdue) TaskOverdue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Phone Number
                if (task.phoneNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📱 ${task.phoneNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.End) {
                Row {
                    if (task.phoneNumber.isNotBlank()) {
                        IconButton(onClick = onSendSMS) {
                            Icon(
                                Icons.Default.Send,
                                "Send SMS",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📝",
            fontSize = 72.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tasks yet",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to create your first task",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun sendSMS(context: Context, number: String, message: String) {
    try {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            
            if (activeSubscriptions != null && activeSubscriptions.size > 1) {
                // More than one SIM, ideally show a dialog. For now, we'll log it or pick the first one.
                // In a real app, you'd show a dialog here.
                Toast.makeText(context, "Multiple SIMs detected. Using default.", Toast.LENGTH_SHORT).show()
            }
        }

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        
        smsManager.sendTextMessage(number, null, message, null, null)
        Toast.makeText(context, "SMS sent successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
