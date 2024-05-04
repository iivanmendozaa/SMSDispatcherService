package com.example.smsdispatcherservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.smsdispatcherservice.services.FetchOutgoingMessagesService
import com.example.smsdispatcherservice.services.WebService

class MainActivity : ComponentActivity() {


    @SuppressLint("HardwareIds")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


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

            val serviceIntent = Intent(this, FetchOutgoingMessagesService::class.java)
            this.startService(serviceIntent)

            showToast("FetchOutgoingMessagesService Service has been launched")

            val webServiceIntent = Intent(this, WebService::class.java)
            this.startForegroundService(webServiceIntent)

            showToast("Website Management Service has been launched")


        }

        setContentView(R.layout.activity_main)

        val mWebView = findViewById<View>(R.id.WebView) as WebView
        mWebView.loadUrl("localhost:8080")

        val webSetting = mWebView.settings
        webSetting.javaScriptEnabled = true
        webSetting.allowContentAccess = true
        webSetting.domStorageEnabled = true
        webSetting.allowFileAccessFromFileURLs = true

        mWebView.webViewClient = WebViewClient()

        val handler = Handler()
        val refreshInterval = 30000 // Refresh interval in milliseconds (e.g., 30 seconds)

        val refreshRunnable = object : Runnable {
            override fun run() {
                // Reload the webpage
                mWebView.reload()
                // Schedule the next refresh
                handler.postDelayed(this, refreshInterval.toLong())
            }
        }

        handler.postDelayed(refreshRunnable, refreshInterval.toLong())


        mWebView.canGoBack()

        mWebView.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK
                && event.action == MotionEvent.ACTION_UP
                && mWebView.canGoBack()){
                mWebView.goBack()
                return@OnKeyListener true

            }
            false

        })


    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}

