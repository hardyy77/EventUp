package com.example.eventup.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.eventup.models.Event
import com.example.eventup.utils.DatabaseHandler
import com.example.eventup.utils.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateInterestingEventsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val events = withContext(Dispatchers.IO) {
                EventRepository.getAllEvents()
            }
            if (events.isNotEmpty()) {
                val selectedEvents = events.shuffled().take(4)
                saveTodayInterestingEvents(selectedEvents)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun saveTodayInterestingEvents(events: List<Event>) {
        withContext(Dispatchers.IO) {
            val deleteQuery = "DELETE FROM todayinterestingevents"
            DatabaseHandler.executeUpdate(deleteQuery)

            events.forEach { event ->
                val insertQuery = """
                    INSERT INTO todayinterestingevents (id, name, location, date, genres, description, interest)
                    VALUES ('${event.id}', '${event.name}', '${event.location}', '${event.date}', '${event.genres}', '${event.description}', ${event.interest})
                """.trimIndent()
                DatabaseHandler.executeUpdate(insertQuery)
            }
        }
    }
}
