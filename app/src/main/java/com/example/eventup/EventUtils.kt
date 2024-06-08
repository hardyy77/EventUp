package com.example.eventup

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

object EventUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getInterestingEvents(context: Context, callback: (List<Event>) -> Unit) {
        //val firestore = FirebaseFirestore.getInstance()
        val sharedPreferences = context.getSharedPreferences("EventUpPreferences", Context.MODE_PRIVATE)
        val lastUpdateTime = sharedPreferences.getLong("lastUpdateTime", 0)
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000

        if (currentTime - lastUpdateTime >= oneDayMillis) {
            updateInterestingEvents(context, callback)
        } else {
            val gson = Gson()
            val json = sharedPreferences.getString("selectedInterestingEvents", "")
            val type = object : TypeToken<List<Event>>() {}.type
            val selectedEvents: List<Event> = gson.fromJson(json, type)
            callback(selectedEvents)
        }
    }

    private fun updateInterestingEvents(context: Context, callback: (List<Event>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val currentDate = dateFormat.format(Date())
        firestore.collection("events")
            .whereGreaterThanOrEqualTo("date", currentDate)
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: "",
                        document.getString("genres") ?: "",
                        document.getString("description") ?: "",
                        document.getLong("interest")?.toInt() ?: 0
                    )
                }
                val selectedEvents = events.shuffled().take(4)
                saveSelectedEventsToPreferences(context, selectedEvents)
                callback(selectedEvents)
            }
            .addOnFailureListener { exception ->
                println("Error getting interesting events: $exception")
            }
    }

    private fun saveSelectedEventsToPreferences(context: Context, events: List<Event>) {
        val sharedPreferences = context.getSharedPreferences("EventUpPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(events)
        editor.putString("selectedInterestingEvents", json)
        editor.putLong("lastUpdateTime", System.currentTimeMillis())
        editor.apply()
    }

    fun getPopularEvents(callback: (List<Event>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val currentDate = dateFormat.format(Date())
        firestore.collection("events")
            .whereGreaterThanOrEqualTo("date", currentDate)
            .orderBy("date")
            .orderBy("interest", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(4)
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: "",
                        document.getString("genres") ?: "",
                        document.getString("description") ?: "",
                        document.getLong("interest")?.toInt() ?: 0
                    )
                }
                callback(events)
            }
            .addOnFailureListener { exception ->
                println("Error getting popular events: $exception")
            }
    }

    fun getAllEvents(callback: (List<Event>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: "",
                        document.getString("genres") ?: "",
                        document.getString("description") ?: "",
                        document.getLong("interest")?.toInt() ?: 0
                    )
                }
                callback(events)
            }
            .addOnFailureListener { exception ->
                println("Error getting events: $exception")
            }
    }

    fun filterEvents(events: List<Event>, query: String): List<Event> {
        return events.filter { event ->
            event.name.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true) ||
                    event.date.contains(query, ignoreCase = true) ||
                    event.genres.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true)
        }
    }

}
