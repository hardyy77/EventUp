package com.example.eventup.utils

import com.example.eventup.models.Event

object EventRepository {

    // Funkcja pobierająca wszystkie wydarzenia z bazy danych
    suspend fun getAllEvents(): List<Event> {
        val query = "SELECT * FROM events"
        val resultSet = DatabaseHandler.executeQuery(query)
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

}
