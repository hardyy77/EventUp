package com.example.eventup.utils

import android.util.Log
import com.example.eventup.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EventUtils {

    // Funkcja pobierająca wszystkie wydarzenia z bazy danych
    suspend fun getAllEvents(): List<Event> {
        val query = "SELECT * FROM events"
        Log.d("EventUtils", "Executing getAllEvents query: $query")
        val events = mutableListOf<Event>()
        try {
            val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
            while (resultSet?.next() == true) {
                val event = Event(
                    id = resultSet.getInt("id"),
                    name = resultSet.getString("name"),
                    location = resultSet.getString("location"),
                    date = resultSet.getString("date"),
                    genres = resultSet.getString("genres"),
                    description = resultSet.getString("description"),
                    interest = resultSet.getInt("interest")
                )
                events.add(event)
            }
            Log.d("EventUtils", "Fetched ${events.size} events")
        } catch (e: Exception) {
            Log.e("EventUtils", "Error fetching all events: ${e.message}", e)
        }
        return events
    }

    // Funkcja usuwająca wydarzenie z bazy danych
    suspend fun deleteEvent(eventId: String) {
        Log.d("EventUtils", "Deleting event with id: $eventId")
        try {
            withContext(Dispatchers.IO) {
                // Najpierw usuń powiązane rekordy w tabeli user_favorites
                DatabaseHandler.executeUpdate("DELETE FROM user_favorites WHERE event_id = '$eventId'")
                // Następnie usuń rekord w tabeli events
                DatabaseHandler.executeUpdate("DELETE FROM events WHERE id = '$eventId'")
            }
            Log.d("EventUtils", "Deleted event with id: $eventId")
        } catch (e: Exception) {
            Log.e("EventUtils", "Error deleting event with id: $eventId: ${e.message}", e)
        }
    }

    // Funkcja filtrująca wydarzenia na podstawie zapytania użytkownika
    fun filterEvents(events: List<Event>, query: String): List<Event> {
        Log.d("EventUtils", "Filtering events with query: $query")
        val filteredEvents = events.filter { event ->
            event.name.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true) ||
                    event.date.contains(query, ignoreCase = true) ||
                    event.genres.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true)
        }
        Log.d("EventUtils", "Found ${filteredEvents.size} events matching query: $query")
        return filteredEvents
    }
}
