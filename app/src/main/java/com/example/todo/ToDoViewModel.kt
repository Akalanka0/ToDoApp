package com.example.todo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todo.data.TaskDatabase
import com.example.todo.data.TaskEntity
import com.example.todo.data.TaskPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ToDoViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao = TaskDatabase.getDatabase(application).taskDao()

    // Input fields
    var taskText by mutableStateOf("")
    var descriptionText by mutableStateOf("")
    var phoneText by mutableStateOf("")
    var selectedPriority by mutableStateOf(TaskPriority.MEDIUM)
    var selectedDueDate by mutableStateOf<Long?>(null)
    
    // Theme state: false = light, true = dark
    var isDarkMode by mutableStateOf(false)
    
    // Editing state
    var editingTask by mutableStateOf<TaskEntity?>(null)

    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQueryState: StateFlow<String> = _searchQuery
    var searchQuery by mutableStateOf("")
        private set

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        _searchQuery.value = query
    }

    private val _filterPriority = MutableStateFlow<TaskPriority?>(null)
    val filterPriority: StateFlow<TaskPriority?> = _filterPriority

    private val _showCompletedTasks = MutableStateFlow(true)
    val showCompletedTasks: StateFlow<Boolean> = _showCompletedTasks

    // All tasks from database
    private val allTasks = taskDao.getAllTasks()

    // Filtered and sorted task list
    val taskList: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _searchQuery,
        _filterPriority,
        _showCompletedTasks
    ) { tasks, query, priority, showCompleted ->
        tasks.filter { task ->
            val matchesSearch = task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
            val matchesPriority = priority == null || task.priority == priority.value
            val matchesCompleted = showCompleted || !task.done

            matchesSearch && matchesPriority && matchesCompleted
        }.sortedWith(
            compareByDescending<TaskEntity> { !it.done } // Incomplete tasks first
                .thenByDescending { TaskPriority.fromValue(it.priority).value } // High priority first
                .thenBy { it.dueDate ?: Long.MAX_VALUE } // Earliest due date first
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Statistics
    val taskStats: StateFlow<TaskStats> = allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    ).let { tasks ->
        MutableStateFlow(TaskStats()).apply {
            viewModelScope.launch {
                tasks.collect { taskList ->
                    value = TaskStats(
                        total = taskList.size,
                        completed = taskList.count { it.done },
                        pending = taskList.count { !it.done },
                        overdue = taskList.count { task ->
                            !task.done && task.dueDate != null && task.dueDate < System.currentTimeMillis()
                        },
                        highPriority = taskList.count { it.priority == TaskPriority.HIGH.value && !it.done }
                    )
                }
            }
        }
    }

    fun addTask() {
        if (taskText.isNotBlank()) {
            viewModelScope.launch {
                val newTask = editingTask?.copy(
                    title = taskText,
                    description = descriptionText,
                    phoneNumber = phoneText,
                    priority = selectedPriority.value,
                    dueDate = selectedDueDate
                ) ?: TaskEntity(
                    title = taskText,
                    description = descriptionText,
                    phoneNumber = phoneText,
                    priority = selectedPriority.value,
                    dueDate = selectedDueDate,
                    createdAt = System.currentTimeMillis()
                )
                
                if (editingTask != null) {
                    taskDao.updateTask(newTask)
                } else {
                    taskDao.insertTask(newTask)
                }
                clearInputs()
            }
        }
    }

    fun startEditing(task: TaskEntity) {
        editingTask = task
        taskText = task.title
        descriptionText = task.description
        phoneText = task.phoneNumber
        selectedPriority = TaskPriority.fromValue(task.priority)
        selectedDueDate = task.dueDate
    }

    fun cancelEditing() {
        clearInputs()
    }

    fun toggleTheme() {
        isDarkMode = !isDarkMode
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }

    fun toggleTaskDone(task: TaskEntity) {
        viewModelScope.launch {
            val completedAt = if (!task.done) System.currentTimeMillis() else null
            taskDao.updateTask(task.copy(done = !task.done, completedAt = completedAt))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    fun setFilterPriority(priority: TaskPriority?) {
        _filterPriority.value = if (_filterPriority.value == priority) null else priority
    }

    fun setShowCompletedTasks(show: Boolean) {
        _showCompletedTasks.value = show
    }

    private fun clearInputs() {
        onSearchQueryChange("")
        taskText = ""
        descriptionText = ""
        phoneText = ""
        selectedPriority = TaskPriority.MEDIUM
        selectedDueDate = null
        editingTask = null
    }
}

data class TaskStats(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    val highPriority: Int = 0
)
