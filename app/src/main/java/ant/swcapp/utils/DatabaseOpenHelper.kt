package ant.swcapp.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.lang.Exception

class DatabaseOpenHelper(val context: Context?, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) :
    SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        createTableAlarm(db)
        createTableResponse(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    private fun createTableAlarm(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE IF NOT EXISTS ALARM_TB (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                is_enabled INTEGER NOT NULL,
                mon INTEGER NOT NULL,
                tue INTEGER NOT NULL,
                wed INTEGER NOT NULL,
                thu INTEGER NOT NULL,
                fri INTEGER NOT NULL,
                sat INTEGER NOT NULL,
                sun INTEGER NOT NULL,
                has_response INTEGER NOT NULL,
                message TEXT NOT NULL,
                response TEXT NOT NULL,
                response_time INTEGER NOT NULL,
                last_alarm_date TEXT,
                response_end INTEGER NOT NULL
            )
        """.trimIndent()

        MyLogger.d("Initialize ALARM_TB", sql)

        try {
            db.execSQL(sql)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createTableResponse(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE IF NOT EXISTS RESPONSE_TB (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                datetime TEXT NOT NULL,
                alarm_message TEXT NOT NULL,
                alarm_response TEXT NOT NULL,
                response TEXT NOT NULL
            )
        """.trimIndent()

        MyLogger.d("Initialize RESPONSE_TB", sql)

        try {
            db.execSQL(sql)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}