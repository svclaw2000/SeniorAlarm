package ant.swcapp.data

import android.content.Context
import ant.swcapp.utils.DatabaseHandler
import ant.swcapp.utils.SDF
import com.google.gson.JsonObject
import java.lang.Exception
import java.util.*

class Alarm(
    var id: Int = -1,
    time: AlarmTime = AlarmTime(0, 0),
    isEnabled: Boolean = true,
    var repeat: AlarmRepeat = AlarmRepeat(),
    var message: String = "",
    var repeatTime : Int = 5,
    var repeatCount : Int = 3,
    var lastAlarmDate : Date? = Date(),
    var recordEnd : Boolean = true
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
        recordEnd = true
        lastAlarmDate = null
    }

    fun setIsEnabled(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        if (!isEnabled) {
            recordEnd = true
            lastAlarmDate = null
        }
    }

    fun getIsEnabled() : Boolean {
        return isEnabled
    }

    companion object {
        const val CODE_ALARM = 0L
        const val CODE_REPEAT = 1L

        fun getFromJson(jAlarm: JsonObject): Alarm {
            return Alarm(
                id = jAlarm["id"].asInt,
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
                message = jAlarm["message"].asString,
                repeatTime = jAlarm["repeat_time"].asInt,
                repeatCount = jAlarm["repeat_count"].asInt,
                lastAlarmDate = if (!jAlarm["last_alarm_date"].isJsonNull) SDF.dateTimeBar.parse(jAlarm["last_alarm_date"].asString) else null,
                recordEnd = jAlarm["record_end"].asInt == 1
            )
        }

        fun getAll(context: Context): Array<Alarm> {
            val mHandler = DatabaseHandler.open(context)

            try {
                val sql = """
                    SELECT id, hour, minute, is_enabled, sun, mon, tue, wed, thu, fri, sat, message, repeat_time, repeat_count, last_alarm_date, record_end 
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
                    SELECT id, hour, minute, is_enabled, sun, mon, tue, wed, thu, fri, sat, message, repeat_time, repeat_count, last_alarm_date, record_end 
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
                if (nextAlarm == null || nextTime[0] < nextAlarmTime) {
                    nextAlarm = alarm
                    nextAlarmTime = nextTime[0]
                }
            }

            return nextAlarm
        }

        fun getUncompletedAlarmString(context : Context) : String? {
            val mHandler = DatabaseHandler.open(context)

            try {
                val sql = """
                    SELECT id, hour, minute, is_enabled, sun, mon, tue, wed, thu, fri, sat, message, repeat_time, repeat_count, last_alarm_date, record_end 
                    FROM ALARM_TB 
                    WHERE record_end=0
                    ORDER BY hour, minute
                """.trimIndent()

                val lResult = mHandler.read(sql)
                val alarms = Array(lResult.size()) { Alarm() }

                for (i in 0..lResult.size() - 1) {
                    alarms[i] = getFromJson(lResult[i].asJsonObject)
                }

                val ret = arrayListOf("RecordDate\tLastAlarmDate\tMessage")
                for (alarm in alarms) {
                    ret.add("${SDF.dateTimeBar.format(Date())}\t${SDF.dateTimeBar.format(alarm.lastAlarmDate)}\t${alarm.message}")
                }
                return ret.joinToString("\n")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                mHandler.close()
            }

            return null
        }

        fun setComplete(context: Context) : Boolean {
            val mHandler = DatabaseHandler.open(context)

            try {
                val sql = """
                UPDATE ALARM_TB SET
                record_end=1
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
    }

    fun add(context: Context): Int? {
        val mHandler = DatabaseHandler.open(context)

        try {
            var sql = """
                INSERT INTO ALARM_TB (hour, minute, is_enabled, sun, mon, tue, wed, thu, fri, sat, message, repeat_time, repeat_count, last_alarm_date, record_end ) 
                VALUES (
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
                    "${message}",
                    ${repeatTime},
                    ${repeatCount},
                    ${if (lastAlarmDate == null) "null" else "\"${SDF.dateTimeBar.format(lastAlarmDate)}\""},
                    ${if (recordEnd) 1 else 0}
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
                message="${message}",
                repeat_time="${repeatTime}",
                repeat_count=${repeatCount},
                last_alarm_date=${if (lastAlarmDate == null) "null" else "\"${SDF.dateTimeBar.format(lastAlarmDate)}\""},
                record_end=${if (recordEnd) 1 else 0} 
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

    fun getNextAlarmTime(): Array<Long>? {
        if (!isEnabled && lastAlarmDate == null) {
            return null
        }

        val nextCal = Calendar.getInstance()
        val curTime = Date().time

        if (!recordEnd && lastAlarmDate != null) {
            nextCal.time = lastAlarmDate
            for (i in 1..repeatCount) {
                nextCal.add(Calendar.MINUTE, repeatTime)
                if (nextCal.timeInMillis > curTime) {
                    return arrayOf(nextCal.timeInMillis, CODE_REPEAT)
                }
            }
        }

        nextCal.timeInMillis = curTime
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

        return arrayOf(nextCal.timeInMillis, CODE_ALARM)
    }

    // Call this function when the alarm ring.
    fun startAlarm(context: Context) {
        if (repeat.getCount() == 0) {
            isEnabled = false
        }
        lastAlarmDate = Date()
        recordEnd = false
        save(context)
    }
}
