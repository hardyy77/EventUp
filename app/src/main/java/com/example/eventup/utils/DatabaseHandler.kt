package com.example.eventup.utils

import android.util.Log
import com.example.eventup.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

object DatabaseHandler {

    // Stałe zawierające dane połączenia z bazą danych
    private const val URL = "jdbc:mysql://192.168.0.192:3306/eventup"
    private const val USER = "root"
    private const val PASSWORD = "root"

    init {
        try {
            // Rejestracja sterownika JDBC MySQL
            Class.forName("com.mysql.cj.jdbc.Driver")
            println("MySQL JDBC Driver Registered")
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            println("MySQL JDBC Driver not found")
        }
    }

    // Funkcja inicjalizująca, pozostawiona pusta celowo
    fun init() {
        // Celowa pozostawiona pusta metoda aby można było ją wywołać w celu inicjalizacji w MainActivity
    }

    // Funkcja uzyskująca połączenie z bazą danych
    private fun getConnection(): Connection? {
        println("Connecting to database...")
        return try {
            DriverManager.getConnection(URL, USER, PASSWORD).also {
                println("Connection established successfully")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            println("Failed to create the database connection: ${e.message}")
            null
        }
    }

    // Funkcja wykonująca zapytanie SQL w trybie asynchronicznym
    suspend fun executeQuery(query: String): ResultSet? = withContext(Dispatchers.IO) {
        val connection: Connection?
        var resultSet: ResultSet? = null
        try {
            connection = getConnection()
            if (connection != null) {
                val statement = connection.createStatement()
                resultSet = statement.executeQuery(query)
                Log.d("DatabaseHandler", "Query executed: $query")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        resultSet
    }

    // Funkcja wykonująca aktualizację SQL w trybie asynchronicznym
    suspend fun executeUpdate(query: String): Int = withContext(Dispatchers.IO) {
        var result = 0
        var connection: Connection? = null
        try {
            connection = getConnection()
            if (connection != null) {
                val statement = connection.createStatement()
                result = statement.executeUpdate(query)
                statement.close()
                Log.d("DatabaseHandler", "Update executed: $query")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            connection?.close()
        }
        result
    }

    // Funkcja weryfikująca użytkownika na podstawie email i hasła
    suspend fun verifyUser(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val query = "SELECT * FROM users WHERE email = '$email' AND password = '$password'"
        var connection: Connection? = null
        var resultSet: ResultSet? = null
        var isValidUser = false
        try {
            connection = getConnection()
            if (connection != null) {
                resultSet = executeQuery(query)
                if (resultSet != null && resultSet.next()) {
                    isValidUser = true
                    Log.d("DatabaseHandler", "User verified: $email")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            resultSet?.close()
            connection?.close()
        }
        isValidUser
    }

    // Funkcja uzyskująca użytkownika na podstawie adresu email
    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        val query = "SELECT * FROM users WHERE email = '$email'"
        var connection: Connection? = null
        var resultSet: ResultSet? = null
        var user: User? = null
        try {
            connection = getConnection()
            if (connection != null) {
                resultSet = executeQuery(query)
                if (resultSet != null && resultSet.next()) {
                    user = User(
                        uid = resultSet.getInt("uid").toString(),
                        email = resultSet.getString("email"),
                        displayName = resultSet.getString("displayName"),
                        role = resultSet.getString("role")
                    )
                    Log.d("DatabaseHandler", "User retrieved: $email")
                }
            }
        } catch (e: SQLException) {
            Log.e("DatabaseHandler", "Error retrieving user: ${e.message}")
            e.printStackTrace()
        } finally {
            resultSet?.close()
            connection?.close()
        }
        user
    }
}
