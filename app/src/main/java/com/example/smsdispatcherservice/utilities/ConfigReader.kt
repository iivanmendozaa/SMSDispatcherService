package com.example.smsdispatcherservice.utilities

import android.content.Context
import org.json.JSONObject
import java.io.IOException

class ConfigReader(private val context: Context) {

    fun getConfigParameter(key: String): String? {
        var propertyValue: String? = null

        try {
            val inputStream = context.assets.open("appSettings.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            propertyValue = jsonObject.optString(key)
            inputStream.close()
        } catch (e: IOException) {
            println("Error reading config file: ${e.message}")
        }

        return propertyValue
    }
}
