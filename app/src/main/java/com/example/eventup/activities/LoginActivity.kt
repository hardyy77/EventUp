package com.example.eventup.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.R
import com.example.eventup.utils.DatabaseHandler
import com.example.eventup.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button

    // Funkcja wywoływana przy tworzeniu aktywności.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicjalizacja pól tekstowych i przycisków z layoutu
        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)

        // Ustawienie listenera dla przycisku logowania
        loginButton.setOnClickListener {
            val email = emailField.text.toString() // Pobranie tekstu z pola email
            val password = passwordField.text.toString() // Pobranie tekstu z pola hasła
            Log.d("LoginActivity", "Login button clicked with email: $email")
            loginUser(email, password) // Próba zalogowania użytkownika
        }

        // Ustawienie listenera dla przycisku rejestracji
        signUpButton.setOnClickListener {
            val email = emailField.text.toString() // Pobranie tekstu z pola email
            val password = passwordField.text.toString() // Pobranie tekstu z pola hasła
            registerUser(email, password) // Próba rejestracji użytkownika
        }
    }

    // Loguje użytkownika na podstawie podanego adresu e-mail i hasła.
    private fun loginUser(email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("LoginActivity", "Attempting to login user with email: $email")
                val hashedPassword = hashPassword(password) // Haszowanie hasła
                Log.d("LoginActivity", "Hashed password: $hashedPassword")

                // Weryfikacja użytkownika w bazie danych w kontekście IO
                val isValidUser = withContext(Dispatchers.IO) {
                    DatabaseHandler.verifyUser(email, hashedPassword)
                }

                if (isValidUser) {
                    setCurrentUser(email) // Ustawienie zalogowanego użytkownika
                } else {
                    Log.d("LoginActivity", "Login failed for email: $email")
                    setResult(Activity.RESULT_CANCELED)
                    Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during login: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Login error occurred", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Ustawia aktualnego zalogowanego użytkownika.

    private fun setCurrentUser(email: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Pobranie użytkownika z bazy danych w kontekście IO
                val user = withContext(Dispatchers.IO) {
                    DatabaseHandler.getUserByEmail(email)
                }

                if (user != null) {
                    UserUtils.setCurrentUser(user) // Ustawienie aktualnego użytkownika w systemie
                    Log.d("LoginActivity", "Current user set: ${user.email} with role ${user.role}")

                    // Wysłanie broadcastu informującego o pomyślnym zalogowaniu
                    sendBroadcast(Intent("com.example.eventup.LOGIN_SUCCESS"))
                    setResult(Activity.RESULT_OK)
                    finish() // Zamknięcie aktywności po zalogowaniu
                } else {
                    Log.e("LoginActivity", "Failed to set current user")
                    Toast.makeText(this@LoginActivity, "Failed to set current user", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error setting current user: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Error setting current user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Rejestruje nowego użytkownika na podstawie podanego adresu e-mail i hasła.

    private fun registerUser(email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val hashedPassword = hashPassword(password) // Haszowanie hasła
                val query = "INSERT INTO users (email, password) VALUES ('$email', '$hashedPassword')" // Zapytanie SQL do wstawienia użytkownika
                val result = withContext(Dispatchers.IO) {
                    DatabaseHandler.executeUpdate(query) // Wykonanie zapytania SQL
                }

                if (result > 0) {
                    Toast.makeText(this@LoginActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                    loginUser(email, password) // Automatyczne logowanie po rejestracji
                } else {
                    Toast.makeText(this@LoginActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during registration: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Registration error occurred", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Haszuje hasło za pomocą algorytmu SHA-256.

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256") // Inicjalizacja algorytmu SHA-256
        val digest = md.digest(password.toByteArray()) // Haszowanie hasła
        // Konwersja hasła do formatu szesnastkowego
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
