package com.example.eventup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var allEvents: List<Event>  // List of all events
    private lateinit var searchView: SearchView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        firestore = FirebaseFirestore.getInstance()

        val recyclerView = view.findViewById<RecyclerView>(R.id.events_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        eventsAdapter = EventsAdapter()
        recyclerView.adapter = eventsAdapter

        searchView = view.findViewById(R.id.search_view)
        setupSearchView()

        getEvents()

        return view
    }

    private fun getEvents() {
        firestore.collection("events")
            .get()
            .addOnSuccessListener { result ->
                allEvents = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: "",
                        document.getString("genres") ?: ""
                    )
                }
                eventsAdapter.submitList(allEvents)
            }
            .addOnFailureListener { exception ->
                println("Error getting events: $exception")
            }
    }

    private fun setupSearchView() {
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
        val filteredEvents = allEvents.filter { event ->
            event.name.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true) ||
                    event.date.contains(query, ignoreCase = true) ||
                    event.genres.contains(query, ignoreCase = true)
        }
        eventsAdapter.submitList(filteredEvents)
    }
}
