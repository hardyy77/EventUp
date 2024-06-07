package com.example.eventup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var eventsAdapter: EventsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        firestore = FirebaseFirestore.getInstance()

        val recyclerView = view.findViewById<RecyclerView>(R.id.events_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        eventsAdapter = EventsAdapter()
        recyclerView.adapter = eventsAdapter

        getEvents()

        return view
    }

    private fun getEvents() {
        firestore.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        document.getString("name") ?: "",
                        document.getString("location") ?: "",
                        document.getString("date") ?: ""
                    )
                }
                eventsAdapter.submitList(events)
            }
            .addOnFailureListener { exception ->
                println("Error getting events: $exception")
            }
    }
}
