package com.example.smsdispatcherservice.infrastructure

import android.content.Context
import com.example.smsdispatcherservice.domain.Message
import com.example.smsdispatcherservice.utilities.ConfigReader
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException


class MSSQLDatabaseHandler(private val context: Context) {

    private val configReader = ConfigReader(context = context)
    val config = configReader.getConfig()
    private val connectionString = config.getString("msSQLConnectionString")
    private val connectionUser = config.getString("msSQLUser")
    private val connectionPassword = config.getString("msSQLPassword")



    fun main() {

        var connection: Connection? = null

        try {

            connection = getConnection()

            println("Connection to the database established.")
        } catch (e: Exception) {
            println("Error connecting to the database: ${e.message}")
        } finally {
            // Close the connection
            connection?.close()
        }
    }

    fun retrieveMessagesForAndroidDevice(androidDeviceId: String): List<Message> {
        val messages = mutableListOf<Message>()
        var connection: Connection? = null
        var preparedStatement: PreparedStatement? = null

        try {
            connection = getConnection()
            val query = "SELECT * FROM OutgoingMessages WHERE AndroidDeviceId = ? AND Sent = 0"
            preparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, androidDeviceId)
            val resultSet = preparedStatement.executeQuery()

            while (resultSet.next()) {
                val messageId = resultSet.getInt("Id")
                val messageNumber = resultSet.getString("Number")
                val messageContent = resultSet.getString("Message")
                // Add retrieved message to the list
                messages.add(Message(messageId, messageNumber, messageContent))
            }
        } catch (e: SQLException) {
            println("Error retrieving messages from database: ${e.message}")
        } finally {
            closeResources(preparedStatement, connection)
        }

        return messages
    }

    fun markMessageAsSent(messageId: Int) {
        var connection: Connection? = null
        var preparedStatement: PreparedStatement? = null

        try {
            connection = getConnection()
            val query = "UPDATE OutgoingMessages SET Sent = 1 WHERE Id = ?"
            preparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setInt(1, messageId)
            preparedStatement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            closeResources(preparedStatement, connection)
        }
    }

    private fun getConnection(): Connection? {

        var connection: Connection? = null
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")
            connection = DriverManager.getConnection(connectionString, connectionUser, connectionPassword)
        } catch (e: SQLException) {
            println("Error establishing database connection: ${e.message}")
        }
        return connection
    }

    private fun closeResources(preparedStatement: PreparedStatement?, connection: Connection?) {
        try {
            preparedStatement?.close()
            connection?.close()
        } catch (e: SQLException) {
            println("Error closing database resources: ${e.message}")
        }
    }


}
