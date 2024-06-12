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
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventup.models.Event
import com.example.eventup.utils.EventUtils
import com.example.eventup.utils.DatabaseHandler
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.activities.ManageEventActivity
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentSearchBinding
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var allEvents: List<Event>
    private var userId: String = "currentUserId" // Replace with actual user ID management

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.eventsRecyclerView.layoutManager = LinearLayoutManager(context)
        eventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.eventsRecyclerView.adapter = eventsAdapter

        setupSearchView(binding.searchView)

        fetchAllEvents()

        registerRefreshReceiver()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        context?.unregisterReceiver(refreshReceiver)
    }

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

    private fun filterEvents(query: String) {
        val filteredEvents = EventUtils.filterEvents(allEvents, query)
        println("Filtering events with query: $query, found ${filteredEvents.size} events")
        eventsAdapter.submitList(filteredEvents)
    }

    private fun fetchAllEvents() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val events = withContext(Dispatchers.IO) { EventUtils.getAllEvents() }
                allEvents = events
                syncFavorites(allEvents, {
                    eventsAdapter.submitList(allEvents)
                }, {
                    // Handle error
                })
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun toggleFavorite(event: Event) {
        CoroutineScope(Dispatchers.Main).launch {
            val position = eventsAdapter.events.indexOf(event)
            println("Toggling favorite for event: ${event.id} (isFavorite: ${event.isFavorite})")
            try {
                if (event.isFavorite) {
                    withContext(Dispatchers.IO) { EventUtils.removeEventFromFavorites(event, userId) }
                    event.isFavorite = false
                } else {
                    withContext(Dispatchers.IO) { EventUtils.addEventToFavorites(event, userId) }
                    event.isFavorite = true
                }
                eventsAdapter.notifyItemChanged(position)
                println("Updated favorite status for event: ${event.id}")
            } catch (e: Exception) {
                println("Failed to update favorite status: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun navigateToEventDetails(event: Event) {
        println("Navigating to event details for event: ${event.id}")
        val intent = Intent(context, EventDetailsActivity::class.java)
        val eventJson = Gson().toJson(event)
        intent.putExtra("event", eventJson)
        startActivity(intent)
    }

    private fun editEvent(event: Event) {
        val intent = Intent(context, ManageEventActivity::class.java)
        intent.putExtra("EVENT_ID", event.id)
        startActivity(intent)
    }

    private fun deleteEvent(event: Event) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) { EventUtils.deleteEvent(event.id) }
                sendRefreshBroadcast()
                eventsAdapter.notifyDataSetChanged()
                println("Deleted event: ${event.id}")
            } catch (e: Exception) {
                println("Failed to delete event: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun registerRefreshReceiver() {
        val filter = IntentFilter("com.example.eventup.REFRESH_EVENTS")
        context?.registerReceiver(refreshReceiver, filter)
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("SearchFragment", "Received broadcast to refresh events")
            fetchAllEvents()
        }
    }

    private fun sendRefreshBroadcast() {
        val intent = Intent("com.example.eventup.REFRESH_EVENTS")
        context?.sendBroadcast(intent)
    }

    private fun syncFavorites(events: List<Event>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val query = "SELECT event_id FROM favorites WHERE user_id = '$userId'"
                val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
                val favoriteEventIds = mutableListOf<String>()
                while (resultSet?.next() == true) {
                    favoriteEventIds.add(resultSet.getString("event_id"))
                }
                events.forEach { it.isFavorite = favoriteEventIds.contains(it.id) }
                onSuccess()
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }
}
