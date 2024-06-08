package com.example.eventup

import android.content.Context
import android.content.Intent

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
