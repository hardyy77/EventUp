package com.example.eventup.utils

import android.content.Context
import androidx.work.*
import com.example.eventup.workers.UpdateInterestingEventsWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TaskScheduler {

    fun scheduleDailyTask(context: Context) {
        val updateRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "UpdateInterestingEventsWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance()
        dueTime.set(Calendar.HOUR_OF_DAY, 7)
        dueTime.set(Calendar.MINUTE, 0)
        dueTime.set(Calendar.SECOND, 0)
        if (dueTime.before(currentTime)) {
            dueTime.add(Calendar.HOUR_OF_DAY, 24)
        }
        return dueTime.timeInMillis - currentTime.timeInMillis
    }

    fun scheduleDailyInterestingEventsUpdate(context: Context) {
        val updateRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "UpdateInterestingEvents",
            ExistingPeriodicWorkPolicy.REPLACE,
            updateRequest
        )
    }
}
