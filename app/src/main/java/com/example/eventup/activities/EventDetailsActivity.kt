package com.example.eventup.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.databinding.ActivityEventDetailsBinding
import com.example.eventup.models.Event
import com.google.gson.Gson

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val eventJson = intent.getStringExtra("event")
        val event = Gson().fromJson(eventJson, Event::class.java)
        if (event != null) {
            binding.eventTitle.text = event.name
            binding.eventLocation.text = event.location
            binding.eventDate.text = event.date
            binding.eventGenres.text = event.genres
            binding.eventDescription.text = event.description
        }
    }
}
