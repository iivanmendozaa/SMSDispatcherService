package com.example.smsdispatcherservice.infrastructure

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class RabbitMQPublisher(private val exchangeName: String, private val routingKey: String, private val hostname: String, private val username: String, private val password: String) {
    private val factory = ConnectionFactory()
    private lateinit var connection: Connection
    private lateinit var channel: Channel

    suspend fun connect(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                factory.host = hostname
                factory.username = username
                factory.password = password
                connection = factory.newConnection()
                channel = connection.createChannel()
                true // Indicate success
            } catch (e: IOException) {
                e.printStackTrace()
                false // Indicate failure
            }
        }
    }

    suspend fun publish(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                channel.basicPublish(exchangeName, routingKey, null, message.toByteArray())
                true // Indicate success
            } catch (e: IOException) {
                e.printStackTrace()
                // Handle publishing error
                false // Indicate failure
            }
        }
    }

    fun close() {
        channel.close()
        connection.close()
    }
}