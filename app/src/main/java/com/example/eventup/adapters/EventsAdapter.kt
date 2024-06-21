package com.example.eventup.adapters

import android.util.Log
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

// Adapter do obsługi listy wydarzeń w RecyclerView
class EventsAdapter(
    private val onClick: (Event) -> Unit, // Funkcja wywoływana przy kliknięciu na wydarzenie
    private val onFavoriteClick: (Event) -> Unit, // Funkcja wywoływana przy kliknięciu na ikonę ulubionych
    private val onEditClick: (Event) -> Unit, // Funkcja wywoływana przy kliknięciu na edytowanie wydarzenia
    private val onDeleteClick: (Event) -> Unit // Funkcja wywoływana przy kliknięciu na usunięcie wydarzenia
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    var events: List<Event> = emptyList() // Lista wydarzeń do wyświetlenia
    private val currentUser = UserUtils.getCurrentUser() // Aktualnie zalogowany użytkownik

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        // Inflatujemy widok dla pojedynczego elementu wydarzenia
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view, onClick, onFavoriteClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        // Wiążemy dane wydarzenia z widokiem
        holder.bind(events[position])
    }

    override fun getItemCount(): Int {
        // Zwracamy liczbę wydarzeń
        return events.size
    }

    fun submitList(events: List<Event>) {
        // Aktualizujemy listę wydarzeń i powiadamiamy adapter o zmianach
        this.events = events
        notifyDataSetChanged()
    }

    // ViewHolder odpowiedzialny za zarządzanie widokiem pojedynczego elementu listy
    inner class EventViewHolder(
        itemView: View,
        private val onClick: (Event) -> Unit, // Funkcja wywoływana przy kliknięciu na wydarzenie
        private val onFavoriteClick: (Event) -> Unit, // Funkcja wywoływana przy kliknięciu na ikonę ulubionych
        private val onEditClick: (Event) -> Unit, // Funkcja wywoływana przy kliknięciu na edytowanie wydarzenia
        private val onDeleteClick: (Event) -> Unit // Funkcja wywoływana przy kliknięciu na usunięcie wydarzenia
    ) : RecyclerView.ViewHolder(itemView) {

        // Inicjalizacja widoków
        private val titleTextView: TextView = itemView.findViewById(R.id.event_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.event_date)
        private val locationTextView: TextView = itemView.findViewById(R.id.event_location)
        private val genresTextView: TextView = itemView.findViewById(R.id.event_genres)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.iv_favorite)
        private val optionsImageView: ImageView = itemView.findViewById(R.id.iv_options)

        private var currentEvent: Event? = null // Aktualnie wybrane wydarzenie

        init {
            // Ustawienie listenera dla kliknięć na cały element listy
            itemView.setOnClickListener {
                currentEvent?.let {
                    onClick(it)
                }
            }

            // Ustawienie listenera dla kliknięć na ikonę ulubionych
            favoriteImageView.setOnClickListener {
                currentEvent?.let { event ->
                    if (currentUser != null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                // Przełączanie stanu ulubionych
                                toggleFavorite(event)
                                onFavoriteClick(event)
                            } catch (e: Exception) {
                                // Logowanie błędów
                                Log.e("EventsAdapter", "Error during switching favorites: ${e.message}")
                            }
                        }
                    } else {
                        // Pokaż komunikat użytkownikowi, że musi się zalogować
                        Log.w("EventsAdapter", "User must log in to add to favorites")
                    }
                }
            }

            // Ustawienie listenera dla kliknięć na ikonę opcji
            optionsImageView.setOnClickListener {
                currentEvent?.let { event ->
                    showPopupMenu(optionsImageView, event)
                }
            }
        }

        // Funkcja do bindowania danych wydarzenia do widoków
        fun bind(event: Event) {
            currentEvent = event
            // Ustawienie danych w widokach
            titleTextView.text = event.name
            dateTextView.text = event.date
            locationTextView.text = event.location
            genresTextView.text = event.genres
            updateFavoriteIcon(false)

            // Jeśli użytkownik jest zalogowany, sprawdź, czy wydarzenie jest ulubione
            if (currentUser != null) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val isFavorite = FavoritesRepository.isEventFavorite(event.id.toString(), currentUser.uid)
                        updateFavoriteIcon(isFavorite)
                    } catch (e: Exception) {
                        // Logowanie błędów
                        Log.e("EventsAdapter", "Error checking favorites: ${e.message}")
                    }
                }
            }
            checkUserAuthorization()
        }

        // Funkcja do aktualizacji ikony ulubionych w zależności od stanu
        private fun updateFavoriteIcon(isFavorite: Boolean) {
            favoriteImageView.setImageResource(if (isFavorite) R.drawable.ic_filled_heart else R.drawable.ic_outline_heart)
        }

        // Funkcja do sprawdzenia, czy bieżący użytkownik jest administratorem
        private fun checkUserAuthorization() {
            if (currentUser?.role == "admin") {
                optionsImageView.visibility = View.VISIBLE
            } else {
                optionsImageView.visibility = View.GONE
            }
        }

        // Funkcja do pokazania menu opcji
        private fun showPopupMenu(view: View, event: Event) {
            // Tworzenie nowego obiektu PopupMenu z kontekstem widoku i samym widokiem jako anchor
            val popup = PopupMenu(view.context, view)

            // Pobieranie inflatera menu z obiektu PopupMenu
            val inflater: MenuInflater = popup.menuInflater

            // Inflacja (rozdmuchiwanie) menu z pliku zasobów R.menu.menu_event_options do obiektu PopupMenu
            inflater.inflate(R.menu.menu_event_options, popup.menu)

            // Ustawienie listenera dla elementów menu PopupMenu
            popup.setOnMenuItemClickListener { menuItem ->
                // Sprawdzenie, który element menu został kliknięty
                when (menuItem.itemId) {
                    // Jeśli kliknięto opcję "Edytuj" (menu_edit)
                    R.id.menu_edit -> {
                        // Wywołanie funkcji onEditClick z przekazanym wydarzeniem
                        onEditClick(event)
                        true // Zwrócenie true, aby wskazać, że kliknięcie zostało obsłużone
                    }
                    // Jeśli kliknięto opcję "Usuń" (menu_delete)
                    R.id.menu_delete -> {
                        // Wywołanie funkcji onDeleteClick z przekazanym wydarzeniem
                        onDeleteClick(event)
                        true // Zwrócenie true, aby wskazać, że kliknięcie zostało obsłużone
                    }
                    // Jeśli kliknięto inną opcję (chociaż w tym przypadku nie ma innych opcji)
                    else -> false // Zwrócenie false, aby wskazać, że kliknięcie nie zostało obsłużone
                }
            }

            // Wyświetlenie PopupMenu na ekranie
            popup.show()
        }


        // Funkcja do przełączania stanu ulubionych
        private suspend fun toggleFavorite(event: Event) {
            try {
                // Sprawdzenie, czy wydarzenie jest już ulubione
                val isFavorite = currentUser?.let { FavoritesRepository.isEventFavorite(event.id.toString(), it.uid) }
                if (isFavorite == true) {
                    // Usunięcie z ulubionych
                    currentUser?.let { FavoritesRepository.removeEventFromFavorites(event.id.toString(), it.uid) }
                    updateFavoriteIcon(false)
                } else {
                    // Dodanie do ulubionych
                    currentUser?.let { FavoritesRepository.addEventToFavorites(event.id.toString(), it.uid) }
                    updateFavoriteIcon(true)
                }
            } catch (e: Exception) {
                // Logowanie błędów
                Log.e("EventsAdapter", "Błąd podczas przełączania ulubionych: ${e.message}")
            }
        }
    }
}
