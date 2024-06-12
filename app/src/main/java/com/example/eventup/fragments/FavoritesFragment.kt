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
import com.example.eventup.utils.EventRepository
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private lateinit var eventAdapter: EventsAdapter
    private var favoriteEvents = mutableListOf<Event>()
    private var userId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val recyclerView = binding.recyclerViewFavorites
        val loginPrompt: TextView = binding.loginPrompt

        userId = UserUtils.getCurrentUserId().toString()

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

    override fun onDestroyView() {
        super.onDestroyView()
        context?.unregisterReceiver(refreshReceiver)
    }

    private fun loadFavoriteEvents() {
        userId?.let { id ->
            lifecycleScope.launch {
                try {
                    val events = FavoritesRepository.getFavorites(id)
                    favoriteEvents.clear()
                    favoriteEvents.addAll(events)
                    eventAdapter.submitList(favoriteEvents)
                } catch (e: Exception) {
                    println("Failed to load favorite events: ${e.message}")
                }
            }
        }
    }

    private fun toggleFavorite(event: Event) {
        val position = eventAdapter.events.indexOf(event)
        println("Toggling favorite for event: ${event.id} (isFavorite: ${event.isFavorite})")
        val isCurrentlyFavorite = event.isFavorite
        event.isFavorite = !isCurrentlyFavorite

        lifecycleScope.launch {
            try {
                if (isCurrentlyFavorite) {
                    FavoritesRepository.removeEventFromFavorites(event, userId!!)
                } else {
                    FavoritesRepository.addEventToFavorites(event, userId!!)
                }
                eventAdapter.notifyItemChanged(position)
            } catch (e: Exception) {
                println("Failed to toggle favorite: ${e.message}")
                // Revert the change if the operation fails
                event.isFavorite = isCurrentlyFavorite
                eventAdapter.notifyItemChanged(position)
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
                EventRepository.deleteEvent(event.id)
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
