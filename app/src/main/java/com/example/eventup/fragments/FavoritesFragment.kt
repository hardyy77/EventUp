package com.example.eventup.fragments

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
                // Obsługa kliknięcia na wydarzenie
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
        if (event.isFavorite) {
            FirestoreUtils.removeEventFromFavorites(event, {
                // Success handler
                event.isFavorite = false
                eventAdapter.notifyDataSetChanged()
            }, { e ->
                // Failure handler
            })
        } else {
            FirestoreUtils.addEventToFavorites(event, {
                // Success handler
                event.isFavorite = true
                eventAdapter.notifyDataSetChanged()
            }, { e ->
                // Failure handler
            })
        }
    }
}
