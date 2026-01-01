package com.example.todo.data

// Task priorities
enum class TaskPriority(val displayName: String, val value: Int, val color: Long) {
    LOW("Low", 0, 0xFF4CAF50),
    MEDIUM("Medium", 1, 0xFFFF9800),
    HIGH("High", 2, 0xFFF44336);

    companion object {
        fun fromValue(value: Int): TaskPriority {
            return entries.find { it.value == value } ?: MEDIUM
        }
    }
}
