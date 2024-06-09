package com.example.eventup.utils

import com.example.eventup.models.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreUtils {

    fun saveEvent(event: Event, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("events")
            .add(event.toMap())
            .addOnSuccessListener { documentReference ->
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getEvents(onSuccess: (List<Event>) -> Unit, onFailure: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val events = result.map { document ->
                    Event(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        location = document.getString("location") ?: "",
                        date = document.getString("date") ?: "",
                        genres = document.getString("genres") ?: "",
                        description = document.getString("description") ?: "",
                        interest = document.getLong("interest")?.toInt() ?: 0,
                        isFavorite = document.getBoolean("isFavorite") ?: false
                    )
                }
                onSuccess(events)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun addEventToFavorites(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && event.id.isNotEmpty()) {
            val firestore = FirebaseFirestore.getInstance()
            val eventRef = firestore.collection("users").document(currentUser.uid).collection("favorites").document(event.id)
            eventRef.set(event.toMap())
                .addOnSuccessListener {
                    println("Successfully added event to favorites: ${event.id}")
                    event.interest += 1
                    updateEventInterest(event)
                    onSuccess()
                }
                .addOnFailureListener { e -> onFailure(e) }
        } else {
            onFailure(IllegalArgumentException("Invalid user or event ID"))
        }
    }

    fun removeEventFromFavorites(event: Event, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && event.id.isNotEmpty()) {
            val firestore = FirebaseFirestore.getInstance()
            val eventRef = firestore.collection("users").document(currentUser.uid).collection("favorites").document(event.id)
            eventRef.delete()
                .addOnSuccessListener {
                    println("Successfully removed event from favorites: ${event.id}")
                    event.interest -= 1
                    updateEventInterest(event)
                    onSuccess()
                }
                .addOnFailureListener { e -> onFailure(e) }
        } else {
            onFailure(IllegalArgumentException("Invalid user or event ID"))
        }
    }

    fun getFavorites(onSuccess: (List<Event>) -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(currentUser.uid).collection("favorites")
                .get()
                .addOnSuccessListener { documents ->
                    val favoriteEvents = documents.map { document ->
                        document.toObject(Event::class.java)
                    }
                    onSuccess(favoriteEvents)
                }
                .addOnFailureListener { e -> onFailure(e) }
        } else {
            onFailure(IllegalArgumentException("Invalid user ID"))
        }
    }

    private fun updateEventInterest(event: Event) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("events").document(event.id).update("interest", event.interest)
    }
}
