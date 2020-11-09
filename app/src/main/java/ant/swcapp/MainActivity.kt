package ant.swcapp

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import android.os.Handler
import android.os.Message
import ant.swcapp.data.Alarm
import ant.swcapp.data.Response
import ant.swcapp.utils.MyLogger
import ant.swcapp.utils.SDF
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.*
import kotlin.collections.ArrayList


private const val PROCESS_LOG = "PROCESS LOG"


class MainActivity : AppCompatActivity() {
    var curResponseAlarm : Alarm? = null
    lateinit var alarmManager : AlarmManager

    companion object {
        // requestPermission() 에 사용되어 callBack 함수인 onRequestPermission() 에서
        // 어떤 권한에 대한 onRequestPermission 인지 식별하기 위한 식별자인데
        // onRequestPermission 을 사용안해서 큰 의미는 없는 상수입니다
        const val REQUEST_RECORD_AUDIO_PERMISSION = 430

        const val TAG = "MainActivity"

        // 애니메이션 설정에 사용하는 상수입니다
        const val ANIMATION_IDLE: Int = 1
        const val ANIMATION_LISTEN: Int = 2
        const val ANIMATION_ALARM: Int = 3
        const val ANIMATION_BYE: Int = 4

        var activity : MainActivity? = null

        val RESPONSE_LIST = arrayOf("그래", "응", "먹었어", "이미", "벌써")
    }

    // SpeechRecognizer 초기화용 인텐트
    lateinit var _intent: Intent

    // STT 오브젝트 생성
    lateinit var mRecognizer: SpeechRecognizer
    var isSpeaking = false

    val tempCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activity = this@MainActivity
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // SpeechRecognizer 설정에 필요한 정보를 담는 인텐트
        _intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        _intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        _intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        _intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        _intent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
        _intent.putExtra("android.speech.extra.GET_AUDIO", true)

        // SpeechRecognizer 초기화
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        mRecognizer.setRecognitionListener(listener)

        // Initialize talk ballon
        Glide.with(this).load(R.drawable.talk_ballon).into(talkBallon)

        btn_alarm_list.setOnClickListener {
            val intent = Intent(this@MainActivity, AlarmListActivity::class.java)
            startActivity(intent)
        }

        when (intent?.getIntExtra(AlarmReceiver.EXTRA_TYPE, -1) ?: return) {
            AlarmReceiver.RC_ALARM -> {
                MyLogger.d(TAG, "Alarm Received")
                val alarm = Alarm.getById(this@MainActivity, intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)) ?: return
                alarm.startAlarm(this@MainActivity)
                announcerTextView.text = alarm.message
                val announcer = announcer as MainActivity.Announcer
                announcer.apply {
                    setInterested()
                    talk(alarm.message)
                }
                startAlarm()
            }
            AlarmReceiver.RC_RESPONSE -> {
                MyLogger.d(TAG, "Response Received")
                val response = Alarm.getById(this@MainActivity, intent.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)) ?: return
                announcerTextView.text = response.response
                val announcer = announcer as MainActivity.Announcer
                announcer.apply {
                    setInterested()
                    talkForResponse(response.response, response)
                }
                startResponse()
            }
        }
    }

    fun startRecording() {
        Log.d("rec Init", "mRecognizer 이 실행됩니다")
        mRecognizer.startListening(_intent) // STT 실행
        announcerTextView.text = getString(R.string.announcer_listen)
    }

    /*
    아나운서 클래스입니다
    애니메이션 부분과 발화(STT) 부분으로 나누어 볼 수 있겠습니다.
    생성 과정에서 애니메이션이 실행되게 하였습니다.
     */
    internal class Announcer : ImageView {
        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

        init {
            setIdle() // 초기 애니메이션: 기본
            process() // 애니메이션 실행
        }

        // 애니메이션 이미지 변경을 담당하는 핸들러
        private val announcerHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                setImageResource(frameList[nowFrame])
            }
        }

        // 애니매이션용 변수
        private lateinit var frameList: Array<Int> // 이미지를 저장한 배열
        private lateinit var frameDelay: Array<Long> // 애니메이션에 해당하는 딜레이를 저장하는 배열
        private var isLoop: Boolean = true // 애니메이션을 반복할 것인지, 아닌지
        private var nextAnimation = ANIMATION_IDLE // 다음 애니메이션을 가리키는 값
        private var nowFrame: Int = 0 // 현재 몇 번째 프레임인지?
        private var nowDelay: Long = 0L // 현재 딜레이는 얼마나 되는지?


        // 스레드 생성 후 애니메이션을 실행하는 함수
        private fun process() {
            Thread(Runnable {
                var preTime: Long = 0 // 과거 이 사진이 언제 변경되었는지? (nowTime 과 비교하여 Delay 만큼 경과했는지 비교하기 위해 사용)
                var nowTime: Long // 현재 시간
                // Log._intent(PROCESS_LOG, "INIT")

                while (true) {
                    nowTime = System.currentTimeMillis()
                    // Log._intent(PROCESS_LOG, "preTime: $preTime, nowTime: $nowTime, nowTime - preTime: ${nowTime - preTime}, nowDelay: $nowDelay")
                    // 지정 딜레이만큼 시간이 지났으면 if 문 실행, 아니면 nowTime 만을 갱신하며 아무것도 하지 않는다
                    if (nowTime - preTime > nowDelay) { // 딜레이만큼 시간 경과
                        // Log._intent(PROCESS_LOG, "nowTime - preTime > nowDelay")
                        if (nowFrame == frameList.size - 1) { // 지금 이미지가 이 애니메이션의 마지막 이미지라면
                            if (isLoop) { // 만약 이 애니메이션을 반복했다고 설정했다면, Frame 을 0 (처음 값) 으로 되돌려 반복시킨다
                                nowFrame = 0
                            } else { // 반복이 설정되어 있지 않다면, nextAnimation 값에 따라 다음 애니메이션을 실행시킨다
                                when (nextAnimation) {
                                    ANIMATION_IDLE -> setIdle()
                                    ANIMATION_LISTEN -> setListen()
                                    ANIMATION_ALARM -> setAlarm()
                                    ANIMATION_BYE -> setBye()
                                    else -> {
                                    }
                                }
                            }
                        } else { // 이 프레임이 애니메이션의 마지막이 아니라면 ( 수행할 프레임이 더 남아있다면) 프레임 값 증가
                            // Log._intent(PROCESS_LOG, "BEFORE nowFrame $nowFrame")
                            nowFrame++
                            // Log._intent(PROCESS_LOG, "AFTER nowFrame $nowFrame")
                        }
                        // 프레임이 변경되었으므로 이후 preTime 재설정, Handler 를 통해 이미지 변경 수행
                        nowDelay = frameDelay[nowFrame]
                        preTime = System.currentTimeMillis()
                        // Log._intent(PROCESS_LOG, "nowFrame BEFORE setImageResource $nowFrame")
                        val msg: Message = announcerHandler.obtainMessage()
                        announcerHandler.sendMessage(msg)
                    }
                }
            }).start()
        }

        // Announcer 의 상태를 Idle 로 설정
        fun setIdle() {
            frameList = arrayOf(
                R.drawable.announcer_idle1, R.drawable.announcer_idle2,
                R.drawable.announcer_idle1, R.drawable.announcer_idle2
            )
            frameDelay = arrayOf(2000L, 200L, 3000L, 200L)

            nowFrame = 0
            nowDelay = frameDelay[nowFrame]

            isLoop = true
        }

        // Announcer 의 상태를 Listen 으로 설정
        fun setListen() {
            frameList = arrayOf(
                R.drawable.announcer_smile1, R.drawable.announcer_idle2,
                R.drawable.announcer_smile1, R.drawable.announcer_idle2
            )
            frameDelay = arrayOf(2000L, 200L, 3000L, 200L)

            nowFrame = 0
            nowDelay = frameDelay[nowFrame]

            isLoop = true

        }

        // Announcer 의 상태를 Alarm 으로 설정
        fun setAlarm() {
            frameList = arrayOf(
                R.drawable.announcer_alarm1, R.drawable.announcer_idle2,
                R.drawable.announcer_alarm1, R.drawable.announcer_idle2
            )
            frameDelay = arrayOf(2000L, 200L, 3000L, 200L)

            nowFrame = 0
            nowDelay = frameDelay[nowFrame]

            isLoop = false

        }

        // Announcer 의 상태를 Interested 로 설정
        fun setInterested() {
            frameList = arrayOf(
                R.drawable.announcer_interested1, R.drawable.announcer_idle2,
                R.drawable.announcer_interested1, R.drawable.announcer_idle2
            )
            frameDelay = arrayOf(2000L, 200L, 3000L, 200L)

            nowFrame = 0
            nowDelay = frameDelay[nowFrame]

            isLoop = false
        }

        private fun setBye() {
        }

        // 발화 (TTS) 를 위한 변수
        private var mediaPlayer: MediaPlayer? = null // 플레이어

        // 발화 (TTS) 함수
        // msg:String (in java; String msg) 를 넣어서 실행하면 된다
        fun talk(msg: String) {
            if (mediaPlayer != null) {
                mediaPlayer!!.stop()
                mediaPlayer = null
            }
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

        fun talkForResponse(msg: String, alarm: Alarm) {
            if (mediaPlayer != null) {
                mediaPlayer!!.stop()
                mediaPlayer = null
            }
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
                setOnCompletionListener {
                    activity?.curResponseAlarm = alarm
                    activity?.startRecording()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // if(음성 녹음 권한이 없다면)
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 권한을 요청합니다
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    // SpeechRecognizer 가 음성을 인식하는 과정에서 호출되는 함수들을 가진 객체입니다.
    // SpeechRecognizer 생성 시 (mRecognizer 객체 생성 시) 사용했습니다.
    private var listener: RecognitionListener = object : RecognitionListener {

        // 버튼 눌렀을 때
        override fun onBeginningOfSpeech() {
            announcerTextView.text = getString(R.string.announcer_listen)
            sttTextView.text = null
            val announcer = announcer as MainActivity.Announcer
            announcer.apply {
                setListen() // 아나운서 상태 Listen 으로 변경
            }
            isSpeaking = true
        }

        // 중간 결과를 받아왔을 때
        override fun onPartialResults(partialResults: Bundle?) {
            var key: String = SpeechRecognizer.RESULTS_RECOGNITION
            val mResult: ArrayList<String>? = partialResults?.getStringArrayList(key)
            if (mResult != null) {
                sttTextView.text = "" + mResult[0]
            } // sttTextView 창에 표현

        }

        // 최종 결과를 받아왔을때
        override fun onResults(results: Bundle?) {
            if (!isSpeaking) return
            isSpeaking = false
            var key: String = SpeechRecognizer.RESULTS_RECOGNITION
            val mResult: ArrayList<String>? = results?.getStringArrayList(key) ?: throw Exception("No Result Found")
            val sMsg = mResult!![0]
            sttTextView.text = sMsg // sttTextView 창에 표현
            Log.i("@@@", sMsg)

            for (resp in RESPONSE_LIST) {
                if (sMsg.contains(resp)) {
                    curResponseAlarm?.responseEnd = true
                    curResponseAlarm?.save(this@MainActivity)
                    Response.add(this@MainActivity, curResponseAlarm!!, sMsg)
                    curResponseAlarm = null
                    break
                }
            }

            if (curResponseAlarm != null && curResponseAlarm!!.isPassedWithNoResponse()) {
                Response.add(this@MainActivity, curResponseAlarm!!, "null")
            }
            startResponse()
//            val sRetRaw = HttpAsyncTask(this@MainActivity).execute(Utils.POST_DIALOG, getJsonFromString(sMsg)).get()
//            val sRet = getStringFromResult(sRetRaw)
//            announcerTextView.text = sRet
//            val announcer = announcer as MainActivity.Announcer
//            announcer.apply {
//                setInterested() // 아나운서 상태 Interested 로 변경
//                talk(sRet)
//            }

            // 참고했습니다: https//dsnight.tistory.com/15
        }

        override fun onEvent(eventType: Int, params: Bundle?) {

        }

        override fun onEndOfSpeech() {
        }

        // 적절하게 인식되지 않았을 때
        override fun onError(error: Int) {
            isSpeaking = false
            Log.i("@@@", "에러: ${error}")
            announcerTextView.text = getString(R.string.announcer_error)
            val announcer = announcer as MainActivity.Announcer
            announcer.apply {
                setAlarm() // 아나운서 상태 Alarm 으로 변경
            }
            startResponse()
        }

        override fun onBufferReceived(buffer: ByteArray?) {
        }

        override fun onReadyForSpeech(params: Bundle?) {
        }

        override fun onRmsChanged(rmsdB: Float) {
        }
    }

    fun getJsonFromString(sMsg: String): String {
        val jMsg = JsonObject()
        jMsg.addProperty("requestType", "dialog");
        jMsg.addProperty("userID", "admin");
        jMsg.addProperty("rawText", sMsg);
        return jMsg.toString()
    }

    fun getStringFromResult(sRet: String): String {
        Log.i("@@@", sRet)
        val jRet = JsonParser.parseString(sRet).asJsonObject
        if (jRet.has("resp")) {
            return (jRet["resp"].asJsonObject
                    ["client_actions"].asJsonObject
                    ["vision"].asJsonArray
                    [0].asJsonObject
                    ["object"].asJsonArray
                    [0].asString)
        }

        return getString(R.string.announcer_error)
    }

    fun startAlarm() {
        val alarm = Alarm.getNextAlarm(this@MainActivity)
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        alarmIntent.putExtra(AlarmReceiver.EXTRA_TYPE, AlarmReceiver.RC_ALARM)
        alarmIntent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm?.id)
        val alarmPendingIntent = PendingIntent.getBroadcast(this, AlarmReceiver.RC_ALARM, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (alarm == null) {
            alarmManager.cancel(alarmPendingIntent)
            tv_next_alarm.text = null
        } else {
            val alarmTriggerTime = alarm.getNextAlarmTime() ?: return
            tempCalendar.timeInMillis = alarmTriggerTime
            tv_next_alarm.text = String.format(getString(R.string.next_fmt), tempCalendar[Calendar.YEAR], tempCalendar[Calendar.MONTH] + 1, tempCalendar[Calendar.DAY_OF_MONTH],
                tempCalendar[Calendar.HOUR_OF_DAY], tempCalendar[Calendar.MINUTE], alarm.message)
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTriggerTime, alarmPendingIntent)
            MyLogger.d("@Alarm@", "${alarmTriggerTime - Date().time}")
        }
    }

    fun startResponse() {
        val response = Alarm.getNextResponse(this@MainActivity)
        val responseIntent = Intent(this, AlarmReceiver::class.java)
        responseIntent.putExtra(AlarmReceiver.EXTRA_TYPE, AlarmReceiver.RC_RESPONSE)
        responseIntent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, response?.id)
        val responsePendingIntent = PendingIntent.getBroadcast(this, AlarmReceiver.RC_RESPONSE, responseIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (response == null) {
            alarmManager.cancel(responsePendingIntent)
            tv_next_response.text = null
        } else {
            val responseTriggerTime = response.getNextResponseTime() ?: return
            tempCalendar.timeInMillis = responseTriggerTime
            tv_next_response.text = String.format(getString(R.string.next_fmt), tempCalendar[Calendar.YEAR], tempCalendar[Calendar.MONTH] + 1, tempCalendar[Calendar.DAY_OF_MONTH],
                tempCalendar[Calendar.HOUR_OF_DAY], tempCalendar[Calendar.MINUTE], response.response)
            alarmManager.set(AlarmManager.RTC_WAKEUP, responseTriggerTime, responsePendingIntent)
            MyLogger.d("@Response@", "${responseTriggerTime - Date().time}")
        }
    }

    override fun onResume() {
        super.onResume()
        startAlarm()
        startResponse()
    }
}

