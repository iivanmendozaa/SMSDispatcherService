package com.example.smsdispatcherservice.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.smsdispatcherservice.MainActivity
import com.example.smsdispatcherservice.R
import com.example.smsdispatcherservice.infrastructure.MessageSender
import com.example.smsdispatcherservice.utilities.ConfigReader
import com.example.smsdispatcherservice.utilities.OutgoingSmsLog
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.newFixedLengthResponse
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketException

@Suppress("DEPRECATION")
class WebService : Service() {
    private var apiKey: String = "thisistheapikey"
    private var server: NanoHTTPD? = null
    private var messageSender: MessageSender = MessageSender()
    private lateinit var wakeLock: PowerManager.WakeLock
    private var configReader: ConfigReader? = null
    private var config: JSONObject? = null
    private var outgoingSmsLog: OutgoingSmsLog? = null
    private var androidDeviceId: String? = null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate() {
        super.onCreate()

        configReader = ConfigReader(this.applicationContext)
        config = configReader!!.getConfig()

        //apiKey = loadApiKeyFromConfig()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WebService::WakeLock")
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification.build())
        outgoingSmsLog = OutgoingSmsLog(this.applicationContext)

        androidDeviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)


        // Acquire the wake lock
        wakeLock.acquire()

        if (server == null) {
            server = object : NanoHTTPD(8080) {
                override fun serve(session: IHTTPSession): Response {
                    //APIKEY MIDDLEWARE


                    val response = when {
                        (session.method == Method.GET) -> handleGetRequest(session)
                        (session.method == Method.POST && session.uri == "/sendMessage") -> handlePostEndpoint1(session)
                        (session.method == Method.POST && session.uri == "/modifySettings") -> settingsController(session)
                        else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
                    }

                    return response
                }
            }
            try {
                server?.start()
                println("Web server started")
            } catch (e: Exception) {
                println("Error starting web server: ${e.message}")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        server?.stop()
        println("Web server stopped")
        // Release the wake lock
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
        val restartIntent = Intent(applicationContext, WebService::class.java)
        startService(restartIntent)
    }

    private fun loadApiKeyFromConfig(): String {
        val assetManager = applicationContext.assets
        try {
            val inputStream = assetManager.open("appSettings.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            return jsonObject.optString("api_key", "")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        //default api key
        return DEFAULT_API_KEY
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

    private fun handleGetRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        return when (uri) {
            "/rawSettings" -> getSettingsController(session) // New route for retrieving settings
            "/health" -> handleHealthCheck()
            "/" -> serveHtmlPage("index.html") // New endpoint for serving HTML page
            "/home" -> serveHtmlPage("home.html") // New endpoint for serving HTML page
            "/settings" -> serveHtmlPage("settings.html") // New endpoint for serving HTML page
            "/sentMessages" -> serveHtmlPage("sentMessages.html") // New endpoint for serving HTML page
            "/login.js" -> serveHtmlPage("login.js") // New endpoint for serving HTML page
            "/settings.js" -> serveHtmlPage("settings.js") // New endpoint for serving HTML page
            "/another-page" -> serveHtmlPage("another-page.html") // New endpoint for serving HTML page
            "/getAllOutgoingMessages" -> getAllOutgoingMessagesController(session) // Register the new endpoint
            "/deviceInfo" -> deviceInfoController(session) // Register the new endpoint
            else -> newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not found")
        }
    }

    private fun getAllOutgoingMessagesController(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        try {
            // Retrieve all outgoing messages from the OutgoingSmsLog
            val outgoingSmsLog = OutgoingSmsLog(this.applicationContext)
            val allMessages = outgoingSmsLog.getAll()
            val jsonResponse = allMessages.toString() ?: "{}" // Convert config to JSON string

            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                jsonResponse
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error retrieving outgoing messages")
        }
    }

    private fun serveHtmlPage(htmlFile: String): NanoHTTPD.Response {
        return try {
            val inputStream = assets.open(htmlFile)
            val htmlContent = inputStream.bufferedReader().use { it.readText() }
            newFixedLengthResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML, htmlContent)
        } catch (e: IOException) {
            e.printStackTrace()
            newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error reading HTML file")
        }
    }
    private fun handlePostEndpoint1(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        try {
            val phoneNumber = session.parms["phoneNumber"]
            val message = session.parms["message"]

            if (phoneNumber.isNullOrEmpty()) {
                return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Missing phoneNumber Parameter"
                )
            }
            if (message.isNullOrEmpty()) {
                return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Missing message Parameter"
                )
            }

            messageSender.sendSMS(phoneNumber, message)
            outgoingSmsLog?.addRegister(phoneNumber,message, "","API")


            return newFixedLengthResponse("Message Sent")

        } catch (e: SocketException) {
            e.printStackTrace()
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Socket closed")
        } catch (e: Exception) {
            e.printStackTrace()
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error handling request")
        }
    }

    private fun getSettingsController(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        try {
            val currentConfig = configReader?.getConfig()
            val jsonResponse = currentConfig?.toString() ?: "{}" // Convert config to JSON string

            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                jsonResponse
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                NanoHTTPD.MIME_PLAINTEXT,
                "Error retrieving settings"
            )
        }
    }

    private fun deviceInfoController(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        try {
            // Retrieve the Android device ID using appropriate methods
            val androidDeviceId = androidDeviceId

            // Construct a JSON object containing the device ID
            val json = JSONObject().apply {
                put("AndroidDeviceId", androidDeviceId)
            }

            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                "application/json",
                json.toString()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return newFixedLengthResponse(
                NanoHTTPD.Response.Status.INTERNAL_ERROR,
                NanoHTTPD.MIME_PLAINTEXT,
                "Error handling request"
            )
        }
    }

    private fun settingsController(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        try {
            val key = session.parms["key"]
            val value = session.parms["value"]

            if (key.isNullOrEmpty()) {
                return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Missing Key Parameter"
                )
            }
            if (value.isNullOrEmpty()) {
                return newFixedLengthResponse(
                    NanoHTTPD.Response.Status.BAD_REQUEST,
                    NanoHTTPD.MIME_PLAINTEXT,
                    "Missing Value Parameter"
                )
            }

            val currentConfig = configReader?.getConfig()
            currentConfig?.put(key,value)
            configReader?.updateConfig(currentConfig!!)

            return newFixedLengthResponse("Settings Updated")

        } catch (e: SocketException) {
            e.printStackTrace()
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Socket closed")
        } catch (e: Exception) {
            e.printStackTrace()
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Error handling request")
        }
    }

    private fun handleHealthCheck(): NanoHTTPD.Response {
        return newFixedLengthResponse("Server is up and running!")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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

    private fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Web service is running")
            .setSmallIcon(R.drawable.notification_icon)
    }

    companion object {
        private const val CHANNEL_ID = "WebServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val DEFAULT_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"

    }
}

