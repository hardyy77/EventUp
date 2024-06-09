package com.example.eventup.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eventup.models.Event
import com.example.eventup.utils.EventUtils
import com.example.eventup.utils.FirestoreUtils
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentHomeBinding
import com.google.gson.Gson

class HomeFragment : Fragment() {

    private lateinit var interestingEventsAdapter: EventsAdapter
    private lateinit var popularEventsAdapter: EventsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Najciekawsze
        binding.interestingRecyclerView.layoutManager = GridLayoutManager(context, 2)
        interestingEventsAdapter = EventsAdapter({ event -> navigateToEventDetails(event) }, { event -> toggleFavorite(event) })
        binding.interestingRecyclerView.adapter = interestingEventsAdapter

        // Popularne
        binding.popularRecyclerView.layoutManager = GridLayoutManager(context, 2)
        popularEventsAdapter = EventsAdapter({ event -> navigateToEventDetails(event) }, { event -> toggleFavorite(event) })
        binding.popularRecyclerView.adapter = popularEventsAdapter

        EventUtils.getInterestingEvents(requireContext()) { events ->
            events.forEach { event -> println("Interesting event ID: ${event.id}") }
            interestingEventsAdapter.submitList(events)
        }

        EventUtils.getPopularEvents { events ->
            events.forEach { event -> println("Popular event ID: ${event.id}") }
            popularEventsAdapter.submitList(events)
        }

        return binding.root
    }

    private fun navigateToEventDetails(event: Event) {
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
                updateUI()
            }, { e ->
                println("Failed to remove event from favorites: ${e.message}")
                e.printStackTrace()
            })
        } else {
            FirestoreUtils.addEventToFavorites(event, {
                event.isFavorite = true
                println("Added event to favorites: ${event.id}")
                updateUI()
            }, { e ->
                println("Failed to add event to favorites: ${e.message}")
                e.printStackTrace()
            })
        }
    }

    private fun updateUI() {
        interestingEventsAdapter.notifyDataSetChanged()
        popularEventsAdapter.notifyDataSetChanged()
    }
}
