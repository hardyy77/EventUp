package com.example.eventup.utils

import android.content.Context
import android.content.Intent
import com.example.eventup.activities.EventDetailsActivity
import com.example.eventup.models.Event
import com.google.gson.Gson

object EventDetailsHelper {
    fun openEventDetails(context: Context, event: Event) {
        val intent = Intent(context, EventDetailsActivity::class.java).apply {
            val eventJson = Gson().toJson(event)
            putExtra("event", eventJson)
        }
        context.startActivity(intent)
    }
}
