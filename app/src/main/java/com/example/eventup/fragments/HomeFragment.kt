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
import androidx.work.*
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.sql.SQLException
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var interestingEventsAdapter: EventsAdapter
    private lateinit var popularEventsAdapter: EventsAdapter
    private lateinit var newestEventsAdapter: EventsAdapter
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var isReceiverRegistered = false
    private val currentUser = UserUtils.getCurrentUser()
    private val mutex = Mutex()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerViews()

        lifecycleScope.launch {
            fetchInterestingEvents()
            fetchPopularEvents()
            fetchNewestEvents()
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
        binding.interestingRecyclerView.layoutManager = LinearLayoutManager(context)
        interestingEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event, true) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.interestingRecyclerView.adapter = interestingEventsAdapter

        binding.popularRecyclerView.layoutManager = LinearLayoutManager(context)
        popularEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event, false) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.popularRecyclerView.adapter = popularEventsAdapter

        binding.newestRecyclerView.layoutManager = LinearLayoutManager(context)
        newestEventsAdapter = EventsAdapter(
            onClick = { event -> navigateToEventDetails(event) },
            onFavoriteClick = { event -> toggleFavorite(event, false) },
            onEditClick = { event -> editEvent(event) },
            onDeleteClick = { event -> deleteEvent(event) }
        )
        binding.newestRecyclerView.adapter = newestEventsAdapter
    }

    private suspend fun fetchInterestingEvents() {
        val query = "SELECT * FROM todayinterestingevents"
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    val event = Event(
                        id = resultSet.getInt("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest")
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

        syncEvents(events, interestingEventsAdapter)
    }

    private suspend fun fetchPopularEvents() {
        val query = "SELECT * FROM events ORDER BY interest DESC LIMIT 4"
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    val event = Event(
                        id = resultSet.getInt("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest")
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

        syncEvents(events, popularEventsAdapter)
    }

    private suspend fun fetchNewestEvents() {
        val query = "SELECT * FROM events ORDER BY added_date DESC LIMIT 4"
        val resultSet = withContext(Dispatchers.IO) { DatabaseHandler.executeQuery(query) }
        val events = mutableListOf<Event>()

        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    val event = Event(
                        id = resultSet.getInt("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest"),
                        addedDate = resultSet.getString("added_date")
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
            Log.e("HomeFragment", "Failed to fetch newest events: resultSet is null")
        }

        syncEvents(events, newestEventsAdapter)
    }

    private fun navigateToEventDetails(event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java)
        val eventJson = Gson().toJson(event)
        intent.putExtra("event", eventJson)
        startActivity(intent)
    }

    private fun toggleFavorite(event: Event, isInteresting: Boolean) {
        if (currentUser == null) {
            Toast.makeText(context, "Please log in to favorite events.", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = if (isInteresting) interestingEventsAdapter else popularEventsAdapter
        val position = adapter.events.indexOf(event)
        println("Toggling favorite for event: ${event.id}")

        lifecycleScope.launch {
            mutex.withLock {
                handleFavoriteToggle(event, adapter, position)
            }
        }
    }

    private suspend fun handleFavoriteToggle(event: Event, adapter: EventsAdapter, position: Int) {
        try {
            Log.i("HomeFragment", "Checking current favorite state in the database for event: ${event.id}")
            val isFavoriteNow = withContext(Dispatchers.IO) {
                currentUser?.let { FavoritesRepository.isEventFavorite(event.id.toString(), it.uid) }
            } ?: false

            if (isFavoriteNow != event.isFavorite) {
                Log.i("HomeFragment", "Mismatch between UI and database state. Syncing UI with database.")
                event.isFavorite = isFavoriteNow
                adapter.notifyItemChanged(position)
                return
            }

            if (isFavoriteNow) {
                withContext(Dispatchers.IO) {
                    currentUser?.let { FavoritesRepository.removeEventFromFavorites(event.id.toString(), it.uid) }
                }
                Log.i("HomeFragment", "Removed event: ${event.id} from favorites")
                event.isFavorite = false
            } else {
                withContext(Dispatchers.IO) {
                    currentUser?.let { FavoritesRepository.addEventToFavorites(event.id.toString(), it.uid) }
                }
                Log.i("HomeFragment", "Added event: ${event.id} to favorites")
                event.isFavorite = true
            }

            adapter.notifyItemChanged(position)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to toggle favorite: ${e.message}", e)
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
                EventUtils.deleteEvent(event.id.toString())
                sendRefreshBroadcast()
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
            fetchNewestEvents()
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

    private fun sendRefreshBroadcast() {
        val intent = Intent("com.example.eventup.REFRESH_EVENTS")
        context?.sendBroadcast(intent)
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

    private fun syncEvents(events: List<Event>, adapter: EventsAdapter) {
        lifecycleScope.launch {
            try {
                val favoriteEventIds = if (currentUser != null) {
                    withContext(Dispatchers.IO) {
                        FavoritesRepository.getUserEventIds(currentUser.uid)
                    }
                } else {
                    emptyList()
                }
                adapter.submitList(events.map { event ->
                    event.copy().apply {
                        isFavorite = favoriteEventIds.contains(id.toString())
                    }
                })
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to sync events: ${e.message}", e)
            }
        }
    }
}
