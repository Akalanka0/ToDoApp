package com.example.todo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class Task(
    val title: String,
    val phoneNumber: String,
    var done: Boolean = false
)

class ToDoViewModel : ViewModel() {
    var taskText by mutableStateOf("")
    var phoneText by mutableStateOf("")
    var taskList by mutableStateOf(listOf<Task>())

    fun addTask() {
        if (taskText.isNotBlank() && phoneText.isNotBlank()) {
            taskList = taskList + Task(taskText, phoneText)
            taskText = ""
            phoneText = ""
        }
    }

    fun toggleTaskDone(task: Task) {
        task.done = !task.done
        taskList = taskList.toList() // trigger recomposition
    }

    fun deleteTask(task: Task) {
        taskList = taskList - task
    }
}
