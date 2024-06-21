package com.example.eventup.utils

import android.util.Log
import com.example.eventup.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FavoritesRepository {

    // Funkcja pobierająca ulubione wydarzenia użytkownika
    suspend fun getFavorites(userId: String): List<Event> {
        val query = """
            SELECT e.* FROM events e 
            JOIN user_favorites f ON e.id = f.event_id 
            WHERE f.user_id = '$userId'
        """.trimIndent()
        return withContext(Dispatchers.IO) {
            try {
                DatabaseHandler.executeQuery(query)?.use { resultSet ->
                    val favoriteEvents = mutableListOf<Event>()
                    while (resultSet.next()) {
                        val event = Event(
                            id = resultSet.getInt("id"),
                            name = resultSet.getString("name"),
                            location = resultSet.getString("location"),
                            date = resultSet.getString("date"),
                            genres = resultSet.getString("genres"),
                            description = resultSet.getString("description"),
                            interest = resultSet.getInt("interest")
                        )
                        favoriteEvents.add(event)
                    }
                    Log.d("FavoritesRepository", "Fetched ${favoriteEvents.size} favorite events for user: $userId")
                    favoriteEvents
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("FavoritesRepository", "Error fetching favorites for user $userId: ${e.message}", e)
                emptyList()
            }
        }
    }

    // Funkcja pobierająca identyfikatory wydarzeń ulubionych przez użytkownika
    suspend fun getUserEventIds(userId: String): List<String> {
        val query = "SELECT event_id FROM user_favorites WHERE user_id = '$userId'"
        return withContext(Dispatchers.IO) {
            try {
                DatabaseHandler.executeQuery(query)?.use { resultSet ->
                    val eventIds = mutableListOf<String>()
                    while (resultSet.next()) {
                        eventIds.add(resultSet.getString("event_id"))
                    }
                    Log.d("FavoritesRepository", "Fetched ${eventIds.size} favorite event IDs for user: $userId")
                    eventIds
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e("FavoritesRepository", "Error fetching favorite event IDs for user $userId: ${e.message}", e)
                emptyList()
            }
        }
    }

    // Funkcja dodająca wydarzenie do ulubionych użytkownika
    suspend fun addEventToFavorites(eventId: String, userId: String) {
        val query = "INSERT INTO user_favorites (user_id, event_id) VALUES ('$userId', '$eventId')"
        withContext(Dispatchers.IO) {
            try {
                DatabaseHandler.executeUpdate(query)
                Log.d("FavoritesRepository", "Added event $eventId to favorites for user $userId")
            } catch (e: Exception) {
                Log.e("FavoritesRepository", "Error adding event $eventId to favorites for user $userId: ${e.message}", e)
            }
        }
    }

    // Funkcja usuwająca wydarzenie z ulubionych użytkownika
    suspend fun removeEventFromFavorites(eventId: String, userId: String) {
        val query = "DELETE FROM user_favorites WHERE user_id = '$userId' AND event_id = '$eventId'"
        withContext(Dispatchers.IO) {
            try {
                DatabaseHandler.executeUpdate(query)
                Log.d("FavoritesRepository", "Removed event $eventId from favorites for user $userId")
            } catch (e: Exception) {
                Log.e("FavoritesRepository", "Error removing event $eventId from favorites for user $userId: ${e.message}", e)
            }
        }
    }

    // Funkcja sprawdzająca, czy wydarzenie jest ulubione przez użytkownika
    suspend fun isEventFavorite(eventId: String, userId: String): Boolean {
        val query = "SELECT 1 FROM user_favorites WHERE event_id = '$eventId' AND user_id = '$userId' LIMIT 1"
        return withContext(Dispatchers.IO) {
            try {
                DatabaseHandler.executeQuery(query)?.use { resultSet ->
                    val isFavorite = resultSet.next()
                    Log.d("FavoritesRepository", "Event $eventId is ${if (isFavorite) "a" else "not a"} favorite for user $userId")
                    isFavorite
                } ?: false
            } catch (e: Exception) {
                Log.e("FavoritesRepository", "Error checking if event $eventId is favorite for user $userId: ${e.message}", e)
                false
            }
        }
    }
}
