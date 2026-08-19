package com.example.repeattimer

import android.app.*
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import androidx.core.app.NotificationCompat
import java.util.Locale
import kotlin.math.max

class TimerService : Service() {
    companion object {
        const val ACTION_START = "START"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_STOP = "STOP"
        private const val CHANNEL_ID = "repeat_timer"
        private const val NOTIFICATION_ID = 1001
        @Volatile var running = false
        @Volatile var paused = false
        @Volatile var currentRemainingMs = 0L
    }
    private val handler = Handler(Looper.getMainLooper())
    private var lastTick = 0L
    private var durationMs = 240000L
    private var tone: ToneGenerator? = null
    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            val now = SystemClock.elapsedRealtime()
            currentRemainingMs -= now - lastTick
            lastTick = now
            if (currentRemainingMs <= 0L) { beepThreeTimes(); currentRemainingMs = durationMs.coerceAtLeast(1000L) }
            updateNotification()
            handler.postDelayed(this, 200L)
        }
    }
    override fun onCreate() {
        super.onCreate(); createNotificationChannel(); startForeground(NOTIFICATION_ID, notification("준비됨"))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                durationMs = intent.getLongExtra("duration_ms", 240000L).coerceAtLeast(1000L)
                if (!running && !paused) currentRemainingMs = durationMs
                if (currentRemainingMs <= 0L) currentRemainingMs = durationMs
                running = true; paused = false; lastTick = SystemClock.elapsedRealtime()
                handler.removeCallbacks(ticker); handler.post(ticker); updateNotification()
            }
            ACTION_PAUSE -> { running = false; paused = true; handler.removeCallbacks(ticker); updateNotification() }
            ACTION_STOP -> { running = false; paused = false; handler.removeCallbacks(ticker); currentRemainingMs = durationMs; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
        return START_STICKY
    }
    private fun createNotificationChannel() {
        val c = NotificationChannel(CHANNEL_ID, "반복 타이머", NotificationManager.IMPORTANCE_LOW)
        c.description = "운동 반복 타이머 백그라운드 실행 알림"; c.setSound(null, null)
        getSystemService(NotificationManager::class.java).createNotificationChannel(c)
    }
    private fun notification(text: String): Notification {
        val i = Intent(this, TimerService::class.java).apply { action = ACTION_STOP }
        val p = PendingIntent.getService(this, 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentTitle("반복 타이머").setContentText(text).setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "중지", p).build()
    }
    private fun updateNotification() {
        val t = max(0L, (currentRemainingMs + 999L) / 1000L)
        val text = String.format(Locale.US, "%02d:%02d:%02d · %s", t / 3600, (t % 3600) / 60, t % 60, if (running) "실행 중" else "일시정지")
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }
    private fun beepThreeTimes() {
        tone?.release(); tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        handler.postDelayed({ tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 180) }, 320)
        handler.postDelayed({ tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 180) }, 640)
    }
    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { running = false; handler.removeCallbacksAndMessages(null); tone?.release(); tone = null; super.onDestroy() }
}
