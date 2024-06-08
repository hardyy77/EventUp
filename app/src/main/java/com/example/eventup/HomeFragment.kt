package com.example.eventup

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var interestingEventsAdapter: EventsAdapter
    private lateinit var popularEventsAdapter: EventsAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val TAG = "HomeFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        firestore = FirebaseFirestore.getInstance()

        // Najciekawsze
        val interestingRecyclerView = view.findViewById<RecyclerView>(R.id.interesting_recycler_view)
        interestingRecyclerView.layoutManager = GridLayoutManager(context, 2)
        interestingEventsAdapter = EventsAdapter()
        interestingRecyclerView.adapter = interestingEventsAdapter

        // Popularne
        val popularRecyclerView = view.findViewById<RecyclerView>(R.id.popular_recycler_view)
        popularRecyclerView.layoutManager = GridLayoutManager(context, 2)
        popularEventsAdapter = EventsAdapter()
        popularRecyclerView.adapter = popularEventsAdapter

        getInterestingEvents()
        getPopularEvents()

        return view
    }

    private fun getInterestingEvents() {
        val sharedPreferences = requireContext().getSharedPreferences("EventUpPreferences", Context.MODE_PRIVATE)
        val lastUpdateTime = sharedPreferences.getLong("lastUpdateTime", 0)
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000

        if (currentTime - lastUpdateTime >= oneDayMillis) {
            updateInterestingEvents()
        } else {
            val gson = Gson()
            val json = sharedPreferences.getString("selectedInterestingEvents", "")
            val type = object : TypeToken<List<Event>>() {}.type
            val selectedEvents: List<Event> = gson.fromJson(json, type)
            interestingEventsAdapter.submitList(selectedEvents)
            Log.d(TAG, "Loaded interesting events from preferences: $selectedEvents")
        }
    }

    private fun updateInterestingEvents() {
        val currentDate = dateFormat.format(Date())
        firestore.collection("events")
            .whereGreaterThanOrEqualTo("date", currentDate)
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: "",
                        document.getString("genres") ?: "",
                        document.getString("description") ?: "",
                        document.getLong("interest")?.toInt() ?: 0
                    )
                }
                val selectedEvents = events.shuffled().take(4)
                interestingEventsAdapter.submitList(selectedEvents)
                saveSelectedEventsToPreferences(selectedEvents)
                Log.d(TAG, "Updated interesting events: $selectedEvents")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting interesting events: $exception")
            }
    }

    private fun saveSelectedEventsToPreferences(events: List<Event>) {
        if (isAdded) {
            val sharedPreferences = requireContext().getSharedPreferences("EventUpPreferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val gson = Gson()
            val json = gson.toJson(events)
            editor.putString("selectedInterestingEvents", json)
            editor.putLong("lastUpdateTime", System.currentTimeMillis())
            editor.apply()
        } else {
            Log.e(TAG, "Fragment not attached to context, cannot save selected events to preferences.")
        }
    }

    private fun getPopularEvents() {
        val currentDate = dateFormat.format(Date())
        firestore.collection("events")
            .whereGreaterThanOrEqualTo("date", currentDate)
            .orderBy("date") // Dodane jako pierwsze sortowanie
            .orderBy("interest", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(4)
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: "",
                        document.getString("genres") ?: "",
                        document.getString("description") ?: "",
                        document.getLong("interest")?.toInt() ?: 0
                    )
                }
                popularEventsAdapter.submitList(events)
                Log.d(TAG, "Loaded popular events: $events")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting popular events: $exception")
            }
    }
}
