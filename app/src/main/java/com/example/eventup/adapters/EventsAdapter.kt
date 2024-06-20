package com.example.eventup.adapters

import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventup.R
import com.example.eventup.models.Event
import com.example.eventup.utils.FavoritesRepository
import com.example.eventup.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EventsAdapter(
    private val onClick: (Event) -> Unit,
    private val onFavoriteClick: (Event) -> Unit,
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    var events: List<Event> = emptyList()
    private val currentUser = UserUtils.getCurrentUser()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view, onClick, onFavoriteClick, onEditClick, onDeleteClick)
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

    inner class EventViewHolder(
        itemView: View,
        private val onClick: (Event) -> Unit,
        private val onFavoriteClick: (Event) -> Unit,
        private val onEditClick: (Event) -> Unit,
        private val onDeleteClick: (Event) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.event_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.event_date)
        private val locationTextView: TextView = itemView.findViewById(R.id.event_location)
        private val genresTextView: TextView = itemView.findViewById(R.id.event_genres)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.iv_favorite)
        private val optionsImageView: ImageView = itemView.findViewById(R.id.iv_options)

        private var currentEvent: Event? = null

        init {
            itemView.setOnClickListener {
                currentEvent?.let {
                    onClick(it)
                }
            }
            favoriteImageView.setOnClickListener {
                currentEvent?.let { event ->
                    if (currentUser != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            toggleFavorite(event)
                            onFavoriteClick(event)
                        }
                    } else {
                        // Pokaż komunikat użytkownikowi, że musi się zalogować
                    }
                }
            }
            optionsImageView.setOnClickListener {
                currentEvent?.let { event ->
                    showPopupMenu(optionsImageView, event)
                }
            }
        }

        fun bind(event: Event) {
            currentEvent = event
            titleTextView.text = event.name
            dateTextView.text = event.date
            locationTextView.text = event.location
            genresTextView.text = event.genres
            updateFavoriteIcon(false)
            if (currentUser != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    val isFavorite = FavoritesRepository.isEventFavorite(event.id.toString(), currentUser.uid)
                    updateFavoriteIcon(isFavorite)
                }
            }
            checkUserAuthorization()
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            favoriteImageView.setImageResource(if (isFavorite) R.drawable.ic_filled_heart else R.drawable.ic_outline_heart)
        }

        private fun checkUserAuthorization() {
            if (currentUser?.role == "admin") {
                optionsImageView.visibility = View.VISIBLE
            } else {
                optionsImageView.visibility = View.GONE
            }
        }

        private fun showPopupMenu(view: View, event: Event) {
            val popup = PopupMenu(view.context, view)
            val inflater: MenuInflater = popup.menuInflater
            inflater.inflate(R.menu.menu_event_options, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_edit -> {
                        onEditClick(event)
                        true
                    }
                    R.id.menu_delete -> {
                        onDeleteClick(event)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private suspend fun toggleFavorite(event: Event) {
            val isFavorite = currentUser?.let { FavoritesRepository.isEventFavorite(event.id.toString(), it.uid) }
            if (isFavorite == true) {
                currentUser?.let { FavoritesRepository.removeEventFromFavorites(event.id.toString(), it.uid) }
                updateFavoriteIcon(false)
            } else {
                currentUser?.let { FavoritesRepository.addEventToFavorites(event.id.toString(), it.uid) }
                updateFavoriteIcon(true)
            }
        }
    }
}
