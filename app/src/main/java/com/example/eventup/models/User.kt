package com.example.eventup.models

data class User(
    val uid: String = "", // Unikalny identyfikator użytkownika
    val email: String = "", // Adres email użytkownika
    val displayName: String = "", // Wyświetlana nazwa użytkownika
    var role: String = "user" // Rola użytkownika w systemie, domyślnie "user"
)
