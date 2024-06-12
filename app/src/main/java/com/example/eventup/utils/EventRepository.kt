package com.example.eventup.utils

import com.example.eventup.models.Event

object EventRepository {

    suspend fun saveEvent(event: Event) {
        val query = """
            INSERT INTO events (name, location, date, genres, description, interest, isFavorite)
            VALUES ('${event.name}', '${event.location}', '${event.date}', '${event.genres}', '${event.description}', ${event.interest}, ${event.isFavorite})
        """.trimIndent()
        DatabaseHandler.executeUpdate(query)
    }

    suspend fun updateEvent(event: Event) {
        val query = """
            UPDATE events 
            SET name = '${event.name}', location = '${event.location}', date = '${event.date}', genres = '${event.genres}', description = '${event.description}', interest = ${event.interest}, isFavorite = ${event.isFavorite}
            WHERE id = '${event.id}'
        """.trimIndent()
        DatabaseHandler.executeUpdate(query)
    }

    suspend fun getAllEvents(): List<Event> {
        val query = "SELECT * FROM events"
        val resultSet = DatabaseHandler.executeQuery(query)
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

    suspend fun deleteEvent(eventId: String) {
        val query = "DELETE FROM events WHERE id = '$eventId'"
        DatabaseHandler.executeUpdate(query)
    }

    suspend fun syncFavorites(events: List<Event>, userId: String) {
        val query = "SELECT eventId FROM favorites WHERE userId = '$userId'"
        val resultSet = DatabaseHandler.executeQuery(query)
        val favoriteEventIds = mutableListOf<String>()
        while (resultSet?.next() == true) {
            favoriteEventIds.add(resultSet.getString("eventId"))
        }
        events.forEach { it.isFavorite = favoriteEventIds.contains(it.id) }
    }
}
