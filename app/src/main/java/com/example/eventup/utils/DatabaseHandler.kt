package com.example.eventup.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import android.util.Log

object DatabaseHandler {

    private const val URL = "jdbc:mysql://192.168.1.144:3306/eventup"
    private const val USER = "root"
    private const val PASSWORD = "root"

    init {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            Log.d("DatabaseHandler", "MySQL JDBC Driver Registered")
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            Log.e("DatabaseHandler", "MySQL JDBC Driver not found")
        }
    }

    fun init() {
        // This method is intentionally left empty to be called for initialization purposes
    }

    private fun getConnection(): Connection? {
        Log.d("DatabaseHandler", "Connecting to database...")
        return try {
            DriverManager.getConnection(URL, USER, PASSWORD).also {
                Log.d("DatabaseHandler", "Connection established successfully")
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            Log.e("DatabaseHandler", "Failed to create the database connection: ${e.message}")
            null
        }
    }

    suspend fun executeQuery(query: String): ResultSet? = withContext(Dispatchers.IO) {
        var connection: Connection? = null
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
            Log.e("DatabaseHandler", "Error executing query: ${e.message}")
        } finally {
            // Do not close the connection here, let the caller handle it
        }
        resultSet
    }

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
            Log.e("DatabaseHandler", "Error executing update: ${e.message}")
        } finally {
            connection?.close()
        }
        result
    }

    suspend fun getUserId(email: String): String? = withContext(Dispatchers.IO) {
        val query = "SELECT id FROM users WHERE email = '$email'"
        var connection: Connection? = null
        var resultSet: ResultSet? = null
        var userId: String? = null
        try {
            connection = getConnection()
            if (connection != null) {
                resultSet = executeQuery(query)
                if (resultSet != null && resultSet.next()) {
                    userId = resultSet.getString("id")
                    Log.d("DatabaseHandler", "User ID retrieved: $userId for email: $email")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            Log.e("DatabaseHandler", "Error retrieving user ID: ${e.message}")
        } finally {
            resultSet?.close()
            connection?.close()
        }
        userId
    }

    suspend fun getUserRole(userId: String): String? = withContext(Dispatchers.IO) {
        val query = "SELECT role FROM users WHERE id = '$userId'"
        var connection: Connection? = null
        var resultSet: ResultSet? = null
        var role: String? = null
        try {
            connection = getConnection()
            if (connection != null) {
                resultSet = executeQuery(query)
                if (resultSet != null && resultSet.next()) {
                    role = resultSet.getString("role")
                    Log.d("DatabaseHandler", "User role retrieved: $role for user ID: $userId")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
            Log.e("DatabaseHandler", "Error retrieving user role: ${e.message}")
        } finally {
            resultSet?.close()
            connection?.close()
        }
        role
    }

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
            Log.e("DatabaseHandler", "Error verifying user: ${e.message}")
        } finally {
            resultSet?.close()
            connection?.close()
        }
        isValidUser
    }
}
