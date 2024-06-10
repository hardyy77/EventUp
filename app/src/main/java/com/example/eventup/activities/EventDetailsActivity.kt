package com.example.eventup.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.models.Event
import com.example.eventup.databinding.ActivityEventDetailsBinding

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Suppress the deprecation warning for this method
        @Suppress("DEPRECATION")
        val event: Event? = intent.getParcelableExtra("event")
        if (event != null) {
            displayEventDetails(event)
        }
    }

    private fun displayEventDetails(event: Event) {
        // Wyświetlanie szczegółów wydarzenia w UI
        binding.eventTitle.text = event.name
        binding.eventDate.text = event.date
        binding.eventLocation.text = event.location
        binding.eventGenres.text = event.genres
        binding.eventDescription.text = event.description
    }
}
