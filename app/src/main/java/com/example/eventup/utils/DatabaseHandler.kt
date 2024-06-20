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

    private const val URL = "jdbc:mysql://192.168.0.192:3306/eventup"
    private const val USER = "root"
    private const val PASSWORD = "root"

    init {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            println("MySQL JDBC Driver Registered")
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            println("MySQL JDBC Driver not found")
        }
    }

    fun init() {
        // This method is intentionally left empty to be called for initialization purposes
    }

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
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            resultSet?.close()
            connection?.close()
        }
        userId
    }

    suspend fun getUserRole(userId: Int): String? = withContext(Dispatchers.IO) {
        val query = "SELECT role FROM users WHERE uid = $userId"
        var role: String? = null
        var connection: Connection? = null
        var resultSet: ResultSet? = null
        try {
            connection = getConnection()
            if (connection != null) {
                resultSet = executeQuery(query)
                if (resultSet != null && resultSet.next()) {
                    role = resultSet.getString("role")
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
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
        } finally {
            resultSet?.close()
            connection?.close()
        }
        isValidUser
    }

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
