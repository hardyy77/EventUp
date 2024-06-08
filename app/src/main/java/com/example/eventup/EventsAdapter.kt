package com.example.eventup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventsAdapter : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    private var events: List<Event> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int {
        return events.size
    }

    fun submitList(events: List<Event>) {
        this.events = events
        notifyDataSetChanged()
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.event_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.event_date)
        private val locationTextView: TextView = itemView.findViewById(R.id.event_location)
        private val genresTextView: TextView = itemView.findViewById(R.id.event_genres)

        fun bind(event: Event) {
            titleTextView.text = event.name
            dateTextView.text = event.date
            locationTextView.text = event.location
            genresTextView.text = event.genres
        }
    }
}
