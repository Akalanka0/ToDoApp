package com.example.todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo.TaskStats
import com.example.todo.ui.theme.*

@Composable
fun StatisticsDialog(
    stats: TaskStats,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "📊 Task Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Total Tasks",
                    value = stats.total.toString(),
                    color = PrimaryPurple
                )
                
                StatCard(
                    label = "Completed",
                    value = stats.completed.toString(),
                    color = TaskCompleted
                )
                
                StatCard(
                    label = "Pending",
                    value = stats.pending.toString(),
                    color = TaskPending
                )
                
                if (stats.overdue > 0) {
                    StatCard(
                        label = "Overdue",
                        value = stats.overdue.toString(),
                        color = TaskOverdue
                    )
                }
                
                if (stats.highPriority > 0) {
                    StatCard(
                        label = "High Priority",
                        value = stats.highPriority.toString(),
                        color = PriorityHigh
                    )
                }

                // Completion Rate
                if (stats.total > 0) {
                    val completionRate = (stats.completed.toFloat() / stats.total * 100).toInt()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completion Rate: $completionRate%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    LinearProgressIndicator(
                        progress = completionRate / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = TaskCompleted,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun StatCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
