package com.example.eventup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eventup.databinding.FragmentHomeBinding
import com.google.gson.Gson

class HomeFragment : Fragment() {

    private lateinit var interestingEventsAdapter: EventsAdapter
    private lateinit var popularEventsAdapter: EventsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Najciekawsze
        binding.interestingRecyclerView.layoutManager = GridLayoutManager(context, 2)
        interestingEventsAdapter = EventsAdapter { event -> navigateToEventDetails(event) }
        binding.interestingRecyclerView.adapter = interestingEventsAdapter

        // Popularne
        binding.popularRecyclerView.layoutManager = GridLayoutManager(context, 2)
        popularEventsAdapter = EventsAdapter { event -> navigateToEventDetails(event) }
        binding.popularRecyclerView.adapter = popularEventsAdapter

        EventUtils.getInterestingEvents(requireContext()) { events ->
            interestingEventsAdapter.submitList(events)
        }

        EventUtils.getPopularEvents { events ->
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

}
