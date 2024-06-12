package com.example.eventup.models

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "user",
    val id: Int?
)
