package com.example.smsdispatcherservice.utilities

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smsdispatcherservice.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class ConfigReader(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "config.db"
        private const val TABLE_SETTINGS = "settings"
        private const val COLUMN_KEY = "key"
        private const val COLUMN_VALUE = "value"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = "CREATE TABLE $TABLE_SETTINGS ($COLUMN_KEY TEXT PRIMARY KEY, $COLUMN_VALUE TEXT)"
        db.execSQL(createTableQuery)

        val fileConfig = getConfig()
        updateConfig(fileConfig)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        onCreate(db)
    }

    @SuppressLint("Range")
    fun getConfig(): JSONObject {
        val query = "SELECT * FROM $TABLE_SETTINGS"
        val cursor = readableDatabase.rawQuery(query, null)
        val config = JSONObject()
        cursor.use {
            while (it.moveToNext()) {
                val key = it.getString(it.getColumnIndex(COLUMN_KEY))
                val value = it.getString(it.getColumnIndex(COLUMN_VALUE))
                config.put(key, value)
            }
        }
        return config
    }

    fun updateConfig(newConfig: JSONObject) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Clear existing settings
            db.delete(TABLE_SETTINGS, null, null)

            // Insert new settings
            for (key in newConfig.keys()) {
                val value = newConfig.getString(key)
                val contentValues = ContentValues()
                contentValues.put(COLUMN_KEY, key)
                contentValues.put(COLUMN_VALUE, value)
                db.insert(TABLE_SETTINGS, null, contentValues)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
    fun getFileConfig(): JSONObject {
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

