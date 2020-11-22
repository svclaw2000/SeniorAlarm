package ant.swcapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ant.swcapp.data.Alarm
import ant.swcapp.utils.Extras
import kotlinx.android.synthetic.main.activity_alarm_list.*

class AlarmListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_list)

        btn_add.setOnClickListener {
            val intent = Intent(this@AlarmListActivity, AlarmActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val alarms = Alarm.getAll(this@AlarmListActivity)
        val adapter = AlarmRecyclerAdapter(alarms)
        val lm = LinearLayoutManager(this@AlarmListActivity)
        alarm_container.layoutManager = lm
        alarm_container.adapter = adapter
    }

    inner class AlarmRecyclerAdapter(val lAlarm : Array<Alarm>) :
            RecyclerView.Adapter<AlarmRecyclerAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val container = itemView.findViewById<LinearLayout>(R.id.ll_alarm)
            val tv_time = itemView.findViewById<TextView>(R.id.tv_time)
            val sw_alarm = itemView.findViewById<Switch>(R.id.sw_alarm)
            val tv_message = itemView.findViewById<TextView>(R.id.tv_message)
            val tv_repeat = itemView.findViewById<TextView>(R.id.tv_repeat)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(this@AlarmListActivity).inflate(R.layout.item_alarm_list, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return lAlarm.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val alarm = lAlarm[position]
            holder.tv_time.text = alarm.getTime().getString()
            holder.sw_alarm.isChecked = alarm.getIsEnabled()
            holder.tv_message.text = alarm.message
            holder.tv_repeat.text = "${alarm.repeat.getString()} / ${alarm.repeatTime}분 / ${alarm.repeatCount}회"

            holder.sw_alarm.setOnCheckedChangeListener { buttonView, isChecked ->
                alarm.setIsEnabled(isChecked)
                alarm.save(this@AlarmListActivity)
            }

            holder.container.setOnLongClickListener {
                val items = arrayOf("알람 수정", "알람 삭제")
                AlertDialog.Builder(this@AlarmListActivity)
                    .setTitle(alarm.getTime().getString())
                    .setItems(items) { dialog, which ->
                        when (which) {
                            0 -> {
                                val intent = Intent(this@AlarmListActivity, AlarmActivity::class.java)
                                intent.putExtra(Extras.ALARM_ID.name, alarm.id)
                                startActivity(intent)
                            }
                            1 -> {
                                AlertDialog.Builder(this@AlarmListActivity)
                                    .setTitle("알람 삭제")
                                    .setMessage("${alarm.getTime().getString()}의 알람을 삭제하시겠습니까?")
                                    .setPositiveButton("삭제") { _, _ ->
                                        alarm.delete(this@AlarmListActivity)
                                        refresh()
                                    }
                                    .setNegativeButton("취소", null)
                                    .show()
                            }
                        }
                    }
                    .show()
                false
            }
        }
    }
}