package ant.swcapp.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.lang.Exception

class DatabaseOpenHelper(val context: Context?, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) :
    SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        createTableAlarm(db)
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
                message TEXT NOT NULL,
                repeat_time INTEGER NOT NULL,
                repeat_count INTEGER NOT NULL,
                last_alarm_date TEXT,
                record_end INTEGER NOT NULL
            )
        """.trimIndent()

        MyLogger.d("Initialize ALARM_TB", sql)

        try {
            db.execSQL(sql)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}