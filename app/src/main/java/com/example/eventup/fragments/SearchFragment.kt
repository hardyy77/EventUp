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
import androidx.appcompat.widget.SearchView
import com.example.eventup.models.Event
import com.example.eventup.utils.EventUtils
import com.example.eventup.utils.UserUtils
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.activities.ManageEventActivity
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentSearchBinding
import com.example.eventup.utils.FavoritesRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SearchFragment : Fragment() {

    // Deklaracja adaptera do wyświetlania wydarzeń i listy wszystkich wydarzeń
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var allEvents: List<Event>

    // ID użytkownika i zmienne związane z synchronizacją
    private var userId: String? = null
    private val mutex = Mutex()
    private val currentUser = UserUtils.getCurrentUser()

    // Funkcja tworząca widok fragmentu
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Konfiguracja RecyclerView i adaptera
        binding.eventsRecyclerView.layoutManager = LinearLayoutManager(context)
        eventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.eventsRecyclerView.adapter = eventsAdapter

        // Konfiguracja SearchView
        setupSearchView(binding.searchView)

        // Pobranie wszystkich wydarzeń
        fetchAllEvents()

        // Rejestracja odbiornika broadcastu
        registerRefreshReceiver()

        return binding.root
    }

    // Funkcja wywoływana przy niszczeniu widoku fragmentu
    override fun onDestroyView() {
        super.onDestroyView()
        context?.unregisterReceiver(refreshReceiver)
    }

    // Konfiguracja SearchView do filtrowania wydarzeń
    private fun setupSearchView(searchView: SearchView) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterEvents(newText ?: "")
                return true
            }
        })
    }

    // Funkcja filtrująca wydarzenia na podstawie zapytania
    private fun filterEvents(query: String) {
        val filteredEvents = EventUtils.filterEvents(allEvents, query)
        Log.d("SearchFragment", "Filtering events with query: $query, found ${filteredEvents.size} events")
        eventsAdapter.submitList(filteredEvents)
    }

    // Funkcja pobierająca wszystkie wydarzenia
    private fun fetchAllEvents() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val events = withContext(Dispatchers.IO) { EventUtils.getAllEvents() }
                allEvents = events
                Log.d("SearchFragment", "Fetched ${allEvents.size} events")
                allEvents.forEach {
                    Log.d("SearchFragment", "Event: ${it.name}, ${it.location}, ${it.date}")
                }
                syncEvents(allEvents) {
                    eventsAdapter.submitList(allEvents)
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Failed to fetch events: ${e.message}", e)
            }
        }
    }

    // Funkcja przełączająca stan ulubionego wydarzenia
    private fun toggleFavorite(event: Event) {
        if (currentUser == null) {
            Toast.makeText(context, "Please log in to favorite events.", Toast.LENGTH_SHORT).show()
            return
        }

        val position = eventsAdapter.events.indexOf(event)
        Log.d("SearchFragment", "Toggling favorite for event: ${event.id}")

        lifecycleScope.launch {
            mutex.withLock {
                handleFavoriteToggle(event, eventsAdapter, position)
            }
        }
    }

    // Funkcja obsługująca przełączanie stanu ulubionego wydarzenia
    private suspend fun handleFavoriteToggle(event: Event, adapter: EventsAdapter, position: Int) {
        try {
            // Sprawdzenie stanu ulubionych w bazie danych
            Log.i("SearchFragment", "Checking current favorite state in the database for event: ${event.id}")
            val isFavoriteNow = withContext(Dispatchers.IO) {
                currentUser?.let { FavoritesRepository.isEventFavorite(event.id.toString(), it.uid) }
            } ?: false

            // Synchronizacja UI jeśli stan się różni
            if (isFavoriteNow != event.isFavorite) {
                Log.i("SearchFragment", "Mismatch between UI and database state. Syncing UI with database.")
                event.isFavorite = isFavoriteNow
                adapter.notifyItemChanged(position)
                return
            }

            // Wykonanie operacji na ulubionych
            if (isFavoriteNow) {
                withContext(Dispatchers.IO) {
                    currentUser?.let { FavoritesRepository.removeEventFromFavorites(event.id.toString(), it.uid) }
                }
                Log.i("SearchFragment", "Removed event: ${event.id} from favorites")
                event.isFavorite = false
            } else {
                withContext(Dispatchers.IO) {
                    currentUser?.let { FavoritesRepository.addEventToFavorites(event.id.toString(), it.uid) }
                }
                Log.i("SearchFragment", "Added event: ${event.id} to favorites")
                event.isFavorite = true
            }

            adapter.notifyItemChanged(position)
        } catch (e: Exception) {
            Log.e("SearchFragment", "Failed to toggle favorite: ${e.message}", e)
        }
    }

    // Funkcja nawigująca do szczegółów wydarzenia
    private fun navigateToEventDetails(event: Event) {
        Log.d("SearchFragment", "Navigating to event details for event: ${event.id}")
        val intent = Intent(context, EventDetailsActivity::class.java)
        val eventJson = Gson().toJson(event)
        intent.putExtra("event", eventJson)
        startActivity(intent)
    }

    // Funkcja nawigująca do edycji wydarzenia
    private fun editEvent(event: Event) {
        val intent = Intent(context, ManageEventActivity::class.java)
        intent.putExtra("EVENT_ID", event.id)
        startActivity(intent)
    }

    // Funkcja usuwająca wydarzenie
    private fun deleteEvent(event: Event) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) { EventUtils.deleteEvent(event.id.toString()) }
                sendRefreshBroadcast()
                eventsAdapter.notifyDataSetChanged()
                Log.d("SearchFragment", "Deleted event: ${event.id}")
            } catch (e: Exception) {
                Log.e("SearchFragment", "Failed to delete event: ${e.message}", e)
            }
        }
    }

    // Funkcja rejestrująca odbiornik broadcastu
    private fun registerRefreshReceiver() {
        val filter = IntentFilter("com.example.eventup.REFRESH_EVENTS")
        context?.registerReceiver(refreshReceiver, filter)
    }

    // Odbiornik broadcastu do odświeżania listy wydarzeń
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("SearchFragment", "Received broadcast to refresh events")
            fetchAllEvents()
        }
    }

    // Funkcja wysyłająca broadcast do odświeżenia listy wydarzeń
    private fun sendRefreshBroadcast() {
        val intent = Intent("com.example.eventup.REFRESH_EVENTS")
        context?.sendBroadcast(intent)
    }

    // Funkcja synchronizująca wydarzenia z ulubionymi użytkownika
    private fun syncEvents(events: List<Event>, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                userId = UserUtils.getCurrentUserId()?.toString()
                val favoriteEventIds = withContext(Dispatchers.IO) {
                    if (userId != null) {
                        FavoritesRepository.getUserEventIds(userId!!)
                    } else {
                        emptyList()
                    }
                }
                events.forEach { event ->
                    event.isFavorite = favoriteEventIds.contains(event.id.toString())
                }
                eventsAdapter.submitList(events)
                onComplete()
            } catch (e: Exception) {
                Log.e("SearchFragment", "Failed to sync events: ${e.message}", e)
            }
        }
    }
}
