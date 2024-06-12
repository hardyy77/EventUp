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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.eventup.R
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.activities.ManageEventActivity
import com.example.eventup.adapters.EventsAdapter
import com.example.eventup.databinding.FragmentHomeBinding
import com.example.eventup.models.Event
import com.example.eventup.utils.DatabaseHandler
import com.example.eventup.utils.EventUtils
import com.example.eventup.utils.FavoritesRepository
import com.example.eventup.utils.UserUtils
import com.example.eventup.workers.UpdateInterestingEventsWorker
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var interestingEventsAdapter: EventsAdapter
    private lateinit var popularEventsAdapter: EventsAdapter
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isReceiverRegistered = false
    private val currentUser = UserUtils.getCurrentUser()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerViews()

        lifecycleScope.launch {
            fetchInterestingEvents()
            fetchPopularEvents()
        }

        registerRefreshReceiver()

        scheduleWork()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unregisterRefreshReceiver()
        _binding = null
    }

    private fun setupRecyclerViews() {
        // Najciekawsze
        binding.interestingRecyclerView.layoutManager = LinearLayoutManager(context)
        interestingEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event, true) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.interestingRecyclerView.adapter = interestingEventsAdapter

        // Popularne
        binding.popularRecyclerView.layoutManager = LinearLayoutManager(context)
        popularEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event, false) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.popularRecyclerView.adapter = popularEventsAdapter
    }

    private suspend fun fetchInterestingEvents() {
        val query = "SELECT * FROM todayinterestingevents"
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    val event = Event(
                        id = resultSet.getString("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest"),
                        isFavorite = resultSet.getBoolean("isFavorite")
                    )
                    events.add(event)
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                try {
                    resultSet.close()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        } else {
            Log.e("HomeFragment", "Failed to fetch interesting events: resultSet is null")
        }

        interestingEventsAdapter.submitList(events)
    }

    private suspend fun fetchPopularEvents() {
        val query = "SELECT * FROM events ORDER BY interest DESC LIMIT 4"
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    val event = Event(
                        id = resultSet.getString("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest"),
                        isFavorite = resultSet.getBoolean("isFavorite")
                    )
                    events.add(event)
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                try {
                    resultSet.close()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        } else {
            Log.e("HomeFragment", "Failed to fetch popular events: resultSet is null")
        }

        popularEventsAdapter.submitList(events)
    }

    private fun navigateToEventDetails(event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java)
        val eventJson = Gson().toJson(event)
        intent.putExtra("event", eventJson)
        startActivity(intent)
    }

    private fun toggleFavorite(event: Event, isInteresting: Boolean) {
        val adapter = if (isInteresting) interestingEventsAdapter else popularEventsAdapter
        val position = adapter.events.indexOf(event)
        println("Toggling favorite for event: ${event.id} (isFavorite: ${event.isFavorite})")
        if (event.isFavorite) {
            lifecycleScope.launch {
                try {
                    FavoritesRepository.removeEventFromFavorites(event,
                        currentUser!!.id!!.toString()
                    )
                    event.isFavorite = false
                    adapter.notifyItemChanged(position)
                    println("Removed event from favorites: ${event.id}")
                } catch (e: Exception) {
                    println("Failed to remove event from favorites: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            lifecycleScope.launch {
                try {
                    FavoritesRepository.addEventToFavorites(event, currentUser!!.id!!.toString())
                    event.isFavorite = true
                    adapter.notifyItemChanged(position)
                    println("Added event to favorites: ${event.id}")
                } catch (e: Exception) {
                    println("Failed to add event to favorites: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun editEvent(event: Event) {
        val intent = Intent(context, ManageEventActivity::class.java)
        intent.putExtra("EVENT_ID", event.id)
        startActivity(intent)
    }

    private fun deleteEvent(event: Event) {
        lifecycleScope.launch {
            try {
                EventUtils.deleteEvent(event.id)
                updateUI()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to delete event: ${e.message}")
            }
        }
    }

    private fun updateUI() {
        lifecycleScope.launch {
            fetchPopularEvents()
            fetchInterestingEvents()
        }
    }

    private fun registerRefreshReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter("com.example.eventup.REFRESH_EVENTS")
            context?.registerReceiver(refreshReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterRefreshReceiver() {
        if (isReceiverRegistered) {
            context?.unregisterReceiver(refreshReceiver)
            isReceiverRegistered = false
        }
    }

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("HomeFragment", "Received broadcast to refresh events")
            updateUI()
        }
    }

    private fun scheduleWork() {
        val workManager = WorkManager.getInstance(requireContext())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "UpdateInterestingEventsWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
