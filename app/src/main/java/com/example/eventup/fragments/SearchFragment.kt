package com.example.eventup.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventup.models.Event
import com.example.eventup.utils.EventUtils
import com.example.eventup.utils.FirestoreUtils
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentSearchBinding
import com.google.gson.Gson

class SearchFragment : Fragment() {

    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var allEvents: List<Event>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.eventsRecyclerView.layoutManager = LinearLayoutManager(context)
        eventsAdapter = EventsAdapter({ event ->
            navigateToEventDetails(event)
        }, { event ->
            toggleFavorite(event)
        })
        binding.eventsRecyclerView.adapter = eventsAdapter

        setupSearchView(binding.searchView)

        EventUtils.getAllEvents { events ->
            allEvents = events
            FirestoreUtils.syncFavorites(allEvents, {
                eventsAdapter.submitList(allEvents)
            }, {
                // Obsłuż błąd
            })
        }

        return binding.root
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

    private fun navigateToEventDetails(event: Event) {
        println("Navigating to event details for event: ${event.id}")
        val intent = Intent(context, EventDetailsActivity::class.java)
        val eventJson = Gson().toJson(event)
        intent.putExtra("event", eventJson)
        startActivity(intent)
    }

    private fun toggleFavorite(event: Event) {
        println("Toggling favorite for event: ${event.id} (isFavorite: ${event.isFavorite})")
        if (event.isFavorite) {
            FirestoreUtils.removeEventFromFavorites(event, {
                event.isFavorite = false
                println("Removed event from favorites: ${event.id}")
                eventsAdapter.notifyDataSetChanged()
            }, { e ->
                println("Failed to remove event from favorites: ${e.message}")
                e.printStackTrace()
            })
        } else {
            FirestoreUtils.addEventToFavorites(event, {
                event.isFavorite = true
                println("Added event to favorites: ${event.id}")
                eventsAdapter.notifyDataSetChanged()
            }, { e ->
                println("Failed to add event to favorites: ${e.message}")
                e.printStackTrace()
            })
        }
    }
}
