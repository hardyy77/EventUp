package com.example.eventup.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.activities.ManageEventActivity
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentHomeBinding
import com.example.eventup.models.Event
import com.example.eventup.utils.DatabaseHandler
import com.example.eventup.utils.EventUtils
import com.example.eventup.utils.FavoritesRepository
import com.example.eventup.utils.UserUtils
import com.example.eventup.workers.UpdateInterestingEventsWorker
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.sql.SQLException
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    // Adaptery do zarządzania wyświetlaniem list różnych kategorii wydarzeń
    private lateinit var interestingEventsAdapter: EventsAdapter
    private lateinit var popularEventsAdapter: EventsAdapter
    private lateinit var newestEventsAdapter: EventsAdapter

    // Binding do widoku fragmentu
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Flaga do śledzenia, czy odbiornik broadcastów jest zarejestrowany
    private var isReceiverRegistered = false

    // Aktualnie zalogowany użytkownik
    private val currentUser = UserUtils.getCurrentUser()

    // Mutex do synchronizacji operacji na ulubionych wydarzeniach
    private val mutex = Mutex()

    // Tworzenie widoku fragmentu
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerViews() // Konfiguracja RecyclerViews

        // Ładowanie danych dla różnych kategorii wydarzeń
        lifecycleScope.launch {
            try {
                fetchInterestingEvents()
                fetchPopularEvents()
                fetchNewestEvents()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to load events: ${e.message}", e)
            }
        }

        registerRefreshReceiver() // Rejestracja odbiornika broadcastów

        scheduleWork() // Planowanie zadania workera do aktualizacji interesujących wydarzeń

        return binding.root
    }

    // Czyszczenie zasobów przy niszczeniu widoku
    override fun onDestroyView() {
        super.onDestroyView()
        unregisterRefreshReceiver() // Wyrejestrowanie odbiornika broadcastów
        _binding = null
    }

    // Konfiguracja RecyclerViews dla różnych kategorii wydarzeń
    private fun setupRecyclerViews() {
        // Konfiguracja RecyclerView dla interesujących wydarzeń
        binding.interestingRecyclerView.layoutManager = LinearLayoutManager(context)
        interestingEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) }, // Akcja na kliknięcie wydarzenia
            onFavoriteClick = { event -> toggleFavorite(event, true) }, // Akcja na kliknięcie ulubionego
            onEditClick = { event -> editEvent(event) }, // Akcja na kliknięcie edycji
            onDeleteClick = { event -> deleteEvent(event) } // Akcja na kliknięcie usunięcia
        )
        binding.interestingRecyclerView.adapter = interestingEventsAdapter

        // Konfiguracja RecyclerView dla popularnych wydarzeń
        binding.popularRecyclerView.layoutManager = LinearLayoutManager(context)
        popularEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event, false) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.popularRecyclerView.adapter = popularEventsAdapter

        // Konfiguracja RecyclerView dla najnowszych wydarzeń
        binding.newestRecyclerView.layoutManager = LinearLayoutManager(context)
        newestEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event, false) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.newestRecyclerView.adapter = newestEventsAdapter
    }

    // Funkcja do pobierania interesujących wydarzeń z bazy danych
    private suspend fun fetchInterestingEvents() {
        val query = "SELECT * FROM todayinterestingevents" // Zapytanie SQL
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) } // Wykonanie zapytania w tle
        val events = mutableListOf<Event>() // Lista wydarzeń do wypełnienia

        // Przetwarzanie wyników zapytania
        if (resultSet != null) {
            try {
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
                    events.add(event)
                }
            } catch (e: SQLException) {
                e.printStackTrace() // Obsługa błędów SQL
                Log.e("HomeFragment", "Failed to process result set for interesting events: ${e.message}", e)
            } finally {
                try {
                    resultSet.close() // Zamknięcie resultSet
                } catch (e: SQLException) {
                    e.printStackTrace()
                    Log.e("HomeFragment", "Failed to close result set: ${e.message}", e)
                }
            }
        } else {
            Log.e("HomeFragment", "Failed to fetch interesting events: resultSet is null") // Logowanie błędu
        }

        syncEvents(events, interestingEventsAdapter) // Synchronizacja pobranych wydarzeń z adapterem
    }

    // Funkcja do pobierania popularnych wydarzeń z bazy danych
    private suspend fun fetchPopularEvents() {
        val query = "SELECT * FROM events ORDER BY interest DESC LIMIT 4" // Zapytanie SQL
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) } // Wykonanie zapytania w tle
        val events = mutableListOf<Event>() // Lista wydarzeń do wypełnienia

        // Przetwarzanie wyników zapytania
        if (resultSet != null) {
            try {
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
                    events.add(event)
                }
            } catch (e: SQLException) {
                e.printStackTrace() // Obsługa błędów SQL
                Log.e("HomeFragment", "Failed to process result set for popular events: ${e.message}", e)
            } finally {
                try {
                    resultSet.close() // Zamknięcie resultSet
                } catch (e: SQLException) {
                    e.printStackTrace()
                    Log.e("HomeFragment", "Failed to close result set: ${e.message}", e)
                }
            }
        } else {
            Log.e("HomeFragment", "Failed to fetch popular events: resultSet is null") // Logowanie błędu
        }

        syncEvents(events, popularEventsAdapter) // Synchronizacja pobranych wydarzeń z adapterem
    }

    // Funkcja do pobierania najnowszych wydarzeń z bazy danych
    private suspend fun fetchNewestEvents() {
        val query = "SELECT * FROM events ORDER BY added_date DESC LIMIT 4" // Zapytanie SQL
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) } // Wykonanie zapytania w tle
        val events = mutableListOf<Event>() // Lista wydarzeń do wypełnienia

        // Przetwarzanie wyników zapytania
        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    val event = Event(
                        id = resultSet.getInt("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest"),
                        addedDate = resultSet.getString("added_date")
                    )
                    events.add(event)
                }
            } catch (e: SQLException) {
                e.printStackTrace() // Obsługa błędów SQL
                Log.e("HomeFragment", "Failed to process result set for newest events: ${e.message}", e)
            } finally {
                try {
                    resultSet.close() // Zamknięcie resultSet
                } catch (e: SQLException) {
                    e.printStackTrace()
                    Log.e("HomeFragment", "Failed to close result set: ${e.message}", e)
                }
            }
        } else {
            Log.e("HomeFragment", "Failed to fetch newest events: resultSet is null") // Logowanie błędu
        }

        syncEvents(events, newestEventsAdapter) // Synchronizacja pobranych wydarzeń z adapterem
    }

    // Funkcja do nawigacji do szczegółów wydarzenia
    private fun navigateToEventDetails(event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java)
        val eventJson = Gson().toJson(event)
        intent.putExtra("event", eventJson)
        startActivity(intent) // Uruchomienie nowej aktywności z przekazaniem danych o wydarzeniu
    }

    // Funkcja do przełączania stanu ulubionego wydarzenia
    private fun toggleFavorite(event: Event, isInteresting: Boolean) {
        if (currentUser == null) {
            Toast.makeText(context, "Please log in to favorite events.", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = if (isInteresting) interestingEventsAdapter else popularEventsAdapter
        val position = adapter.events.indexOf(event)
        println("Toggling favorite for event: ${event.id}")

        lifecycleScope.launch {
            mutex.withLock {
                handleFavoriteToggle(event, adapter, position)
            }
        }
    }

    // Funkcja obsługująca przełączanie stanu ulubionego wydarzenia
    private suspend fun handleFavoriteToggle(event: Event, adapter: EventsAdapter, position: Int) {
        try {
            Log.i("HomeFragment", "Checking current favorite state in the database for event: ${event.id}")
            val isFavoriteNow = withContext(Dispatchers.IO) {
                currentUser?.let { FavoritesRepository.isEventFavorite(event.id.toString(), it.uid) }
            } ?: false

            if (isFavoriteNow != event.isFavorite) {
                Log.i("HomeFragment", "Mismatch between UI and database state. Syncing UI with database.")
                event.isFavorite = isFavoriteNow
                adapter.notifyItemChanged(position)
                return
            }

            if (isFavoriteNow) {
                withContext(Dispatchers.IO) {
                    currentUser?.let { FavoritesRepository.removeEventFromFavorites(event.id.toString(), it.uid) }
                }
                Log.i("HomeFragment", "Removed event: ${event.id} from favorites")
                event.isFavorite = false
            } else {
                withContext(Dispatchers.IO) {
                    currentUser?.let { FavoritesRepository.addEventToFavorites(event.id.toString(), it.uid) }
                }
                Log.i("HomeFragment", "Added event: ${event.id} to favorites")
                event.isFavorite = true
            }

            adapter.notifyItemChanged(position)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to toggle favorite: ${e.message}", e)
        }
    }

    // Funkcja do nawigacji do edycji wydarzenia
    private fun editEvent(event: Event) {
        val intent = Intent(context, ManageEventActivity::class.java)
        intent.putExtra("EVENT_ID", event.id)
        startActivity(intent) // Uruchomienie nowej aktywności z przekazaniem ID wydarzenia do edycji
    }

    // Funkcja do usuwania wydarzenia
    private fun deleteEvent(event: Event) {
        lifecycleScope.launch {
            try {
                EventUtils.deleteEvent(event.id.toString())
                sendRefreshBroadcast() // Wysłanie broadcastu do odświeżenia listy wydarzeń
                updateUI() // Aktualizacja interfejsu użytkownika
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to delete event: ${e.message}")
            }
        }
    }

    // Funkcja do aktualizacji interfejsu użytkownika
    private fun updateUI() {
        lifecycleScope.launch {
            try {
                fetchPopularEvents()
                fetchInterestingEvents()
                fetchNewestEvents()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to update UI: ${e.message}", e)
            }
        }
    }

    // Funkcja do rejestracji odbiornika broadcastów
    private fun registerRefreshReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter("com.example.eventup.REFRESH_EVENTS")
            context?.registerReceiver(refreshReceiver, filter)
            isReceiverRegistered = true
        }
    }

    // Funkcja do wyrejestrowania odbiornika broadcastów
    private fun unregisterRefreshReceiver() {
        if (isReceiverRegistered) {
            context?.unregisterReceiver(refreshReceiver)
            isReceiverRegistered = false
        }
    }

    // Odbiornik broadcastów do odświeżania listy wydarzeń
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("HomeFragment", "Received broadcast to refresh events")
            updateUI()
        }
    }

    // Funkcja do wysłania broadcastu do odświeżenia listy wydarzeń
    private fun sendRefreshBroadcast() {
        val intent = Intent("com.example.eventup.REFRESH_EVENTS")
        context?.sendBroadcast(intent)
    }

    // Funkcja do planowania zadania workera
    private fun scheduleWork() {
        val workManager = WorkManager.getInstance(requireContext())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "UpdateInterestingEventsWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Funkcja do synchronizacji wydarzeń z bazą danych
    private fun syncEvents(events: List<Event>, adapter: EventsAdapter) {
        lifecycleScope.launch {
            try {
                val favoriteEventIds = if (currentUser != null) {
                    withContext(Dispatchers.IO) {
                        FavoritesRepository.getUserEventIds(currentUser.uid)
                    }
                } else {
                    emptyList()
                }
                adapter.submitList(events.map { event ->
                    event.copy().apply {
                        isFavorite = favoriteEventIds.contains(id.toString())
                    }
                })
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to sync events: ${e.message}", e)
            }
        }
    }
}
