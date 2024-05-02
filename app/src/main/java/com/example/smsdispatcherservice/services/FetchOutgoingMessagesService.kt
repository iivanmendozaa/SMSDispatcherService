package com.example.smsdispatcherservice.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.smsdispatcherservice.MainActivity
import com.example.smsdispatcherservice.R
import com.example.smsdispatcherservice.infrastructure.MSSQLDatabaseHandler
import com.example.smsdispatcherservice.infrastructure.MessageSender
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask

class FetchOutgoingMessagesService : Service() {

    private val timer = Timer()
    private lateinit var wakeLock: PowerManager.WakeLock

    @SuppressLint("WakelockTimeout")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Acquire the wake lock
        wakeLock.acquire()

        createNotificationChannel()
        startForegroundService()
        scheduleDatabaseQueryTask()


        return START_STICKY
    }
    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FetchOutgoingMessagesService::WakeLock")
    }
    override fun onDestroy() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
        val restartIntent = Intent(applicationContext, FetchOutgoingMessagesService::class.java)
        startService(restartIntent)
        println("Instance launched.")
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification.build())
    }

    private fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Service is running")
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Play default notification sound
            .setSmallIcon(R.drawable.notification_icon)
    }

    private fun scheduleDatabaseQueryTask() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun run() {
                performDatabaseQuery()
            }
        }, 0, QUERY_INTERVAL_MILLISECONDS) // 5 minutes interval
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(DelicateCoroutinesApi::class)
    private fun performDatabaseQuery() {
        GlobalScope.launch(Dispatchers.IO) {
            try {

                sendNotification("Hearth Beat", "Keeping Alive")
                println("Im Alive")

                try {
                    val smsSender = MessageSender()
                        withContext(Dispatchers.IO) {
                        val databaseHandler = MSSQLDatabaseHandler(applicationContext)
                        databaseHandler.main()

                        val registers = databaseHandler.retrieveMessagesForAndroidDevice("679f3397506141a8")

                        registers.forEach { message ->
                            smsSender.sendSMS(message.number, message.content)
                            databaseHandler.markMessageAsSent(message.id)
                        }
                            val registersCount = registers.size
                            sendNotification("Sync Executed", "$registersCount ${if (registersCount == 1) "was" else "were"} processed")
                    }

                } catch (e: Exception) {
                    // Handle exceptions here
                }


            } catch (e: Exception) {
                // Handle exceptions here
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    private fun sendNotification(title: String, content: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "FetchOutgoingMessagesChannel"
        private const val QUERY_INTERVAL_MILLISECONDS = 1 * 60 * 1000L // 5 minutes
    }
}
