package com.example.smsdispatcherservice.infrastructure

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class RabbitMQPublisher(private val exchangeName: String, private val routingKey: String, private val hostname: String, private val username: String, private val password: String) {
    private val factory = ConnectionFactory()
    private lateinit var connection: Connection
    private lateinit var channel: Channel

    fun connect(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                factory.host = hostname
                factory.username = username
                factory.password = password
                connection = factory.newConnection()
                channel = connection.createChannel()
                onSuccess.invoke()
            } catch (e: IOException) {
                e.printStackTrace()
                onError.invoke(e)
            }
        }
    }

    fun publish(message: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                channel.basicPublish(exchangeName, routingKey, null, message.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
                // Handle publishing error
            }
        }
    }

    fun close() {
        channel.close()
        connection.close()
    }
}