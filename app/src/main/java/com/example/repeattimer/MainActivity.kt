package com.example.repeattimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var startPauseButton: Button
    private lateinit var stopButton: Button
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var secondPicker: NumberPicker
    private val prefs by lazy { getSharedPreferences("timer", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        timerText = findViewById(R.id.timerText)
        statusText = findViewById(R.id.statusText)
        startPauseButton = findViewById(R.id.startPauseButton)
        stopButton = findViewById(R.id.stopButton)
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        secondPicker = findViewById(R.id.secondPicker)
        setupPicker(hourPicker, 0, 23, prefs.getInt("hour", 0))
        setupPicker(minutePicker, 0, 59, prefs.getInt("minute", 4))
        setupPicker(secondPicker, 0, 59, prefs.getInt("second", 0))
        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            if (!TimerService.running) { saveSettings(); updateTimerText(selectedDurationMs()) }
        }
        hourPicker.setOnValueChangedListener(listener)
        minutePicker.setOnValueChangedListener(listener)
        secondPicker.setOnValueChangedListener(listener)
        startPauseButton.setOnClickListener {
            startTimerService(if (TimerService.running) TimerService.ACTION_PAUSE else TimerService.ACTION_START)
        }
        stopButton.setOnClickListener { startTimerService(TimerService.ACTION_STOP) }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        updateFromService()
    }

    override fun onResume() { super.onResume(); updateFromService() }

    private fun setupPicker(p: NumberPicker, min: Int, max: Int, value: Int) {
        p.minValue = min; p.maxValue = max; p.value = value; p.wrapSelectorWheel = true
        p.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }
    private fun selectedDurationMs() = ((hourPicker.value * 3600L) + (minutePicker.value * 60L) + secondPicker.value) * 1000L
    private fun saveSettings() { prefs.edit().putInt("hour", hourPicker.value).putInt("minute", minutePicker.value).putInt("second", secondPicker.value).apply() }
    private fun startTimerService(action: String) {
        saveSettings()
        val i = Intent(this, TimerService::class.java).apply { this.action = action; putExtra("duration_ms", selectedDurationMs().coerceAtLeast(1000L)) }
        ContextCompat.startForegroundService(this, i)
        updateFromService()
    }
    private fun updateFromService() {
        val remaining = TimerService.currentRemainingMs
        if (remaining > 0) updateTimerText(remaining) else updateTimerText(selectedDurationMs())
        statusText.text = when { TimerService.running -> "반복 실행 중"; TimerService.paused -> "일시정지"; else -> "준비됨" }
        startPauseButton.text = if (TimerService.running) "일시정지" else "시작"
    }
    private fun updateTimerText(ms: Long) {
        val t = (ms + 999L) / 1000L
        timerText.text = "%02d:%02d:%02d".format(t / 3600, (t % 3600) / 60, t % 60)
    }
    override fun onDestroy() { saveSettings(); super.onDestroy() }
}
