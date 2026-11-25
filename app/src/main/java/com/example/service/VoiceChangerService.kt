package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.VoiceChangerEngine
import com.example.data.model.VoicePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VoiceChangerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObserverJob: Job? = null
    private lateinit var engine: VoiceChangerEngine

    override fun onCreate() {
        super.onCreate()
        engine = VoiceChangerEngine.getInstance(this)
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                engine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SELECT_PRESET -> {
                val presetId = intent.getStringExtra(EXTRA_PRESET_ID)
                val preset = VoicePreset.fromId(presetId)
                engine.selectPreset(preset)
                if (!engine.state.value.isRunning) {
                    if (!startForegroundAndEngine()) return START_NOT_STICKY
                } else {
                    updateNotification(preset)
                }
            }
            else -> {
                if (!startForegroundAndEngine()) return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun startForegroundAndEngine(): Boolean {
        if (!engine.hasRecordPermission()) {
            stopSelf()
            return false
        }

        val notification = buildNotification(engine.state.value.activePreset)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: SecurityException) {
            engine.stop()
            stopSelf()
            return false
        }

        val started = engine.start()
        if (!started) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return false
        }

        observeEngineState()
        return true
    }

    private fun observeEngineState() {
        stateObserverJob?.cancel()
        var wasRunning = false
        stateObserverJob = serviceScope.launch {
            engine.state.collectLatest { state ->
                if (state.isRunning) {
                    wasRunning = true
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, buildNotification(state.activePreset))
                } else if (wasRunning) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun updateNotification(preset: VoicePreset) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(preset))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_service_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(preset: VoicePreset): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, MainActivity.ROUTE_VOICE_STUDIO)
        }
        val openPending = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceChangerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ARIF Voice DSP Active • ${preset.displayName}")
            .setContentText("Microphone processing locally on-device. Tap Stop Mic anytime.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, getString(R.string.notification_action_stop_mic), stopPending)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        stateObserverJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "arif_voice_dsp_channel"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.action.START_VOICE_DSP"
        const val ACTION_STOP = "com.example.action.STOP_VOICE_DSP"
        const val ACTION_SELECT_PRESET = "com.example.action.SELECT_VOICE_PRESET"
        const val EXTRA_PRESET_ID = "extra_preset_id"

        fun start(context: Context) {
            val intent = Intent(context, VoiceChangerService::class.java).apply {
                action = ACTION_START
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceChangerService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(intent) }
        }

        fun selectPresetAndStart(context: Context, preset: VoicePreset) {
            val intent = Intent(context, VoiceChangerService::class.java).apply {
                action = ACTION_SELECT_PRESET
                putExtra(EXTRA_PRESET_ID, preset.id)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }
    }
}
