package com.example.timerapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ProgressBar
import android.widget.TextView
import com.example.timerapp.util.PrefUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class MainActivity : AppCompatActivity() {

    enum class TimerState {
        Stopped, Paused, Running
    }

    companion object  {
        fun setAlarm(context: Context, nowseconds: Long, secondsRemaining: Long): Long  {
            val  wakeUpTime = (nowseconds + secondsRemaining)  *  1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent  = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent  = PendingIntent.getBroadcast(context, 0, intent,   0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowseconds,context)
            return wakeUpTime
        }

        fun removeAlarm (context: Context) {
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent  = PendingIntent.getBroadcast(context, 0, intent,   0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        val nowSeconds: Long
            get()  = Calendar.getInstance().timeInMillis / 1000
    }

    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds = 0L
    private var timerState = TimerState.Stopped
    private var secondsRemaining = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val fabStart = findViewById<FloatingActionButton>(R.id.fab_play)
        val fabPause = findViewById<FloatingActionButton>(R.id.fab_pause)
        val fabStop = findViewById<FloatingActionButton>(R.id.fab_stop)

        val timeText = findViewById<TextView>(R.id.textView_countDown)

        val progressBar   =  findViewById<ProgressBar>(R.id.progressBar_timer)


        timeText.text = progressBar.progress.toString()


        fabStart.setOnClickListener { v->
            startTimer()
            timerState = TimerState.Running
            updateButtons()

        }

        fabPause.setOnClickListener { v->
            timer.cancel()
            timerState = TimerState.Paused
            updateButtons()
        }

        fabStop.setOnClickListener{ v->
            timer.cancel()
            onTimerFinished()
        }

    }

    override fun onResume() {
        super.onResume()

        initTimer()

        removeAlarm(this)
    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running) {
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
        }
        else if (timerState == TimerState.Paused)  {
            //Notification
        }
        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun initTimer() {
       timerState = PrefUtil.getTimerState(this)
        if (timerState ==  TimerState.Stopped) {
            setNewTimerLength()
        } else {
            setPreviousTimerLength()
            secondsRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
                PrefUtil.getSecondsRemaining(this)
            else
                timerLengthSeconds

            val alarmSetTime = PrefUtil.getAlarmSetTime(this)
            if (alarmSetTime > 0) {
                secondsRemaining  -= nowSeconds - alarmSetTime
            }
            if (secondsRemaining  <= 0){
                onTimerFinished()
            }

            else if (timerState == TimerState.Running) {
                startTimer()
                updateButtons()
                updateCountDownUi()
            }
        }
    }

    private fun updateCountDownUi() {
        val timeText = findViewById<TextView>(R.id.textView_countDown)
        val progressBar   =  findViewById<ProgressBar>(R.id.progressBar_timer)
        val minutesUntilFinished = secondsRemaining /60
        val secondsInMinuteUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsStr =  secondsInMinuteUntilFinished.toString()
        timeText.text = "$minutesUntilFinished:${
            if (secondsStr.length  == 2) secondsStr
        else "0" + secondsStr}"


        progressBar.progress = (timerLengthSeconds - secondsRemaining).toInt()


    }

    private fun setPreviousTimerLength() {
        val progressBar   =  findViewById<ProgressBar>(R.id.progressBar_timer)
        timerLengthSeconds =  PrefUtil.getPreviousTimerLengthSeconds(this)
        progressBar.max =  timerLengthSeconds.toInt()
    }

    private fun setNewTimerLength() {
        val progressBar   =  findViewById<ProgressBar>(R.id.progressBar_timer)
        val lengthInMinutes  = PrefUtil.getTimerLength(this)
        timerLengthSeconds  = (lengthInMinutes * 60L)
        progressBar.max =  timerLengthSeconds.toInt()

    }

    private fun onTimerFinished() {
        val progressBar   =  findViewById<ProgressBar>(R.id.progressBar_timer)
        timerState =  TimerState.Stopped

        setNewTimerLength()

        progressBar.progress =  0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds
        updateButtons()
        updateCountDownUi()

    }

    private fun updateButtons() {

        val fabStart = findViewById<FloatingActionButton>(R.id.fab_play)
        val fabPause = findViewById<FloatingActionButton>(R.id.fab_pause)
        val fabStop = findViewById<FloatingActionButton>(R.id.fab_stop)

        when (timerState) {
            TimerState.Running -> {
                fabStart.isEnabled =  false
                fabPause.isEnabled = true
                fabStop.isEnabled =  true
            }
            TimerState.Stopped -> {
                fabStart.isEnabled =  true
                fabPause.isEnabled = false
                fabStop.isEnabled =  false
            }
            TimerState.Paused ->  {
                fabStart.isEnabled =  true
                fabPause.isEnabled = false
                fabStop.isEnabled =  true
            }
        }
    }

    private fun startTimer() {
        timerState = TimerState.Running

        timer  =  object : CountDownTimer(secondsRemaining  * 1000, 1000) {
            override fun onFinish() =  onTimerFinished()

            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountDownUi()
            }
        }.start()
    }
}