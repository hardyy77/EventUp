package com.example.eventup.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventup.models.Event
import com.example.eventup.utils.FirestoreUtils
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentFavoritesBinding
import com.example.eventup.activities.EventDetailsActivity
import com.google.firebase.auth.FirebaseAuth

class FavoritesFragment : Fragment() {

    private lateinit var eventAdapter: EventsAdapter
    private var favoriteEvents = mutableListOf<Event>()
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val recyclerView = binding.recyclerViewFavorites
        val loginPrompt: TextView = binding.loginPrompt

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            recyclerView.visibility = View.GONE
            loginPrompt.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            loginPrompt.visibility = View.GONE
            recyclerView.layoutManager = LinearLayoutManager(context)
            eventAdapter = EventsAdapter({ event ->
                navigateToEventDetails(event)
            }, { event ->
                toggleFavorite(event)
            })
            recyclerView.adapter = eventAdapter
            loadFavoriteEvents()
        }

        return binding.root
    }

    private fun loadFavoriteEvents() {
        FirestoreUtils.getFavorites({ events ->
            favoriteEvents.clear()
            favoriteEvents.addAll(events)
            eventAdapter.submitList(favoriteEvents)
        }, {
            // Obsłuż błąd
        })
    }

    private fun toggleFavorite(event: Event) {
        println("Toggling favorite for event: ${event.id} (isFavorite: ${event.isFavorite})")
        val isCurrentlyFavorite = event.isFavorite
        event.isFavorite = !isCurrentlyFavorite

        if (isCurrentlyFavorite) {
            FirestoreUtils.removeEventFromFavorites(event, {
                println("Successfully removed event from favorites: ${event.id}")
                eventAdapter.notifyDataSetChanged()
            }, { e ->
                println("Failed to remove event from favorites: ${e.message}")
                // Revert the change if the operation fails
                event.isFavorite = isCurrentlyFavorite
                eventAdapter.notifyDataSetChanged()
            })
        } else {
            FirestoreUtils.addEventToFavorites(event, {
                println("Successfully added event to favorites: ${event.id}")
                eventAdapter.notifyDataSetChanged()
            }, { e ->
                println("Failed to add event to favorites: ${e.message}")
                // Revert the change if the operation fails
                event.isFavorite = isCurrentlyFavorite
                eventAdapter.notifyDataSetChanged()
            })
        }
    }

    private fun navigateToEventDetails(event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java)
        intent.putExtra("event", event)
        startActivity(intent)
    }
}
