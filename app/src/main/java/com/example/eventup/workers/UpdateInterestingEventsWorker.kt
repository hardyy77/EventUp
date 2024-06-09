package com.example.eventup.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.eventup.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class UpdateInterestingEventsWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val firestore = FirebaseFirestore.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        firestore.collection("events")
            .whereGreaterThanOrEqualTo("date", currentDate)
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        location = document.getString("location") ?: "",
                        date = document.getString("date") ?: "",
                        genres = document.getString("genres") ?: "",
                        description = document.getString("description") ?: "",
                        interest = document.getLong("interest")?.toInt() ?: 0,
                        isFavorite = document.getBoolean("isFavorite") ?: false
                    )
                }
                val selectedEvents = events.shuffled().take(4)
                saveSelectedEventsToPreferences(selectedEvents)
            }
            .addOnFailureListener { exception ->
                println("Error getting interesting events: $exception")
            }

        return Result.success()
    }

    private fun saveSelectedEventsToPreferences(events: List<Event>) {
        val sharedPreferences = applicationContext.getSharedPreferences("EventUpPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(events)
        editor.putString("selectedInterestingEvents", json)
        editor.putLong("lastUpdateTime", System.currentTimeMillis())
        editor.apply()
    }
}
