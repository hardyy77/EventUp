package com.example.eventup.utils

import android.content.Context
import com.example.eventup.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

object EventUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getInterestingEvents(context: Context, callback: (List<Event>) -> Unit) {
        val sharedPreferences = context.getSharedPreferences("EventUpPreferences", Context.MODE_PRIVATE)
        val lastUpdateTime = sharedPreferences.getLong("lastUpdateTime", 0)
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000

        if (currentTime - lastUpdateTime >= oneDayMillis) {
            updateInterestingEvents(context, callback)
        } else {
            val gson = Gson()
            val json = sharedPreferences.getString("selectedInterestingEvents", "")
            if (!json.isNullOrEmpty()) {
                val type = object : TypeToken<List<Event>>() {}.type
                val selectedEvents: List<Event> = gson.fromJson(json, type)
                selectedEvents.forEach { println("Restored interesting event ID from prefs: ${it.id}") }
                restoreInterestingEventsWithIDs(selectedEvents, callback)
            } else {
                callback(emptyList())
            }
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
                    val event = Event(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        location = document.getString("location") ?: "",
                        date = document.getString("date") ?: "",
                        genres = document.getString("genres") ?: "",
                        description = document.getString("description") ?: "",
                        interest = document.getLong("interest")?.toInt() ?: 0,
                        isFavorite = document.getBoolean("isFavorite") ?: false
                    )
                    println("Fetched interesting event with ID: ${event.id}")
                    event
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
        events.forEach { println("Saving interesting event ID to prefs: ${it.id}") }
        editor.putString("selectedInterestingEvents", json)
        editor.putLong("lastUpdateTime", System.currentTimeMillis())
        editor.apply()
    }

    private fun restoreInterestingEventsWithIDs(events: List<Event>, callback: (List<Event>) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val eventsWithIDs = mutableListOf<Event>()
        events.forEach { event ->
            if (event.id.isEmpty()) {
                firestore.collection("events")
                    .whereEqualTo("name", event.name)
                    .whereEqualTo("location", event.location)
                    .whereEqualTo("date", event.date)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            event.id = document.id
                            println("Assigned ID from Firestore to event: ${event.id}")
                        }
                        eventsWithIDs.add(event)
                        if (eventsWithIDs.size == events.size) {
                            callback(eventsWithIDs)
                        }
                    }
                    .addOnFailureListener { exception ->
                        println("Error getting event ID from Firestore: $exception")
                        eventsWithIDs.add(event)
                        if (eventsWithIDs.size == events.size) {
                            callback(eventsWithIDs)
                        }
                    }
            } else {
                eventsWithIDs.add(event)
                if (eventsWithIDs.size == events.size) {
                    callback(eventsWithIDs)
                }
            }
        }
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
                    val event = Event(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        location = document.getString("location") ?: "",
                        date = document.getString("date") ?: "",
                        genres = document.getString("genres") ?: "",
                        description = document.getString("description") ?: "",
                        interest = document.getLong("interest")?.toInt() ?: 0,
                        isFavorite = document.getBoolean("isFavorite") ?: false
                    )
                    println("Fetched popular event with ID: ${event.id}")
                    event
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
                    val event = Event(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        location = document.getString("location") ?: "",
                        date = document.getString("date") ?: "",
                        genres = document.getString("genres") ?: "",
                        description = document.getString("description") ?: "",
                        interest = document.getLong("interest")?.toInt() ?: 0,
                        isFavorite = document.getBoolean("isFavorite") ?: false
                    )
                    println("Fetched event with ID: ${event.id}")
                    event
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
