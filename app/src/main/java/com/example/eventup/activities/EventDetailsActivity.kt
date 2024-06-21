package com.example.eventup.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.models.Event
import com.example.eventup.databinding.ActivityEventDetailsBinding
import com.google.gson.Gson

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding

    //Funkcja wywoływana przy tworzeniu aktywności.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // Pobranie obiektu Event przekazanego przez intent
            val eventJson = intent.getStringExtra("event")
            if (eventJson != null) {
                val event = Gson().fromJson(eventJson, Event::class.java)
                displayEventDetails(event)
            } else {
                // Obsługa przypadku, gdy event jest null
                Log.e("EventDetailsActivity", "No event data passed through intent.")
                finish() // Zamknięcie aktywności, jeśli nie przekazano danych wydarzenia
            }
        } catch (e: Exception) {
            Log.e("EventDetailsActivity", "Error retrieving event data: ${e.message}", e)
            finish() // Zamknięcie aktywności w przypadku błędu
        }
    }

    // Wyświetla szczegóły wydarzenia w interfejsie użytkownika.

    private fun displayEventDetails(event: Event) {
        try {
            binding.eventTitle.text = event.name
            binding.eventDate.text = event.date
            binding.eventLocation.text = event.location
            binding.eventGenres.text = event.genres
            binding.eventDescription.text = event.description
        } catch (e: Exception) {
            Log.e("EventDetailsActivity", "Error displaying event details: ${e.message}", e)
        }
    }
}
