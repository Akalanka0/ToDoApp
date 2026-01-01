package com.example.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val phoneNumber: String = "",
    val priority: Int = 1, // 0=Low, 1=Medium, 2=High
    val dueDate: Long? = null, // Timestamp in milliseconds, nullable
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val color: Int = 0xFF6200EE.toInt() // Custom color for visual distinction
)
