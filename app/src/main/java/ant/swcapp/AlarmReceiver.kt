package ant.swcapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import ant.swcapp.data.Alarm
import ant.swcapp.utils.MyLogger
import kotlinx.android.synthetic.main.activity_main.*

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "AlarmReceiver"

        const val RC_ALARM = 0
        const val RC_REPEAT = 1

        const val EXTRA_TYPE = "type"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val pm = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        val sCpuWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "AntAlarm:AlarmReceiver")
        sCpuWakeLock.acquire(1000)
        sCpuWakeLock.release()

        val activity = MainActivity.activity

        if (activity == null) {
            val startIntent = Intent(context, MainActivity::class.java)
            startIntent.putExtra(EXTRA_TYPE, intent?.getIntExtra(EXTRA_TYPE, -1))
            startIntent.putExtra(EXTRA_ALARM_ID, intent?.getIntExtra(EXTRA_ALARM_ID, -1))
            context.startActivity(startIntent)
        } else {
            when (intent?.getIntExtra(EXTRA_TYPE, -1) ?: return) {
                RC_ALARM -> {
                    MyLogger.d(TAG, "Alarm Received")
                    val alarm = Alarm.getById(context ?: return, intent.getIntExtra(EXTRA_ALARM_ID, -1)) ?: return
                    alarm.startAlarm(context)
                    activity.talk(alarm.message)
                    activity.startAlarm()
                }
                RC_REPEAT -> {
                    MyLogger.d(TAG, "Repeat Received")
                    val alarm = Alarm.getById(context ?: return, intent.getIntExtra(EXTRA_ALARM_ID, -1)) ?: return
                    activity.talk(alarm.message)
                    activity.startAlarm()
                }
            }
        }
    }
}