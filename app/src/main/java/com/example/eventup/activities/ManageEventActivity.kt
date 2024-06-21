package com.example.eventup.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.R
import com.example.eventup.models.Event
import com.example.eventup.utils.DatabaseHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageEventActivity : AppCompatActivity() {

    private lateinit var titleField: EditText
    private lateinit var dateField: EditText
    private lateinit var locationField: EditText
    private lateinit var genresField: EditText
    private lateinit var descriptionField: EditText
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    private var eventId: Int? = null

    // Funkcja wywoływana przy tworzeniu aktywności.
    // Odpowiada za inicjalizację widoku oraz ustawienie przycisków zapisu i usuwania.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event)

        // Inicjalizacja pól tekstowych i przycisków z layoutu
        titleField = findViewById(R.id.titleField)
        dateField = findViewById(R.id.dateField)
        locationField = findViewById(R.id.locationField)
        genresField = findViewById(R.id.genresField)
        descriptionField = findViewById(R.id.descriptionField)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)

        // Pobranie ID wydarzenia z intencji (intent)
        eventId = intent.getIntExtra("EVENT_ID", -1)

        // Jeśli ID wydarzenia jest poprawne, wczytaj dane wydarzenia
        if (eventId != -1) {
            loadEvent(eventId!!)
            deleteButton.visibility = View.VISIBLE
        } else {
            deleteButton.visibility = View.GONE
        }

        // Ustawienie listenera dla przycisku zapisu
        saveButton.setOnClickListener {
            saveEvent()
        }

        // Ustawienie listenera dla przycisku usuwania
        deleteButton.setOnClickListener {
            deleteEvent()
        }
    }

    //Wczytuje dane wydarzenia z bazy danych na podstawie ID wydarzenia.

    private fun loadEvent(eventId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Zapytanie SQL do wczytania danych wydarzenia
                val query = "SELECT * FROM events WHERE id = $eventId"
                // Wykonanie zapytania w tle (IO) i pobranie wyników
                val resultSet = withContext(Dispatchers.IO) {
                    DatabaseHandler.executeQuery(query)
                }
                if (resultSet != null && resultSet.next()) {
                    // Utworzenie obiektu Event z wyników zapytania
                    val event = Event(
                        id = resultSet.getInt("id"),
                        name = resultSet.getString("name"),
                        location = resultSet.getString("location"),
                        date = resultSet.getString("date"),
                        genres = resultSet.getString("genres"),
                        description = resultSet.getString("description"),
                        interest = resultSet.getInt("interest"),
                        addedDate = resultSet.getString("added_date")
                    )
                    // Ustawienie danych wydarzenia w polach tekstowych
                    titleField.setText(event.name)
                    dateField.setText(event.date)
                    locationField.setText(event.location)
                    genresField.setText(event.genres)
                    descriptionField.setText(event.description)
                } else {
                    Toast.makeText(this@ManageEventActivity, "Nie udało się wczytać wydarzenia", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ManageEventActivity", "Błąd podczas wczytywania wydarzenia: ${e.message}", e)
                Toast.makeText(this@ManageEventActivity, "Błąd podczas wczytywania wydarzenia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Zapisuje dane wydarzenia do bazy danych.

    private fun saveEvent() {
        // Pobranie wartości z pól tekstowych
        val title = titleField.text.toString()
        val date = dateField.text.toString()
        val location = locationField.text.toString()
        val genres = genresField.text.toString()
        val description = descriptionField.text.toString()

        // Sprawdzenie, czy wszystkie pola są wypełnione
        if (title.isEmpty() || date.isEmpty() || location.isEmpty() || genres.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Proszę wypełnić wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Zapytanie SQL do zapisania lub zaktualizowania wydarzenia
                val query = if (eventId == null || eventId == -1) {
                    "INSERT INTO events (name, location, date, genres, description, interest, added_date) VALUES ('$title', '$location', '$date', '$genres', '$description', 0, NOW())"
                } else {
                    "UPDATE events SET name = '$title', location = '$location', date = '$date', genres = '$genres', description = '$description' WHERE id = $eventId"
                }

                // Wykonanie zapytania w tle (IO)
                val result = withContext(Dispatchers.IO) {
                    DatabaseHandler.executeUpdate(query)
                }
                if (result > 0) {
                    Toast.makeText(this@ManageEventActivity, "Wydarzenie zapisane", Toast.LENGTH_SHORT).show()
                    // Wysłanie broadcastu informującego o zaktualizowaniu wydarzenia
                    sendUpdateEventBroadcast(Event(eventId ?: 0, title, location, date, genres, description, 0, false, ""))
                    finish()
                } else {
                    Toast.makeText(this@ManageEventActivity, "Nie udało się zapisać wydarzenia", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ManageEventActivity", "Błąd podczas zapisywania wydarzenia: ${e.message}", e)
                Toast.makeText(this@ManageEventActivity, "Błąd podczas zapisywania wydarzenia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Usuwa wydarzenie z bazy danych.

    private fun deleteEvent() {
        eventId?.let {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Zapytanie SQL do usunięcia wydarzenia
                    val query = "DELETE FROM events WHERE id = $it"
                    // Wykonanie zapytania w tle (IO)
                    val result = withContext(Dispatchers.IO) {
                        DatabaseHandler.executeUpdate(query)
                    }
                    if (result > 0) {
                        Toast.makeText(this@ManageEventActivity, "Wydarzenie usunięte", Toast.LENGTH_SHORT).show()
                        // Wysłanie broadcastu informującego o usunięciu wydarzenia
                        sendRefreshBroadcast()
                        finish()
                    } else {
                        Toast.makeText(this@ManageEventActivity, "Nie udało się usunąć wydarzenia", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ManageEventActivity", "Błąd podczas usuwania wydarzenia: ${e.message}", e)
                    Toast.makeText(this@ManageEventActivity, "Błąd podczas usuwania wydarzenia", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Wysyła broadcast informujący o konieczności odświeżenia listy wydarzeń.

    private fun sendRefreshBroadcast() {
        val intent = Intent("com.example.eventup.REFRESH_EVENTS")
        sendBroadcast(intent)
    }

    //Wysyła broadcast informujący o zaktualizowaniu wydarzenia.

    private fun sendUpdateEventBroadcast(event: Event) {
        val intent = Intent("com.example.eventup.UPDATE_EVENT")
        intent.putExtra("updated_event", event)
        sendBroadcast(intent)
    }
}
