package ant.swcapp.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.lang.Exception

class DatabaseHandler(context: Context?) {
    private val mHelper: DatabaseOpenHelper = DatabaseOpenHelper(context, DB_NAME, null, DB_VERSION)
    private var mDB: SQLiteDatabase = mHelper.readableDatabase

    companion object {
        const val DB_NAME = "PhoenixBot.db"
        const val DB_VERSION = 1

        fun open(context: Context?): DatabaseHandler {
            return DatabaseHandler(context)
        }
    }

    fun write(sql: String): Boolean {
        mDB = mHelper.writableDatabase
        MyLogger.d("Execute SQL", sql)
        try {
            mDB.execSQL(sql)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun read(sql: String): JsonArray {
        mDB = mHelper.readableDatabase

        val jRet = JsonArray()
        MyLogger.d("Execute SQL for result", sql)
        try {
            val cursor = mDB.rawQuery(sql, null)
            val colNames = cursor.columnNames
            val colCounts = cursor.columnCount
            if (cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val jItem = JsonObject()
                    for (i in 0..colCounts-1) {
                        jItem.addProperty(colNames[i], cursor.getString(i))
                    }
                    jRet.add(jItem)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return jRet
    }

    fun close() {
        mHelper.close()
    }
}