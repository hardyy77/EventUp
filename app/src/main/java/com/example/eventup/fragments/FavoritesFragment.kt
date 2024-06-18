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
import android.widget.Toast
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

    private lateinit var eventAdapter: EventsAdapter
    private var favoriteEvents = mutableListOf<Event>()
    private var userId: String? = null
    private val mutex = Mutex()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val recyclerView = binding.recyclerViewFavorites
        val loginPrompt: TextView = binding.loginPrompt

        userId = UserUtils.getCurrentUserId()?.toString()

        if (userId == null) {
            recyclerView.visibility = View.GONE
            loginPrompt.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            loginPrompt.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(context)
            eventAdapter = EventsAdapter(
                onClick = { event -> navigateToEventDetails(event) },
                onFavoriteClick = { event -> toggleFavorite(event) },
                onEditClick = { event -> editEvent(event) },
                onDeleteClick = { event -> deleteEvent(event) }
            )
            recyclerView.adapter = eventAdapter
            loadFavoriteEvents()
        }
        registerRefreshReceiver()
        return binding.root
    }

    private fun loadFavoriteEvents() {
        lifecycleScope.launch {
            favoriteEvents.clear()
            userId?.let { id ->
                favoriteEvents.addAll(FavoritesRepository.getFavorites(id))
            }
            eventAdapter.submitList(favoriteEvents.toList())
        }
    }

    private fun toggleFavorite(event: Event) {
        val isCurrentlyFavorite = favoriteEvents.contains(event)
        val position = favoriteEvents.indexOf(event)

        lifecycleScope.launch {
            mutex.withLock {
                try {
                    // Dodanie dokładniejszych logów
                    Log.i("FavoritesFragment", "Checking current favorite state in the database for event: ${event.id}")
                    val isFavoriteNow = FavoritesRepository.isEventFavorite(event.id, userId!!)
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
                        FavoritesRepository.removeEventFromFavorites(event.id, userId!!)
                        favoriteEvents.remove(event)
                    } else {
                        Log.i("FavoritesFragment", "Adding event: ${event.id} to favorites")
                        FavoritesRepository.addEventToFavorites(event.id, userId!!)
                        favoriteEvents.add(event)
                    }
                    eventAdapter.notifyItemChanged(position)
                } catch (e: Exception) {
                    Log.e("FavoritesFragment", "Failed to toggle favorite: ${e.message}", e)
                    // Revert the change if the operation fails
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

    private fun navigateToEventDetails(event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java)
        intent.putExtra("event", event)
        startActivity(intent)
    }

    private fun editEvent(event: Event) {
        val intent = Intent(context, ManageEventActivity::class.java)
        intent.putExtra("EVENT_ID", event.id)
        startActivity(intent)
    }

    private fun deleteEvent(event: Event) {
        lifecycleScope.launch {
            try {
                EventUtils.deleteEvent(event.id)
                loadFavoriteEvents()
                eventAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                println("Failed to delete event: ${e.message}")
            }
        }
    }

    private fun registerRefreshReceiver() {
        val filter = IntentFilter("com.example.eventup.REFRESH_EVENTS")
        context?.registerReceiver(refreshReceiver, filter)
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("FavoritesFragment", "Received broadcast to refresh events")
            loadFavoriteEvents()
        }
    }
}