package ant.swcapp.data

import android.content.Context
import ant.swcapp.utils.DatabaseHandler
import ant.swcapp.utils.MyLogger
import ant.swcapp.utils.SDF
import com.google.gson.JsonObject
import java.lang.Exception
import java.util.*

class Alarm(
    var id: Int = -1,
    var title: String = "",
    time: AlarmTime = AlarmTime(0, 0),
    isEnabled: Boolean = true,
    var repeat: AlarmRepeat = AlarmRepeat(),
    var hasResponse: Boolean = false,
    var message: String = "",
    var response: String = "",
    var responseTime: Int = 30,
    var lastAlarmDate: Date? = null,
    var responseEnd: Boolean = true
) {
    private var time : AlarmTime = AlarmTime(0, 0)
    private var isEnabled : Boolean = true

    init {
        this.time = time
        this.isEnabled = isEnabled
    }

    fun getTime() : AlarmTime {
        return time
    }

    fun setTime(time: AlarmTime) {
        this.time = time
        responseEnd = true
        lastAlarmDate = null
    }

    fun setIsEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        if (!isEnabled) {
            responseEnd = true
        }
    }

    fun getIsEnabled() : Boolean {
        return isEnabled
    }

    companion object {
        fun getFromJson(jAlarm: JsonObject): Alarm {
            return Alarm(
                id = jAlarm["id"].asInt,
                title = jAlarm["title"].asString,
                time = AlarmTime(jAlarm["hour"].asInt, jAlarm["minute"].asInt),
                isEnabled = jAlarm["is_enabled"].asInt == 1,
                repeat = AlarmRepeat(
                    jAlarm["sun"].asInt == 1,
                    jAlarm["mon"].asInt == 1,
                    jAlarm["tue"].asInt == 1,
                    jAlarm["wed"].asInt == 1,
                    jAlarm["thu"].asInt == 1,
                    jAlarm["fri"].asInt == 1,
                    jAlarm["sat"].asInt == 1
                ),
                hasResponse = jAlarm["has_response"].asInt == 1,
                message = jAlarm["message"].asString,
                response = jAlarm["response"].asString,
                responseTime = jAlarm["response_time"].asInt,
                lastAlarmDate = if (!jAlarm["last_alarm_date"].isJsonNull) SDF.dateTimeBar.parse(jAlarm["last_alarm_date"].asString) else null,
                responseEnd = jAlarm["response_end"].asInt == 1
            )
        }

        fun getAll(context: Context): Array<Alarm> {
            val mHandler = DatabaseHandler.open(context)

            try {
                val sql = """
                    SELECT id, title, hour, minute, is_enabled, sun, mon, tue, wed, thu, fri, sat, has_response, message, response, response_time, last_alarm_date, response_end 
                    FROM ALARM_TB
                    ORDER BY hour, minute
                """.trimIndent()

                val lResult = mHandler.read(sql)
                val alarms = Array(lResult.size()) { Alarm() }

                for (i in 0..lResult.size() - 1) {
                    alarms[i] = getFromJson(lResult[i].asJsonObject)
                }

                return alarms
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mHandler.close()
            }

            return arrayOf()
        }

        fun getById(context: Context, id: Int): Alarm? {
            val mHandler = DatabaseHandler.open(context)

            try {
                val sql = """
                    SELECT id, title, hour, minute, is_enabled, sun, mon, tue, wed, thu, fri, sat, has_response, message, response, response_time, last_alarm_date, response_end 
                    FROM ALARM_TB 
                    WHERE id=${id}
                """.trimIndent()

                val jResult = mHandler.read(sql)
                if (jResult.size() > 0) {
                    return getFromJson(jResult[0].asJsonObject)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mHandler.close()
            }

            return null
        }

        fun getNextAlarm(context: Context): Alarm? {
            val alarms = getAll(context)
            if (alarms.isEmpty()) {
                return null
            }

            var nextAlarm: Alarm? = null
            var nextAlarmTime: Long = -1
            for (alarm in alarms) {
                val nextTime = alarm.getNextAlarmTime() ?: continue
                if (nextAlarm == null) {
                    nextAlarm = alarm
                    nextAlarmTime = nextTime
                } else if (nextTime < nextAlarmTime) {
                    nextAlarm = alarm
                    nextAlarmTime = nextTime
                }
            }

            return nextAlarm
        }

        fun getNextResponse(context: Context): Alarm? {
            val alarms = getAll(context)
            if (alarms.isEmpty()) {
                return null
            }

            var nextResponse: Alarm? = null
            var nextResponseTime: Long = -1
            for (alarm in alarms) {
                val nextTime = alarm.getNextResponseTime() ?: continue
                if (nextResponse == null) {
                    nextResponse = alarm
                    nextResponseTime = nextTime
                } else if (nextTime < nextResponseTime) {
                    nextResponse = alarm
                    nextResponseTime = nextTime
                }
            }

            return nextResponse
        }
    }

    fun add(context: Context): Int? {
        val mHandler = DatabaseHandler.open(context)

        try {
            var sql = """
                INSERT INTO ALARM_TB (title, hour, minute, is_enabled, sun, mon, tue, wed, thu, fri, sat, has_response, message, response, response_time, last_alarm_date, response_end) 
                VALUES (
                    "${title}",
                    ${time.hour},
                    ${time.minute},
                    ${if (isEnabled) 1 else 0},
                    ${if (repeat.sun) 1 else 0},
                    ${if (repeat.mon) 1 else 0},
                    ${if (repeat.tue) 1 else 0},
                    ${if (repeat.wed) 1 else 0},
                    ${if (repeat.thu) 1 else 0},
                    ${if (repeat.fri) 1 else 0},
                    ${if (repeat.sat) 1 else 0},
                    ${if (hasResponse) 1 else 0},
                    "${message}",
                    "${response}",
                    ${responseTime},
                    ${if (lastAlarmDate == null) "null" else "\"${SDF.dateTimeBar.format(lastAlarmDate)}\""},
                    ${if (responseEnd) 1 else 0}
                )
            """.trimIndent()

            mHandler.write(sql)

            sql = """
                SELECT MAX(id) AS id FROM ALARM_TB
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

    fun save(context: Context): Boolean {
        val mHandler = DatabaseHandler.open(context)

        try {
            val sql = """
                UPDATE ALARM_TB SET
                title="${title}",
                hour=${time.hour},
                minute=${time.minute},
                is_enabled=${if (isEnabled) 1 else 0},
                sun=${if (repeat.sun) 1 else 0},
                mon=${if (repeat.mon) 1 else 0},
                tue=${if (repeat.tue) 1 else 0},
                wed=${if (repeat.wed) 1 else 0},
                thu=${if (repeat.thu) 1 else 0},
                fri=${if (repeat.fri) 1 else 0},
                sat=${if (repeat.sat) 1 else 0},
                has_response=${if (hasResponse) 1 else 0},
                message="${message}",
                response="${response}",
                response_time=${responseTime},
                last_alarm_date=${if (lastAlarmDate == null) "null" else "\"${SDF.dateTimeBar.format(lastAlarmDate)}\""},
                response_end=${if (responseEnd) 1 else 0} 
                WHERE id=${id}
            """.trimIndent()

            mHandler.write(sql)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mHandler.close()
        }

        return false
    }

    fun delete(context: Context): Boolean {
        val mHandler = DatabaseHandler.open(context)

        try {
            val sql = """
                DELETE FROM ALARM_TB 
                WHERE id=${id}
            """.trimIndent()

            mHandler.write(sql)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mHandler.close()
        }

        return false
    }

    fun getNextAlarmTime(): Long? {
        if (!isEnabled) {
            return null
        }

        val curTime = Date().time
        val nextCal = Calendar.getInstance()
        nextCal[Calendar.HOUR_OF_DAY] = time.hour
        nextCal[Calendar.MINUTE] = time.minute
        nextCal[Calendar.SECOND] = 0
        nextCal[Calendar.MILLISECOND] = 0

        val repeatArray = repeat.getBooleanArray()
        if (repeat.getCount() == 0) {
            if (nextCal.timeInMillis < curTime) {
                nextCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else {
            while (!repeatArray[nextCal[Calendar.DAY_OF_WEEK] - 1] ||
                nextCal.timeInMillis < curTime
            ) {
                nextCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return nextCal.timeInMillis
    }

    // Call this function when the alarm ring.
    fun startAlarm(context: Context) {
        if (repeat.getCount() == 0) {
            isEnabled = false
        }
        lastAlarmDate = Date()
        responseEnd = false
        save(context)
    }

    fun getNextResponseTime() : Long? {
        if (!hasResponse) {
            return null
        }

        val nextCal = Calendar.getInstance()
        if (lastAlarmDate != null && !responseEnd) {
            nextCal.time = lastAlarmDate
            nextCal.add(Calendar.MINUTE, responseTime)

            val curTime = Date().time
            for (i in 1..3) {
                if (nextCal.timeInMillis > curTime) {
                    return nextCal.timeInMillis
                }
                nextCal.add(Calendar.MINUTE, 5)
            }
        }

        nextCal.timeInMillis = getNextAlarmTime() ?: return null
        nextCal.add(Calendar.MINUTE, responseTime)
        return nextCal.timeInMillis
    }

    fun isPassedWithNoResponse() : Boolean {
        val lastAlarmTime = lastAlarmDate?.time ?: return false
        if (lastAlarmTime + 60 * 1000 * (responseTime + 5 * 2 - 1) < Date().time) {
            return true
        }
        return false
    }
}
