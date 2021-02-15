package io.getstream.chat.android.client.utils

import android.util.Log
import io.getstream.chat.android.core.internal.InternalStreamChatApi

@InternalStreamChatApi
public object PerformanceUtils {
    @Volatile
    private var tasks: Map<String, TaskEntry> = emptyMap()

    public fun startTask(taskName: String) {
        synchronized(taskName) {
            val startTime = System.currentTimeMillis()
            val lastEntry = tasks[taskName]
            tasks = if (lastEntry == null) {
                tasks + (taskName to TaskEntry(name = taskName, count = 0, sumDuration = 0.0, lastStart = startTime))
            } else {
                tasks + (taskName to lastEntry.copy(lastStart = startTime))
            }

            log("Task \"$taskName\" started")
        }
    }

    public fun stopTask(taskName: String) {
        synchronized(taskName) {
            val lastEntry = tasks[taskName]
            if (lastEntry == null) {
                log("There is no such task \"$taskName\"")
                return
            } else {
                doMeasureAndUpdateResult(taskName, lastEntry)
            }
        }
    }

    private fun doMeasureAndUpdateResult(taskName: String, entry: TaskEntry) {
        entry.lastStart?.let { startTime ->
            val lastDuration = (System.currentTimeMillis() - startTime) / 1000.0
            val newSumDuration = entry.sumDuration + lastDuration
            val newCount = entry.count + 1
            tasks = tasks + (taskName to entry.copy(count = newCount, sumDuration = newSumDuration, lastStart = null))
            val avgDuration = newSumDuration / newCount

            log(
                "Task \"$taskName\" completed for $lastDuration seconds\n" +
                    "The average duration for task \"$taskName\" is $avgDuration seconds"
            )
        }
    }

    public fun <T> task(taskName: String, task: () -> T): T {
        synchronized(taskName) {
            val count = tasks[taskName]?.count ?: 0
            val startTime = System.currentTimeMillis()
            return task().also {
                doMeasureAndUpdateResult(
                    taskName,
                    TaskEntry(name = taskName, lastStart = startTime, count = count)
                )
            }
        }
    }

    public suspend fun <T> suspendTask(taskName: String, task: suspend () -> T): T {
        val count = tasks[taskName]?.count ?: 0
        val startTime = System.currentTimeMillis()
        return task().also {
            doMeasureAndUpdateResult(
                taskName,
                TaskEntry(name = taskName, lastStart = startTime, count = count)
            )
        }
    }

    public fun log(message: String) {
        Log.d("Performance", message)
    }

    private data class TaskEntry(
        val name: String,
        val count: Int = 0,
        val sumDuration: Double = 0.0,
        val lastStart: Long?,
    )
}
