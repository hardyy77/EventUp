package com.example.eventup.utils

import com.example.eventup.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FavoritesRepository {

    suspend fun addEventToFavorites(event: Event, userId: String) {
        val query = "INSERT INTO favorites (userId, eventId) VALUES ('$userId', '${event.id}')"
        withContext(Dispatchers.IO) {
            DatabaseHandler.executeUpdate(query)
            event.interest += 1
            EventRepository.updateEvent(event)
        }
    }

    suspend fun removeEventFromFavorites(event: Event, userId: String) {
        val query = "DELETE FROM favorites WHERE userId = '$userId' AND eventId = '${event.id}'"
        withContext(Dispatchers.IO) {
            DatabaseHandler.executeUpdate(query)
            event.interest -= 1
            EventRepository.updateEvent(event)
        }
    }

    suspend fun getFavorites(userId: String): List<Event> {
        val query = """
            SELECT e.* FROM events e 
            JOIN favorites f ON e.id = f.eventId 
            WHERE f.userId = '$userId'
        """.trimIndent()
        return withContext(Dispatchers.IO) {
            val resultSet = DatabaseHandler.executeQuery(query)
            val favoriteEvents = mutableListOf<Event>()
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
                favoriteEvents.add(event)
            }
            favoriteEvents
        }
    }
}
