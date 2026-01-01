package com.example.todo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class], version = 3)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with all fields
                database.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        phoneNumber TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'Personal',
                        priority INTEGER NOT NULL DEFAULT 1,
                        dueDate INTEGER,
                        done INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        color INTEGER NOT NULL DEFAULT -10011977
                    )
                """.trimIndent())

                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO tasks_new (id, title, phoneNumber, done, createdAt)
                    SELECT id, title, phoneNumber, done, ${System.currentTimeMillis()}
                    FROM tasks
                """.trimIndent())

                // Remove old table
                database.execSQL("DROP TABLE tasks")

                // Rename new table to tasks
                database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table without category
                database.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        phoneNumber TEXT NOT NULL DEFAULT '',
                        priority INTEGER NOT NULL DEFAULT 1,
                        dueDate INTEGER,
                        done INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER,
                        color INTEGER NOT NULL DEFAULT -10011977
                    )
                """.trimIndent())

                // Copy data from old table to new table (excluding category)
                database.execSQL("""
                    INSERT INTO tasks_new (id, title, description, phoneNumber, priority, dueDate, done, createdAt, completedAt, color)
                    SELECT id, title, description, phoneNumber, priority, dueDate, done, createdAt, completedAt, color
                    FROM tasks
                """.trimIndent())

                // Remove old table
                database.execSQL("DROP TABLE tasks")

                // Rename new table to tasks
                database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
