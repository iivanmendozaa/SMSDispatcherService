package com.example.smsdispatcherservice.infrastructure

import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RabbitMQManager(private val hostname: String, private val username: String, private val password: String) {
    private val factory = ConnectionFactory()

    init {
        factory.host = hostname
        factory.username = username
        factory.password = password
    }

    suspend fun declareExchange(exchangeName: String, exchangeType: String): Boolean {
        return withContext(Dispatchers.IO) {
            suspendCoroutine<Boolean> { continuation ->
                val connection = factory.newConnection()
                val channel = connection.createChannel()
                println("Trying to create exchange.")

                try {
                    channel.exchangeDeclare(exchangeName, exchangeType, true)
                    println("Exchange '$exchangeName' declared successfully.")
                    continuation.resume(true)
                } catch (e: Exception) {
                    println("Error declaring exchange: ${e.message}")
                    continuation.resumeWithException(e)
                } finally {
                    try {
                        channel.close()
                    } catch (e: Exception) {
                        println("Error closing channel: ${e.message}")
                    }
                    try {
                        connection.close()
                    } catch (e: Exception) {
                        println("Error closing connection: ${e.message}")
                    }
                }
            }
        }
    }

    suspend fun addBinding(exchangeName: String, queueName: String, routingKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = factory.newConnection()
                val channel = connection.createChannel()

                // Declare the queue (if it doesn't exist) to check for the binding
                channel.queueDeclare(queueName, false, false, false, null)

                // Attempt to bind the queue to the exchange
                channel.queueBind(queueName, exchangeName, routingKey)
                println("Binding added successfully: Queue '$queueName' bound to Exchange '$exchangeName' with routing key '$routingKey'.")

                channel.close()
                connection.close()

                true // Indicate success
            } catch (e: Exception) {
                // If the binding already exists or an error occurs, log a message or perform other actions if needed
                println("Error adding binding: ${e.message}")
                false // Indicate failure
            }
        }
    }

}