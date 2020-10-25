package ant.swcapp

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ant.swcapp.data.Alarm
import ant.swcapp.data.AlarmTime
import ant.swcapp.utils.Extras
import kotlinx.android.synthetic.main.activity_alarm.*

class AlarmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        val alarm = if (intent.getIntExtra(Extras.ALARM_ID.name, -1) != -1) {
            Alarm.getById(this@AlarmActivity, intent.getIntExtra(Extras.ALARM_ID.name, -1))
        } else {
            Alarm()
        }

        if (alarm == null) {
            finish()
            return
        }

        if (alarm.id != -1) {
            tv_title.text = "수정"
        } else {
            tv_title.text = "추가"
        }

        tv_time.text = alarm.getTime().getString()
        tv_repeat.text = alarm.repeat.getString()
        et_title.setText(alarm.title)
        et_message.setText(alarm.message)
        cb_response.isChecked = alarm.hasResponse
        et_response.setText(alarm.response)
        et_response_time.setText(alarm.responseTime.toString())

        btn_submit.setOnClickListener {
            alarm.title = et_title.text.toString()
            alarm.message = et_message.text.toString()
            alarm.response = et_response.text.toString()
            alarm.hasResponse = cb_response.isChecked
            alarm.responseTime = et_response_time.text.toString().toInt()

            if (alarm.id != -1) {
                alarm.save(this@AlarmActivity)
            } else {
                alarm.add(this@AlarmActivity)
            }

            finish()
        }

        tv_time.setOnClickListener {
            TimePickerDialog(this@AlarmActivity, {view, hourOfDay, minute ->
                alarm.setTime(AlarmTime(hourOfDay, minute))
                tv_time.text = alarm.getTime().getString()
            }, alarm.getTime().hour, alarm.getTime().minute, false).show()
        }

        tv_repeat.setOnClickListener {
            val boolArray = alarm.repeat.getBooleanArray()
            AlertDialog.Builder(this@AlarmActivity)
                .setTitle("반복")
                .setMultiChoiceItems(arrayOf("일", "월", "화", "수", "목", "금", "토"), boolArray) { dialog: DialogInterface?, which: Int, isChecked: Boolean ->
                    boolArray[which] = isChecked
                }
                .setPositiveButton("확인") { dialog, which ->
                    alarm.repeat.setBooleanArray(boolArray)
                    tv_repeat.text = alarm.repeat.getString()
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}