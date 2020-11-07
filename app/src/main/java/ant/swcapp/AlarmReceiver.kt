package ant.swcapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ant.swcapp.data.Alarm
import ant.swcapp.utils.MyLogger
import kotlinx.android.synthetic.main.activity_main.*

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "AlarmReceiver"

        const val RC_ALARM = 0
        const val RC_RESPONSE = 1

        const val EXTRA_TYPE = "type"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val activity = MainActivity.activity

        if (activity == null) {
            val startIntent = Intent(context, MainActivity::class.java)
            startIntent.putExtra(EXTRA_TYPE, intent?.getIntExtra(EXTRA_TYPE, -1))
            startIntent.putExtra(EXTRA_ALARM_ID, intent?.getIntExtra(EXTRA_ALARM_ID, -1))
            context?.startActivity(startIntent)
        } else {
            when (intent?.getIntExtra(EXTRA_TYPE, -1) ?: return) {
                RC_ALARM -> {
                    MyLogger.d(TAG, "Alarm Received")
                    val alarm = Alarm.getById(context ?: return, intent.getIntExtra(EXTRA_ALARM_ID, -1)) ?: return
                    alarm.startAlarm(context)
                    activity.announcerTextView.text = alarm.message
                    val announcer = activity.announcer as MainActivity.Announcer
                    announcer.apply {
                        setInterested()
                        talk(alarm.message)
                    }
                    activity.startAlarm()
                }
                RC_RESPONSE -> {
                    MyLogger.d(TAG, "Response Received")
                    val response = Alarm.getById(context ?: return, intent.getIntExtra(EXTRA_ALARM_ID, -1)) ?: return
                    activity.announcerTextView.text = response.response
                    val announcer = activity.announcer as MainActivity.Announcer
                    announcer.apply {
                        setInterested()
                        talkForResponse(response.response, response)
                    }
                    activity.startResponse()
                }
            }
        }
    }
}