package ant.swcapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main_2.*
import ant.swcapp.data.Alarm
import ant.swcapp.utils.MyLogger
import ant.swcapp.utils.SDF
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        // requestPermission() 에 사용되어 callBack 함수인 onRequestPermission() 에서
        // 어떤 권한에 대한 onRequestPermission 인지 식별하기 위한 식별자인데
        // onRequestPermission 을 사용안해서 큰 의미는 없는 상수입니다
        const val REQUEST_RECORD_AUDIO_PERMISSION = 430
        const val TAG = "MainActivity"
        var activity : MainActivity? = null
    }

    var curResponseAlarm : Alarm? = null
    lateinit var alarmManager : AlarmManager

    val tempCalendar = Calendar.getInstance()
    var isRecording = false

    // 발화 (TTS) 를 위한 변수
    private var mediaPlayer: MediaPlayer? = null // 플레이어
    private var mediaRecorder: MediaRecorder? = null // 리코더

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_2)

        activity = this@MainActivity
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        btn_alarm_list.setOnClickListener {
            val intent = Intent(this@MainActivity, AlarmListActivity::class.java)
            startActivity(intent)
        }

        when (intent?.getIntExtra(AlarmReceiver.EXTRA_TYPE, -1) ?: return) {
            AlarmReceiver.RC_ALARM -> {
                MyLogger.d(TAG, "Alarm Received")
                val alarm = Alarm.getById(this@MainActivity, intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)) ?: return
                alarm.startAlarm(this@MainActivity)
                talk(alarm.message)
                startAlarm()
            }
            AlarmReceiver.RC_REPEAT -> {
                MyLogger.d(TAG, "Repeat Received")
                val alarm = Alarm.getById(this@MainActivity, intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)) ?: return
                talk(alarm.message)
                startAlarm()
            }
        }

        btn_record.setOnClickListener {
            // 버튼 누르면 녹음 여부에 상관없이 일단 종료
            if (mediaRecorder != null) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null

                Alarm.setComplete(this@MainActivity)
                startAlarm()
            }

            if (!isRecording) {
                // 녹음 시작
                val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AntAlarm")
                path.mkdirs()
                val filename = "${SDF.dateTimeBar.format(Date())}.mp4"
                val file = File(path, filename)
                val logFile = File(path, "${filename}.tsv")
                val writer = FileWriter(logFile, false)
                writer.write(Alarm.getUncompletedAlarmString(this@MainActivity) ?: return@setOnClickListener)
                writer.close()

                mediaRecorder = MediaRecorder()
                mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                mediaRecorder?.setOutputFile(file)

                try {
                    mediaRecorder?.prepare()
                    mediaRecorder?.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            isRecording = !isRecording
            btn_record.text = getString(if (isRecording) R.string.record_end else R.string.record_start)
            btn_record.setBackgroundColor(getColor(if (isRecording) R.color.recordingOn else R.color.recordingOff))
        }
    }

    // 발화 (TTS) 함수
    // msg:String (in java; String msg) 를 넣어서 실행하면 된다
    fun talk(msg: String) {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer = null
        }
        mediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.effect_dingdong)
        mediaPlayer!!.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            start()
            setOnCompletionListener {
                mediaPlayer!!.stop()
                mediaPlayer = null

                mediaPlayer = MediaPlayer()
                mediaPlayer!!.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource("http://114.71.220.20:8882/get_text?text=$msg")
                    prepare()
                    start()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // if(음성 녹음 권한이 없다면)
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 권한을 요청합니다
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK, Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    fun startAlarm() {
        val alarm = Alarm.getNextAlarm(this@MainActivity)
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmIntent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm?.id)

        if (alarm == null) {
            val alarmPendingIntent = PendingIntent.getBroadcast(this, AlarmReceiver.RC_ALARM, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.cancel(alarmPendingIntent)
            tv_next_alarm.text = null
        } else {
            val alarmTriggerTime = alarm.getNextAlarmTime() ?: return
            tempCalendar.timeInMillis = alarmTriggerTime[0]
            alarmIntent.putExtra(AlarmReceiver.EXTRA_TYPE, alarmTriggerTime[1].toInt())
            MyLogger.d(TAG, "TYPE: ${alarmTriggerTime[1].toInt()}")

            val alarmPendingIntent = PendingIntent.getBroadcast(this, AlarmReceiver.RC_ALARM, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            tv_next_alarm.text = String.format(getString(R.string.next_fmt), tempCalendar[Calendar.YEAR], tempCalendar[Calendar.MONTH] + 1, tempCalendar[Calendar.DAY_OF_MONTH],
                tempCalendar[Calendar.HOUR_OF_DAY], tempCalendar[Calendar.MINUTE], alarm.message)
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTriggerTime[0], alarmPendingIntent)
            MyLogger.d("@Alarm@", "${alarmTriggerTime[0] - Date().time}")
        }
    }

    override fun onResume() {
        super.onResume()
        startAlarm()
    }
}
