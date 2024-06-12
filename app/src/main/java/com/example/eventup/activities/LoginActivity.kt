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
import com.example.eventup.models.User
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
            registerUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("LoginActivity", "Attempting to login user with email: $email")
            val hashedPassword = hashPassword(password)
            Log.d("LoginActivity", "Hashed password: $hashedPassword")
            val isValidUser = withContext(Dispatchers.IO) {
                DatabaseHandler.verifyUser(email, hashedPassword)
            }
            if (isValidUser) {
                setCurrentUser(email)
            } else {
                Log.d("LoginActivity", "Login failed for email: $email")
                setResult(Activity.RESULT_CANCELED)
                Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setCurrentUser(email: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val user = withContext(Dispatchers.IO) {
                DatabaseHandler.getUserByEmail(email)
            }
            if (user != null) {
                UserUtils.setCurrentUser(user)
                Log.d("LoginActivity", "Current user set: ${user.email} with role ${user.role}")
                sendBroadcast(Intent("com.example.eventup.LOGIN_SUCCESS"))
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Log.e("LoginActivity", "Failed to set current user")
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val hashedPassword = hashPassword(password)
            val query = "INSERT INTO users (email, password) VALUES ('$email', '$hashedPassword')"
            val result = withContext(Dispatchers.IO) {
                DatabaseHandler.executeUpdate(query)
            }
            if (result > 0) {
                Toast.makeText(this@LoginActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                loginUser(email, password) // Automatically log in after registration
            } else {
                Toast.makeText(this@LoginActivity, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(password.toByteArray())
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }
}
