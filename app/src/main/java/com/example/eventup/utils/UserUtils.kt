package com.example.eventup.utils

import com.example.eventup.models.Event
import com.example.eventup.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserUtils {

    private var currentUser: User? = null

    fun getCurrentUserId(): Int? {
        return currentUser?.id
    }

    fun getCurrentUser(): User? {
        return currentUser
    }

    fun setCurrentUser(user: User) {
        currentUser = user
    }

    fun logoutUser() {
        currentUser = null
    }

    suspend fun getFavoriteEvents(userId: String, callback: (List<Event>) -> Unit) = withContext(Dispatchers.IO) {
        val query = "SELECT events.* FROM events JOIN favorites ON events.id = favorites.event_id WHERE favorites.user_id = $userId"
        val resultSet = DatabaseHandler.executeQuery(query)
        val events = mutableListOf<Event>()

        while (resultSet?.next() == true) {
            val event = Event(
                id = resultSet.getInt("id").toString(),
                name = resultSet.getString("name"),
                location = resultSet.getString("location"),
                date = resultSet.getString("date"),
                genres = resultSet.getString("genres"),
                description = resultSet.getString("description"),
                interest = resultSet.getInt("interest"),
                isFavorite = true
            )
            events.add(event)
        }
        withContext(Dispatchers.Main) {
            callback(events)
        }
    }
}
