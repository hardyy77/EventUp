package com.example.eventup.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.R
import com.example.eventup.utils.DatabaseHandler
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailField = findViewById(R.id.emailField)
        passwordField = findViewById(R.id.passwordField)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            Log.d("LoginActivity", "Login button clicked with email: $email")
            loginUser(email, password)
        }

        signUpButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            Log.d("LoginActivity", "Sign-up button clicked with email: $email")
            registerUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        Log.d("LoginActivity", "Attempting to login user with email: $email")
        CoroutineScope(Dispatchers.Main).launch {
            val hashedPassword = hashPassword(password)
            Log.d("LoginActivity", "Hashed password: $hashedPassword")
            val isValidUser = withContext(Dispatchers.IO) {
                Log.d("LoginActivity", "Verifying user in database")
                DatabaseHandler.verifyUser(email, hashedPassword)
            }
            if (isValidUser) {
                Log.d("LoginActivity", "Login successful for email: $email")
                // Login successful
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Log.d("LoginActivity", "Login failed for email: $email")
                // Login failed
                setResult(Activity.RESULT_CANCELED)
                Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        Log.d("LoginActivity", "Attempting to register user with email: $email")
        CoroutineScope(Dispatchers.Main).launch {
            val hashedPassword = hashPassword(password)
            Log.d("LoginActivity", "Hashed password: $hashedPassword")
            val query = "INSERT INTO users (email, password) VALUES ('$email', '$hashedPassword')"
            val result = withContext(Dispatchers.IO) {
                Log.d("LoginActivity", "Executing query: $query")
                DatabaseHandler.executeUpdate(query)
            }
            if (result > 0) {
                Log.d("LoginActivity", "Registration successful for email: $email")
                // Registration successful
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Log.d("LoginActivity", "Registration failed for email: $email")
                // Registration failed
                setResult(Activity.RESULT_CANCELED)
                Toast.makeText(this@LoginActivity, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hashPassword(password: String): String {
        Log.d("LoginActivity", "Hashing password")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray())
        val hashedPassword = digest.fold("", { str, it -> str + "%02x".format(it) })
        Log.d("LoginActivity", "Hashed password: $hashedPassword")
        return hashedPassword
    }
}
