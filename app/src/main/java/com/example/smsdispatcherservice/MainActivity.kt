package com.example.smsdispatcherservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.smsdispatcherservice.infrastructure.MSSQLDatabaseHandler
import com.example.smsdispatcherservice.infrastructure.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {


    @SuppressLint("HardwareIds")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceId: String = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        println("DEVICE ID: $deviceId")


        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,Array(1){ Manifest.permission.INTERNET},101)
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,Array(1){ Manifest.permission.ACCESS_NETWORK_STATE},101)
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,Array(1){ Manifest.permission.FOREGROUND_SERVICE},101)
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,Array(1){ Manifest.permission.WAKE_LOCK},101)
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,Array(1){ Manifest.permission.SEND_SMS},101)
        }


        GlobalScope.launch(Dispatchers.Main) {
            try {
                val smsSender = MessageSender()
                val data = withContext(Dispatchers.IO) {
                    val databaseHandler = MSSQLDatabaseHandler(applicationContext)
                    databaseHandler.main()

                    val registers = databaseHandler.retrieveMessagesForAndroidDevice(deviceId)

                    registers.forEach { message ->
                        println("Message ID: ${message.id}")
                        println("Number: ${message.number}")
                        println("Message: ${message.content}")
                        println("------------------------")
                        smsSender.sendSMS(message.number, message.content)
                        databaseHandler.markMessageAsSent(message.id)

                    }

                }

                // Process retrieved data here
            } catch (e: Exception) {
                // Handle exceptions here
            }
        }

    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

