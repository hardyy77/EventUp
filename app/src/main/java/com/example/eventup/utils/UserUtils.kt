package com.example.eventup.utils

import android.util.Log
import com.example.eventup.models.Event
import com.example.eventup.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UserUtils {

    private var currentUser: User? = null

    // Funkcja zwracająca ID aktualnie zalogowanego użytkownika
    fun getCurrentUserId(): String? {
        return currentUser?.uid
    }

    // Funkcja zwracająca aktualnie zalogowanego użytkownika
    fun getCurrentUser(): User? {
        return currentUser
    }

    // Funkcja ustawiająca aktualnie zalogowanego użytkownika
    fun setCurrentUser(user: User) {
        currentUser = user
        Log.d("UserUtils", "Current user set to: ${user.uid}")
    }

    // Funkcja wylogowująca użytkownika
    fun logoutUser() {
        Log.d("UserUtils", "User ${currentUser?.uid} logged out")
        currentUser = null
    }

}
