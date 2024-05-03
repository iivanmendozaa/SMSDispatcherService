package com.example.smsdispatcherservice.utilities

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OutgoingSmsLog(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "OutgoingSmsLog.db"
        private const val TABLE_NAME = "OutgoingMessagesSent"
        private const val COLUMN_CREATED_ON = "CreatedOn"
        private const val COLUMN_PHONE_NUMBER = "PhoneNumber"
        private const val COLUMN_CONTENT = "Content"
        private const val COLUMN_REQUESTED_ON = "RequestedOn"
        private const val COLUMN_SOURCE = "Source"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_CREATED_ON TEXT," +
                "$COLUMN_PHONE_NUMBER TEXT," +
                "$COLUMN_CONTENT TEXT," +
                "$COLUMN_REQUESTED_ON TEXT," +
                "$COLUMN_SOURCE TEXT)"

        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addRegister(phoneNumber: String, content: String, requestedOn: String, source: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CREATED_ON, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                Date()
            ))
            put(COLUMN_PHONE_NUMBER, phoneNumber)
            put(COLUMN_CONTENT, content)
            put(COLUMN_REQUESTED_ON, requestedOn)
            put(COLUMN_SOURCE, source)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    @SuppressLint("Range")
    fun getAll(): JSONObject {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_CREATED_ON DESC", null)

        val jsonArray = JSONArray()
        while (cursor.moveToNext()) {
            val jsonObject = JSONObject().apply {
                put(COLUMN_CREATED_ON, cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_ON)))
                put(COLUMN_PHONE_NUMBER, cursor.getString(cursor.getColumnIndex(COLUMN_PHONE_NUMBER)))
                put(COLUMN_CONTENT, cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)))
                put(COLUMN_REQUESTED_ON, cursor.getString(cursor.getColumnIndex(COLUMN_REQUESTED_ON)))
                put(COLUMN_SOURCE, cursor.getString(cursor.getColumnIndex(COLUMN_SOURCE)))
            }
            jsonArray.put(jsonObject)
        }
        cursor.close()

        return JSONObject().apply {
            put("outgoingMessages", jsonArray)
        }
    }

    @SuppressLint("Range")
    fun getByDate(createdOn: String): String {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COLUMN_CREATED_ON = ?", arrayOf(createdOn))

        val jsonArray = JSONArray()
        while (cursor.moveToNext()) {
            val jsonObject = JSONObject().apply {
                put(COLUMN_CREATED_ON, cursor.getString(cursor.getColumnIndex(COLUMN_CREATED_ON)))
                put(COLUMN_PHONE_NUMBER, cursor.getString(cursor.getColumnIndex(COLUMN_PHONE_NUMBER)))
                put(COLUMN_CONTENT, cursor.getString(cursor.getColumnIndex(COLUMN_CONTENT)))
                put(COLUMN_REQUESTED_ON, cursor.getString(cursor.getColumnIndex(COLUMN_REQUESTED_ON)))
                put(COLUMN_SOURCE, cursor.getString(cursor.getColumnIndex(COLUMN_SOURCE)))
            }
            jsonArray.put(jsonObject)
        }
        cursor.close()
        return jsonArray.toString()
    }
}