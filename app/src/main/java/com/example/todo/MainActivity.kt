package com.example.todo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todo.ui.theme.ToDoTheme

class MainActivity : ComponentActivity() {

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // permission result handled inside sendSMS function
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToDoApp(
                        requestSmsPermission = {
                            if (ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.SEND_SMS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ToDoApp(requestSmsPermission: () -> Unit, toDoViewModel: ToDoViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ToDo App",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = toDoViewModel.taskText,
            onValueChange = { toDoViewModel.taskText = it },
            label = { Text("Enter task") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = toDoViewModel.phoneText,
            onValueChange = { toDoViewModel.phoneText = it },
            label = { Text("Enter phone number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { toDoViewModel.addTask() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Task")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(toDoViewModel.taskList) { task ->
                TaskItem(
                    task = task,
                    onToggleDone = { toDoViewModel.toggleTaskDone(task) },
                    onDelete = { toDoViewModel.deleteTask(task) },
                    onSendSMS = {
                        requestSmsPermission()
                        sendSMS(task.phoneNumber, task.title)
                    }
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onSendSMS: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = if (task.done) Color(0xFFB2DFDB) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 18.sp,
                fontWeight = if (task.done) FontWeight.Normal else FontWeight.Medium,
                modifier = Modifier.clickable { onToggleDone() }
            )
            Text(
                text = "📱 ${task.phoneNumber}",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }

        Row {
            Text(
                text = "✉️",
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { onSendSMS() }
                    .padding(end = 12.dp)
            )

            Text(
                text = "🗑",
                fontSize = 18.sp,
                modifier = Modifier.clickable { onDelete() }
            )
        }
    }
}

// Function to send SMS
fun sendSMS(number: String, message: String) {
    try {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(number, null, message, null, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
