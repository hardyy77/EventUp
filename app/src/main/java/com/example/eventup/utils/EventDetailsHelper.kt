package com.example.eventup.utils

import android.content.Context
import android.content.Intent
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.models.Event

object EventDetailsHelper {
    fun openEventDetails(context: Context, event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java).apply {
            putExtra("name", event.name)
            putExtra("location", event.location)
            putExtra("date", event.date)
            putExtra("genres", event.genres)
            putExtra("description", event.description)
        }
        context.startActivity(intent)
    }
}
