package com.example.eventup.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.eventup.models.Event
import com.example.eventup.utils.DatabaseHandler
import com.example.eventup.utils.EventRepository
import com.example.eventup.utils.EventUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Klasa UpdateInterestingEventsWorker, dziedzicząca po CoroutineWorker, aby wykonywać zadania w tle
class UpdateInterestingEventsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    // Główna funkcja wykonywana w tle
    override suspend fun doWork(): Result {
        return try {
            // Pobranie wszystkich wydarzeń z bazy danych w kontekście IO
            val events = withContext(Dispatchers.IO) {
                EventUtils.getAllEvents()
            }
            // Jeśli pobrane wydarzenia nie są puste, wybieramy 4 losowe wydarzenia
            if (events.isNotEmpty()) {
                val selectedEvents = events.shuffled().take(4)
                // Zapisujemy wybrane wydarzenia jako interesujące wydarzenia dnia
                saveTodayInterestingEvents(selectedEvents)
                Log.d("UpdateInterestingEventsWorker", "Saved ${selectedEvents.size} interesting events for today")
            }
            // Zwracamy sukces
            Result.success()
        } catch (e: Exception) {
            // W przypadku błędu, logujemy go i zwracamy niepowodzenie
            Log.e("UpdateInterestingEventsWorker", "Error updating interesting events: ${e.message}", e)
            Result.failure()
        }
    }

    // Funkcja zapisująca interesujące wydarzenia dnia
    private suspend fun saveTodayInterestingEvents(events: List<Event>) {
        withContext(Dispatchers.IO) {
            try {
                // Usunięcie wszystkich istniejących rekordów z tabeli todayinterestingevents
                val deleteQuery = "DELETE FROM todayinterestingevents"
                DatabaseHandler.executeUpdate(deleteQuery)
                Log.d("UpdateInterestingEventsWorker", "Cleared todayinterestingevents table")

                // Dodanie nowych interesujących wydarzeń do tabeli todayinterestingevents
                events.forEach { event ->
                    val insertQuery = """
                        INSERT INTO todayinterestingevents (id, name, location, date, genres, description, interest)
                        VALUES ('${event.id}', '${event.name}', '${event.location}', '${event.date}', '${event.genres}', '${event.description}', ${event.interest})
                    """.trimIndent()
                    DatabaseHandler.executeUpdate(insertQuery)
                }
                Log.d("UpdateInterestingEventsWorker", "Inserted ${events.size} events into todayinterestingevents table")
            } catch (e: Exception) {
                // W przypadku błędu, logujemy go
                Log.e("UpdateInterestingEventsWorker", "Error saving interesting events: ${e.message}", e)
            }
        }
    }
}
