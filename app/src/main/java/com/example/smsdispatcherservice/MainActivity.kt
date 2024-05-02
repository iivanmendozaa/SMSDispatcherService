package com.example.smsdispatcherservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
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
import com.example.smsdispatcherservice.services.FetchOutgoingMessagesService
import com.example.smsdispatcherservice.utilities.ConfigReader

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
        else{
            val configReader = ConfigReader(this)
            val QUERY_INTERVAL_MILLISECONDS_STR = configReader.getConfigParameter("QUERY_INTERVAL_MILLISECONDS")
            val QUERY_INTERVAL_MILLISECONDS = QUERY_INTERVAL_MILLISECONDS_STR?.toLongOrNull() ?: 300000L // Default value of 5 minutes in milliseconds

            val serviceIntent = Intent(this, FetchOutgoingMessagesService::class.java)
            serviceIntent.putExtra("QUERY_INTERVAL_MILLISECONDS", QUERY_INTERVAL_MILLISECONDS)
            serviceIntent.putExtra("ANDROID_DEVICE_ID", deviceId)
            this.startService(serviceIntent)

            showToast("FetchOutgoingMessagesService Service has been launched")

        }


    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

