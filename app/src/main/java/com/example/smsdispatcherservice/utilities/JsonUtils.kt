package com.example.smsdispatcherservice.utilities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JsonUtils {
    companion object {
        val gson = Gson()

        // Serialize an object to JSON string
        inline fun <reified T> toJson(data: T): String {
            return gson.toJson(data)
        }

        // Deserialize a JSON string to an object of the specified type
        inline fun <reified T> fromJson(json: String): T {
            return gson.fromJson(json, T::class.java)
        }

        // Deserialize a JSON string to an object of the specified generic type
        fun <T> fromJson(json: String, type: TypeToken<T>): T {
            return gson.fromJson(json, type.type)
        }
    }
}
