package com.example.smsdispatcherservice.utilities

import android.content.Context
import com.example.smsdispatcherservice.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ConfigReader(private val context: Context) {

    fun getConfig(): JSONObject {
        val inputStream: InputStream = context.resources.openRawResource(R.raw.app_settings)
        val jsonString = readJsonString(inputStream)
        return JSONObject(jsonString)
    }

    private fun readJsonString(inputStream: InputStream): String {
        val stringBuilder = StringBuilder()
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        var line: String? = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.append(line)
            line = bufferedReader.readLine()
        }

        inputStream.close()
        return stringBuilder.toString()
    }
}
