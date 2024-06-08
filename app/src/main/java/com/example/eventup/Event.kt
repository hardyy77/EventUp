package com.example.eventup

data class Event(
    val name: String,
    val location: String,
    val date: String,
    val genres: String,
    val description: String = "",
    val interest: Int = 0  // Domyślna wartość interest to 0
)
