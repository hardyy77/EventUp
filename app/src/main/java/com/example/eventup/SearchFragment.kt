package com.example.eventup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventup.databinding.FragmentSearchBinding
import com.google.gson.Gson

class SearchFragment : Fragment() {

    private lateinit var eventsAdapter: EventsAdapter
    private lateinit var allEvents: List<Event>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.eventsRecyclerView.layoutManager = LinearLayoutManager(context)
        eventsAdapter = EventsAdapter { event -> navigateToEventDetails(event) }
        binding.eventsRecyclerView.adapter = eventsAdapter

        setupSearchView(binding.searchView)

        EventUtils.getAllEvents { events ->
            allEvents = events
            eventsAdapter.submitList(allEvents)
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
        eventsAdapter.submitList(filteredEvents)
    }

    private fun navigateToEventDetails(event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java)
        val eventJson = Gson().toJson(event)
        intent.putExtra("event", eventJson)
        startActivity(intent)
    }

}
