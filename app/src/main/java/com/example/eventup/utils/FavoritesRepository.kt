package com.example.eventup.utils

import com.example.eventup.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FavoritesRepository {

    suspend fun getFavorites(userId: String): List<Event> {
        val query = """
            SELECT e.* FROM events e 
            JOIN user_favorites f ON e.id = f.event_id 
            WHERE f.user_id = '$userId'
        """.trimIndent()
        return withContext(Dispatchers.IO) {
            DatabaseHandler.executeQuery(query)?.use { resultSet ->
                val favoriteEvents = mutableListOf<Event>()
                while (resultSet.next()) {
                    val event = Event(
                        id = resultSet.getString("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest")
                    )
                    favoriteEvents.add(event)
                }
                favoriteEvents
            } ?: emptyList()
        }
    }

    suspend fun getUserEventIds(userId: String): List<String> {
        val query = "SELECT event_id FROM user_favorites WHERE user_id = '$userId'"
        return withContext(Dispatchers.IO) {
            DatabaseHandler.executeQuery(query)?.use { resultSet ->
                val eventIds = mutableListOf<String>()
                while (resultSet.next()) {
                    eventIds.add(resultSet.getString("event_id"))
                }
                eventIds
            } ?: emptyList()
        }
    }

    suspend fun addEventToFavorites(eventId: String, userId: String) {
        val query = "INSERT INTO user_favorites (user_id, event_id) VALUES ('$userId', '$eventId')"
        withContext(Dispatchers.IO) {
            DatabaseHandler.executeUpdate(query)
        }
    }

    suspend fun removeEventFromFavorites(eventId: String, userId: String) {
        val query = "DELETE FROM user_favorites WHERE user_id = '$userId' AND event_id = '$eventId'"
        withContext(Dispatchers.IO) {
            DatabaseHandler.executeUpdate(query)
        }
    }

    suspend fun isEventFavorite(eventId: String, userId: String): Boolean {
        val query = "SELECT 1 FROM user_favorites WHERE event_id = '$eventId' AND user_id = '$userId' LIMIT 1"
        return withContext(Dispatchers.IO) {
            DatabaseHandler.executeQuery(query)?.use { resultSet ->
                resultSet.next()
            } ?: false
        }
    }
}
