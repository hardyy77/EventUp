package com.example.eventup.models

import com.google.firebase.firestore.PropertyName

data class Event(
    var id: String = "", // Ensure id is a var and has a default value
    val name: String = "",
    val location: String = "",
    val date: String = "",
    val genres: String = "",
    val description: String = "",
    var interest: Int = 0,
    @get:PropertyName("isFavorite") @set:PropertyName("isFavorite") var isFavorite: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "location" to location,
            "date" to date,
            "genres" to genres,
            "description" to description,
            "interest" to interest,
            "isFavorite" to isFavorite
        )
    }
}
