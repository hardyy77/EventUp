package com.example.eventup

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TaskScheduler {

    fun scheduleDailyTask(context: Context) {
        val updateRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(updateRequest)
    }

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        return targetTime.timeInMillis - now.timeInMillis
    }
}
