package com.example.eventup.activities

import android.content.Intent
import android.os.Bundle
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

    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event)

        titleField = findViewById(R.id.titleField)
        dateField = findViewById(R.id.dateField)
        locationField = findViewById(R.id.locationField)
        genresField = findViewById(R.id.genresField)
        descriptionField = findViewById(R.id.descriptionField)
        saveButton = findViewById(R.id.saveButton)
        deleteButton = findViewById(R.id.deleteButton)

        eventId = intent.getStringExtra("EVENT_ID")

        if (eventId != null) {
            loadEvent(eventId!!)
            deleteButton.visibility = View.VISIBLE
        } else {
            deleteButton.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            saveEvent()
        }

        deleteButton.setOnClickListener {
            deleteEvent()
        }
    }

    private fun loadEvent(eventId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val query = "SELECT * FROM events WHERE id = '$eventId'"
            val resultSet = withContext(Dispatchers.IO) {
                DatabaseHandler.executeQuery(query)
            }
            if (resultSet != null && resultSet.next()) {
                val event = Event(
                    id = resultSet.getString("id"),
                    name = resultSet.getString("name"),
                    location = resultSet.getString("location"),
                    date = resultSet.getString("date"),
                    genres = resultSet.getString("genres"),
                    description = resultSet.getString("description")
                )
                titleField.setText(event.name)
                dateField.setText(event.date)
                locationField.setText(event.location)
                genresField.setText(event.genres)
                descriptionField.setText(event.description)
            } else {
                Toast.makeText(this@ManageEventActivity, "Failed to load event", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEvent() {
        val title = titleField.text.toString()
        val date = dateField.text.toString()
        val location = locationField.text.toString()
        val genres = genresField.text.toString()
        val description = descriptionField.text.toString()

        if (title.isEmpty() || date.isEmpty() || location.isEmpty() || genres.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val event = Event(eventId ?: java.util.UUID.randomUUID().toString(), title, date, location, genres, description)
            val query = if (eventId == null) {
                "INSERT INTO events (id, name, location, date, genres, description) VALUES ('${event.id}', '${event.name}', '${event.location}', '${event.date}', '${event.genres}', '${event.description}')"
            } else {
                "UPDATE events SET name = '${event.name}', location = '${event.location}', date = '${event.date}', genres = '${event.genres}', description = '${event.description}' WHERE id = '${event.id}'"
            }

            val result = withContext(Dispatchers.IO) {
                DatabaseHandler.executeUpdate(query)
            }
            if (result > 0) {
                Toast.makeText(this@ManageEventActivity, "Event saved", Toast.LENGTH_SHORT).show()
                sendUpdateEventBroadcast(event)
                finish()
            } else {
                Toast.makeText(this@ManageEventActivity, "Failed to save event", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteEvent() {
        eventId?.let {
            CoroutineScope(Dispatchers.Main).launch {
                val query = "DELETE FROM events WHERE id = '$it'"
                val result = withContext(Dispatchers.IO) {
                    DatabaseHandler.executeUpdate(query)
                }
                if (result > 0) {
                    Toast.makeText(this@ManageEventActivity, "Event deleted", Toast.LENGTH_SHORT).show()
                    sendRefreshBroadcast()
                    finish()
                } else {
                    Toast.makeText(this@ManageEventActivity, "Failed to delete event", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendRefreshBroadcast() {
        val intent = Intent("com.example.eventup.REFRESH_EVENTS")
        sendBroadcast(intent)
    }

    private fun sendUpdateEventBroadcast(event: Event) {
        val intent = Intent("com.example.eventup.UPDATE_EVENT")
        intent.putExtra("updated_event", event)
        sendBroadcast(intent)
    }
}
