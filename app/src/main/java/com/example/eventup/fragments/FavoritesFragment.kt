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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventup.models.Event
import com.example.eventup.utils.FavoritesRepository
import com.example.eventup.utils.UserUtils
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentFavoritesBinding
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.activities.ManageEventActivity
import com.example.eventup.utils.EventUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FavoritesFragment : Fragment() {

    // Deklaracja zmiennych globalnych dla fragmentu
    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var eventAdapter: EventsAdapter
    private var favoriteEvents = mutableListOf<Event>()
    private var userId: String? = null
    private val mutex = Mutex() // Mutex używany do synchronizacji operacji na ulubionych wydarzeniach
    private var isReceiverRegistered = false // Flaga do śledzenia, czy odbiornik jest zarejestrowany

    // Funkcja tworząca widok fragmentu
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inicjalizacja widoku za pomocą View Binding
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val recyclerView = binding.recyclerViewFavorites
        val loginPrompt: TextView = binding.loginPrompt

        // Pobieranie ID aktualnie zalogowanego użytkownika
        userId = UserUtils.getCurrentUserId()?.toString()

        // Sprawdzanie, czy użytkownik jest zalogowany
        if (userId == null) {
            recyclerView.visibility = View.GONE // Ukrywanie listy ulubionych wydarzeń
            loginPrompt.visibility = View.VISIBLE // Pokazywanie informacji o konieczności zalogowania się
        } else {
            recyclerView.visibility = View.VISIBLE
            loginPrompt.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(context) // Ustawianie menedżera layoutu dla RecyclerView
            // Inicjalizacja adaptera z ustawieniem akcji na kliknięcia
            eventAdapter = EventsAdapter(
                onClick = { event -> navigateToEventDetails(event) },
                onFavoriteClick = { event -> toggleFavorite(event) },
                onEditClick = { event -> editEvent(event) },
                onDeleteClick = { event -> deleteEvent(event) }
            )
            recyclerView.adapter = eventAdapter // Ustawienie adaptera do RecyclerView
            loadFavoriteEvents() // Załadowanie ulubionych wydarzeń
        }
        registerRefreshReceiver() // Rejestracja odbiornika do odświeżania listy wydarzeń
        return binding.root
    }

    // Funkcja czyszcząca zasoby przy niszczeniu widoku
    override fun onDestroyView() {
        super.onDestroyView()
        unregisterRefreshReceiver() // Wyrejestrowanie odbiornika
    }

    // Funkcja ładująca ulubione wydarzenia
    private fun loadFavoriteEvents() {
        lifecycleScope.launch {
            favoriteEvents.clear() // Czyszczenie listy ulubionych wydarzeń
            userId?.let { id ->
                favoriteEvents.addAll(FavoritesRepository.getFavorites(id)) // Pobieranie ulubionych wydarzeń z repozytorium
            }
            eventAdapter.submitList(favoriteEvents.toList()) // Aktualizacja listy w adapterze
        }
    }

    // Funkcja przełączająca stan ulubionego wydarzenia
    private fun toggleFavorite(event: Event) {
        val isCurrentlyFavorite = favoriteEvents.contains(event) // Sprawdzanie, czy wydarzenie jest obecnie ulubione
        val position = favoriteEvents.indexOf(event) // Pobieranie pozycji wydarzenia na liście

        lifecycleScope.launch {
            mutex.withLock {
                try {
                    Log.i("FavoritesFragment", "Checking current favorite state in the database for event: ${event.id}")
                    val isFavoriteNow = FavoritesRepository.isEventFavorite(event.id.toString(), userId!!)
                    Log.i("FavoritesFragment", "Current favorite state in database: $isFavoriteNow, in UI: $isCurrentlyFavorite")

                    if (isCurrentlyFavorite != isFavoriteNow) {
                        Log.i("FavoritesFragment", "Mismatch between UI and database state. Syncing UI with database.")
                        if (isFavoriteNow) {
                            favoriteEvents.add(event)
                        } else {
                            favoriteEvents.remove(event)
                        }
                        eventAdapter.notifyItemChanged(position)
                        return@withLock
                    }

                    if (isCurrentlyFavorite) {
                        Log.i("FavoritesFragment", "Removing event: ${event.id} from favorites")
                        FavoritesRepository.removeEventFromFavorites(event.id.toString(), userId!!)
                        favoriteEvents.remove(event)
                    } else {
                        Log.i("FavoritesFragment", "Adding event: ${event.id} to favorites")
                        FavoritesRepository.addEventToFavorites(event.id.toString(), userId!!)
                        favoriteEvents.add(event)
                    }
                    eventAdapter.notifyItemChanged(position)
                } catch (e: Exception) {
                    Log.e("FavoritesFragment", "Failed to toggle favorite: ${e.message}", e)
                    if (isCurrentlyFavorite) {
                        favoriteEvents.add(event)
                    } else {
                        favoriteEvents.remove(event)
                    }
                    eventAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    // Funkcja nawigująca do szczegółów wydarzenia
    private fun navigateToEventDetails(event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java)
        intent.putExtra("event", event)
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
        lifecycleScope.launch {
            try {
                EventUtils.deleteEvent(event.id.toString()) // Usuwanie wydarzenia za pomocą narzędzia EventUtils
                loadFavoriteEvents() // Odświeżenie listy ulubionych wydarzeń
                eventAdapter.notifyDataSetChanged() // Powiadomienie adaptera o zmianach
                context?.sendBroadcast(Intent("com.example.eventup.REFRESH_EVENTS")) // Wysłanie broadcastu do odświeżenia listy wydarzeń
            } catch (e: Exception) {
                Log.e("FavoritesFragment", "Failed to delete event: ${e.message}", e)
            }
        }
    }

    // Funkcja rejestrująca odbiornik do odświeżania listy wydarzeń
    private fun registerRefreshReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter("com.example.eventup.REFRESH_EVENTS")
            context?.registerReceiver(refreshReceiver, filter)
            isReceiverRegistered = true
        }
    }

    // Funkcja wyrejestrowująca odbiornik
    private fun unregisterRefreshReceiver() {
        if (isReceiverRegistered) {
            context?.unregisterReceiver(refreshReceiver)
            isReceiverRegistered = false
        }
    }

    // Odbiornik do odświeżania listy wydarzeń
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("FavoritesFragment", "Received broadcast to refresh events")
            loadFavoriteEvents()
        }
    }
}
