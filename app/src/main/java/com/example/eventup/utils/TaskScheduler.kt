package com.example.eventup.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.eventup.workers.UpdateInterestingEventsWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object TaskScheduler {

    // Funkcja planująca codzienne zadanie aktualizacji interesujących wydarzeń
    fun scheduleDailyTask(context: Context) {
        try {
            val updateRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "UpdateInterestingEventsWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                updateRequest
            )
            Log.d("TaskScheduler", "Scheduled daily task for UpdateInterestingEventsWorker")
        } catch (e: Exception) {
            Log.e("TaskScheduler", "Error scheduling daily task: ${e.message}", e)
        }
    }

    // Funkcja obliczająca początkowe opóźnienie dla zadania
    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val dueTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(currentTime)) {
                add(Calendar.HOUR_OF_DAY, 24)
            }
        }
        val initialDelay = dueTime.timeInMillis - currentTime.timeInMillis
        Log.d("TaskScheduler", "Initial delay calculated: $initialDelay milliseconds")
        return initialDelay
    }

    // Funkcja planująca codzienną aktualizację interesujących wydarzeń (zakomentowana)
//    fun scheduleDailyInterestingEventsUpdate(context: Context) {
//        val updateRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(24, TimeUnit.HOURS)
//            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
//            .build()
//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//            "UpdateInterestingEvents",
//            ExistingPeriodicWorkPolicy.REPLACE,
//            updateRequest
//        )
//    }
}
