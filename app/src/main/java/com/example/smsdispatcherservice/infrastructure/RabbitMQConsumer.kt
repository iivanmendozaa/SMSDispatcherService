package com.example.smsdispatcherservice.infrastructure

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException

class RabbitMQConsumer(private val queueName: String, private val username: String, private val password: String) {
    private val factory = ConnectionFactory()
    private lateinit var connection: Connection
    private lateinit var channel: Channel

    @OptIn(DelicateCoroutinesApi::class)
    fun connect(onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                factory.host = "192.168.1.76" // RabbitMQ server host
                factory.username = username
                factory.password = password
                connection = factory.newConnection()
                channel = connection.createChannel()
                channel.queueDeclare(queueName, false, false, false, null)
                onSuccess.invoke()
            } catch (e: IOException) {
                e.printStackTrace()
                onError.invoke(e)
            }
        }
    }

    fun consume(callback: (String) -> Unit) {
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

    fun close() {
        channel.close()
        connection.close()
    }
}
