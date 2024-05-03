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
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.smsdispatcherservice.MainActivity
import com.example.smsdispatcherservice.R
import com.example.smsdispatcherservice.infrastructure.MSSQLDatabaseHandler
import com.example.smsdispatcherservice.infrastructure.MessageSender
import com.example.smsdispatcherservice.utilities.ConfigReader
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Timer
import java.util.TimerTask

class FetchOutgoingMessagesService : Service() {

    private val timer = Timer()
    private lateinit var wakeLock: PowerManager.WakeLock
    private var queryIntervalMilliseconds: Long = 30000
    private var androidDeviceId: String? = null
    private var configReader: ConfigReader? = null
    private var config: JSONObject? = null
    @SuppressLint("WakelockTimeout")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Acquire the wake lock
        wakeLock.acquire()
        loadSettings()
        println("Trying to launch with $queryIntervalMilliseconds and $androidDeviceId")

        createNotificationChannel()
        startForegroundService()
        scheduleDatabaseQueryTask(queryIntervalMilliseconds, androidDeviceId!!)


        return START_STICKY
    }
    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FetchOutgoingMessagesService::WakeLock")
    }

    @SuppressLint("HardwareIds")
    fun loadSettings(){

        androidDeviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        configReader = ConfigReader(this.applicationContext)
        config = configReader!!.getConfig()
        val QUERY_INTERVAL_MILLISECONDS_STR = config!!.getString("queryInterval")

        queryIntervalMilliseconds = QUERY_INTERVAL_MILLISECONDS_STR.toLongOrNull() ?: 300000L // Default value of 5 minutes in milliseconds
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()

       val restartIntent = Intent(applicationContext, FetchOutgoingMessagesService::class.java)
       restartIntent.putExtra("ANDROID_DEVICE_ID", androidDeviceId)
       startForegroundService(restartIntent)

       println("Instance Relauched.")
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

    private fun scheduleDatabaseQueryTask(queryIntervalMilliseconds: Long, deviceId: String) {
        timer.scheduleAtFixedRate(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun run() {
                performDatabaseQuery(deviceId)
            }
        }, 0, queryIntervalMilliseconds)
    }

    @SuppressLint("SuspiciousIndentation")
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(DelicateCoroutinesApi::class)
    private fun performDatabaseQuery(deviceId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {

                sendNotification("Hearth Beat", "Keeping Alive")
                println("Im Alive")

                try {
                    val smsSender = MessageSender()
                        withContext(Dispatchers.IO) {
                        val databaseHandler = MSSQLDatabaseHandler(applicationContext)
                        databaseHandler.main()

                        val registers = databaseHandler.retrieveMessagesForAndroidDevice(deviceId)

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

    // Function to read settings from the appSettings.json file
    private fun readSettingsFromFile(settingsFile: File): JSONObject {
        if (!settingsFile.exists()) {
            // If the file doesn't exist, create an empty JSONObject
            return JSONObject()
        }
        // Read the contents of the file and parse it as JSON
        val fileContent = settingsFile.readText()
        return JSONObject(fileContent)
    }

    // Function to write settings to the appSettings.json file
    private fun writeSettingsToFile(settings: JSONObject, settingsFile: File) {
        // Convert the JSONObject to a string
        val settingsString = settings.toString()
        // Write the string to the file
        settingsFile.writeText(settingsString)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "FetchOutgoingMessagesChannel"
        private const val DEFAULT_INTERVAL = 300000L // 5 minutes

    }
}
