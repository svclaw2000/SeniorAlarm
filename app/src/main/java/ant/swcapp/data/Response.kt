package ant.swcapp.data

import android.content.Context
import android.os.Environment
import android.widget.Toast
import ant.swcapp.R
import ant.swcapp.utils.DatabaseHandler
import ant.swcapp.utils.MyLogger
import ant.swcapp.utils.SDF
import com.google.gson.JsonObject
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.util.*

class Response(
    var id: Int = -1,
    val datetime: Date = Date(),
    val alarmMessage: String = "",
    val alarmResponse: String = "",
    val response: String = ""
) {
    companion object {
        fun getFromJson(jResponse: JsonObject) : Response {
            return Response(
                id = jResponse["id"].asInt,
                datetime = SDF.dateTimeBar.parse(jResponse["datetime"].asString),
                alarmMessage = jResponse["alarm_message"].asString,
                alarmResponse = jResponse["alarm_response"].asString,
                response = jResponse["response"].asString
            )
        }

        fun getAll(context: Context) : Array<Response> {
            val mHandler = DatabaseHandler.open(context)

            try {
                val sql = """
                    SELECT id, datetime, alarm_message, alarm_response, response
                    FROM RESPONSE_TB
                """.trimIndent()

                val lResult = mHandler.read(sql)
                val responses = Array(lResult.size()) { Response() }

                for (i in 0..lResult.size() - 1) {
                    responses[i] = getFromJson(lResult[i].asJsonObject)
                }

                return responses
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mHandler.close()
            }

            return arrayOf()
        }

        fun add(context: Context, alarm: Alarm, resp: String) {
            Response(
                datetime = Date(),
                alarmMessage = alarm.message,
                alarmResponse = alarm.response,
                response = resp
            ).add(context)
        }

        fun saveToFile(context: Context) {
            val responses = getAll(context)
            MyLogger.d("Response Count", "${responses.size}")

            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            path.mkdirs()
            val file = File(path, "${SDF.dateTimeBar.format(Date())}.tsv")
            val writer = FileWriter(file, false)

            writer.write("ID\tDateTime\tAlarmMessage\tAlarmResponse\tResponse\n")
            for (response in responses) {
                writer.write("${response.id}\t${SDF.dateTimeBar.format(response.datetime)}\t${response.alarmMessage}\t${response.alarmResponse}\t${response.response}\n")
            }
            writer.close()

            Toast.makeText(context, String.format(context.getString(R.string.save_success), file.absolutePath), Toast.LENGTH_SHORT).show()
        }
    }

    fun add(context: Context) : Int? {
        val mHandler = DatabaseHandler.open(context)

        try {
            var sql = """
                INSERT INTO RESPONSE_TB (datetime, alarm_message, alarm_response, response)
                VALUES (
                    "${SDF.dateTimeBar.format(datetime)}",
                    "${alarmMessage}",
                    "${alarmResponse}",
                    "${response}"
                )
            """.trimIndent()

            mHandler.write(sql)

            sql = """
                SELECT MAX(id) AS id FROM RESPONSE_TB
            """.trimIndent()

            val jResult = mHandler.read(sql)

            if (jResult.size() > 0) {
                id = jResult[0].asJsonObject["id"].asInt
                return id
            }

            return null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mHandler.close()
        }

        return null
    }
}