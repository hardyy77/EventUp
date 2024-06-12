package com.example.eventup.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.models.Event
import com.example.eventup.databinding.ActivityEventDetailsBinding
import com.google.gson.Gson

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve the Event object passed through intent
        val eventJson = intent.getStringExtra("event")
        if (eventJson != null) {
            val event = Gson().fromJson(eventJson, Event::class.java)
            displayEventDetails(event)
        } else {
            // Handle the case where event is null
            finish() // Close the activity if no event is passed
        }
    }

    /**
     * Display the details of the event in the UI.
     */
    private fun displayEventDetails(event: Event) {
        binding.eventTitle.text = event.name
        binding.eventDate.text = event.date
        binding.eventLocation.text = event.location
        binding.eventGenres.text = event.genres
        binding.eventDescription.text = event.description
    }
}
