package com.example.smsdispatcherservice.infrastructure

import com.rabbitmq.client.ConnectionFactory
import java.io.IOException

class RabbitMQManager(private val hostname: String, private val username: String, private val password: String) {
    private val factory = ConnectionFactory()

    init {
        factory.host = hostname
        factory.username = username
        factory.password = password
    }

    fun declareExchange(exchangeName: String, exchangeType: String) {
        val connection = factory.newConnection()
        val channel = connection.createChannel()

        try {
            // Check if the exchange already exists
            val exchangeDeclareOk = channel.exchangeDeclarePassive(exchangeName)
            println("Exchange '$exchangeName' already exists.")

            // If the exchange exists, you can log a message or perform other actions if needed
        } catch (e: IOException) {
            // If the exchange doesn't exist, declare it
            try {
                channel.exchangeDeclare(exchangeName, exchangeType)
                println("Exchange '$exchangeName' declared successfully.")
            } catch (e: IOException) {
                println("Error declaring exchange: ${e.message}")
            }
        } finally {
            channel.close()
            connection.close()
        }
    }

    fun addBinding(exchangeName: String, queueName: String, routingKey: String) {
        val connection = factory.newConnection()
        val channel = connection.createChannel()

        try {
            // Declare the queue (if it doesn't exist) to check for the binding
            channel.queueDeclare(queueName, false, false, false, null)

            // Attempt to bind the queue to the exchange
            channel.queueBind(queueName, exchangeName, routingKey)
            println("Binding added successfully: Queue '$queueName' bound to Exchange '$exchangeName' with routing key '$routingKey'.")
        } catch (e: IOException) {
            // If the binding already exists, log a message or perform other actions if needed
            println("Binding already exists: Queue '$queueName' is already bound to Exchange '$exchangeName' with routing key '$routingKey'.")
        } finally {
            channel.close()
            connection.close()
        }
    }

}