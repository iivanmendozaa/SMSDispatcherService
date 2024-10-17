package com.example.smsdispatcherservice.infrastructure

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class RabbitMQConsumer(private val queueName: String, private val hostname: String, private val username: String, private val password: String) {
    private val factory = ConnectionFactory()
    private lateinit var connection: Connection
    private lateinit var channel: Channel

    suspend fun connect(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                factory.host = hostname // RabbitMQ server host
                factory.username = username
                factory.password = password
                connection = factory.newConnection()
                channel = connection.createChannel()
                channel.queueDeclare(queueName, false, false, false, null)
                true // Indicate success
            } catch (e: IOException) {
                e.printStackTrace()
                false // Indicate failure
            }
        }
    }

    suspend fun consume(callback: (String) -> Unit) {
        try {
            withContext(Dispatchers.IO) {
                val consumer = object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String?,
                        envelope: Envelope?,
                        properties: AMQP.BasicProperties?,
                        body: ByteArray?
                    ) {
                        val message = body?.let { String(it) }
                        if (message != null) {
                            callback.invoke(message)
                            channel.basicAck(envelope?.deliveryTag ?: 0, false)
                        }
                    }
                }
                channel.basicConsume(queueName, false, consumer)
            }
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    fun close() {
        channel.close()
        connection.close()
    }
}
