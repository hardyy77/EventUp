package com.example.eventup.utils

import com.example.eventup.models.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.ResultSet

object Repository {

    suspend fun getAllEvents(): List<Event> = withContext(Dispatchers.IO) {
        val query = "SELECT * FROM events"
        val resultSet = DatabaseHandler.executeQuery(query)
        mapResultSetToEvents(resultSet)
    }

    suspend fun addEvent(event: Event) = withContext(Dispatchers.IO) {
        val query = """
            INSERT INTO events (id, name, date, location, genres, description, interest, isFavorite) 
            VALUES ('${event.id}', '${event.name}', '${event.date}', '${event.location}', '${event.genres}', '${event.description}', ${event.interest}, ${event.isFavorite})
        """.trimIndent()
        DatabaseHandler.executeUpdate(query)
    }

    suspend fun updateEvent(event: Event) = withContext(Dispatchers.IO) {
        val query = """
            UPDATE events 
            SET name = '${event.name}', date = '${event.date}', location = '${event.location}', genres = '${event.genres}', description = '${event.description}', interest = ${event.interest}, isFavorite = ${event.isFavorite}
            WHERE id = '${event.id}'
        """.trimIndent()
        DatabaseHandler.executeUpdate(query)
    }

    suspend fun deleteEvent(eventId: String) = withContext(Dispatchers.IO) {
        val query = "DELETE FROM events WHERE id = '$eventId'"
        DatabaseHandler.executeUpdate(query)
    }

    private fun mapResultSetToEvents(resultSet: ResultSet?): List<Event> {
        val events = mutableListOf<Event>()
        resultSet?.let {
            while (it.next()) {
                events.add(
                    Event(
                        id = it.getString("id"),
                        name = it.getString("name"),
                        date = it.getString("date"),
                        location = it.getString("location"),
                        genres = it.getString("genres"),
                        description = it.getString("description"),
                        interest = it.getInt("interest"),
                        isFavorite = it.getBoolean("isFavorite")
                    )
                )
            }
        }
        return events
    }
}
