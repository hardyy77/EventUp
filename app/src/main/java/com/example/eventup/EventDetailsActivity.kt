package com.example.eventup

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EventDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        val eventTitle = findViewById<TextView>(R.id.event_title)
        val eventLocation = findViewById<TextView>(R.id.event_location)
        val eventDate = findViewById<TextView>(R.id.event_date)
        val eventGenres = findViewById<TextView>(R.id.event_genres)
        val eventDescription = findViewById<TextView>(R.id.event_description)

        val extras = intent.extras
        if (extras != null) {
            eventTitle.text = extras.getString("name")
            eventLocation.text = extras.getString("location")
            eventDate.text = extras.getString("date")
            eventGenres.text = extras.getString("genres")
            eventDescription.text = extras.getString("description")
        }
    }
}
