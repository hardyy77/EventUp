package com.example.eventup.models

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    var role: String = "user",
)
