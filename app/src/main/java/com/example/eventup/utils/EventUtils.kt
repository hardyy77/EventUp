package com.example.eventup.utils

import com.example.eventup.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EventUtils {

    suspend fun getAllEvents(): List<Event> {
        val query = "SELECT * FROM events"
        println("Executing getAllEvents query: $query")
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

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
        return events
    }

    suspend fun getPopularEvents(): List<Event> {
        val query = "SELECT * FROM events ORDER BY interest DESC LIMIT 10"
        println("Executing getPopularEvents query: $query")
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

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
        return events
    }

    suspend fun deleteEvent(eventId: String) {
        withContext(Dispatchers.IO) {
            // Najpierw usuń powiązane rekordy w tabeli user_favorites
            DatabaseHandler.executeUpdate("DELETE FROM user_favorites WHERE event_id = '$eventId'")
            // Następnie usuń rekord w tabeli events
            DatabaseHandler.executeUpdate("DELETE FROM events WHERE id = '$eventId'")
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
