package com.example.eventup

data class Event(
    val name: String,
    val location: String,
    val date: String,
    val genres: String,
    val description: String,
    val interest: Int
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "location" to location,
            "date" to date,
            "genres" to genres,
            "description" to description,
            "interest" to interest
        )
    }
}
