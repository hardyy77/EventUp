package com.example.eventup.utils

import com.example.eventup.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EventUtils {

    suspend fun getAllEvents(): List<Event> {
        val query = "SELECT * FROM events"
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

        while (resultSet?.next() == true) {
            val event = Event(
                id = resultSet.getString("id"),
                name = resultSet.getString("name"),
                location = resultSet.getString("location"),
                date = resultSet.getString("date"),
                genres = resultSet.getString("genres"),
                description = resultSet.getString("description"),
                interest = resultSet.getInt("interest"),
                isFavorite = resultSet.getBoolean("isFavorite")
            )
            events.add(event)
        }
        return events
    }

    suspend fun getPopularEvents(): List<Event> {
        val query = "SELECT * FROM events ORDER BY interest DESC LIMIT 10"
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

        while (resultSet?.next() == true) {
            val event = Event(
                id = resultSet.getString("id"),
                name = resultSet.getString("name"),
                location = resultSet.getString("location"),
                date = resultSet.getString("date"),
                genres = resultSet.getString("genres"),
                description = resultSet.getString("description"),
                interest = resultSet.getInt("interest"),
                isFavorite = resultSet.getBoolean("isFavorite")
            )
            events.add(event)
        }
        return events
    }

    suspend fun addEventToFavorites(event: Event, userId: String) {
        val query = "INSERT INTO favorites (event_id, user_id) VALUES ('${event.id}', '$userId')"
        withContext(Dispatchers.IO) {
            DatabaseHandler.executeUpdate(query)
        }
    }

    suspend fun removeEventFromFavorites(event: Event, userId: String) {
        val query = "DELETE FROM favorites WHERE event_id = '${event.id}' AND user_id = '$userId'"
        withContext(Dispatchers.IO) {
            DatabaseHandler.executeUpdate(query)
        }
    }

    suspend fun deleteEvent(eventId: String) {
        val query = "DELETE FROM events WHERE id = '$eventId'"
        withContext(Dispatchers.IO) {
            DatabaseHandler.executeUpdate(query)
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
