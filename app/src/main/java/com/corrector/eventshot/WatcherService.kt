package com.corrector.eventshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WatcherService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_EVENT = "event"
        private const val CHANNEL = "eventshot"
        private val MEDIA_EXT = setOf("jpg", "jpeg", "png", "heic", "heif", "webp",
            "dng", "mp4", "3gp", "mkv", "webm", "mov")
    }

    private var eventName: String = ""
    private var sinceMs: Long = 0L
    private var counter = 0
    private val handler = Handler(Looper.getMainLooper())
    private val processed = HashSet<String>()
    private var observer: ContentObserver? = null

    private val sweepRunnable = object : Runnable {
        override fun run() {
            sweep()
            handler.postDelayed(this, 4000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                eventName = intent.getStringExtra(EXTRA_EVENT) ?: return START_NOT_STICKY
                sinceMs = System.currentTimeMillis()
                counter = 0
                processed.clear()
                startForeground(1, buildNotification())
                registerObserver()
                handler.removeCallbacks(sweepRunnable)
                handler.postDelayed(sweepRunnable, 4000)
            }
            ACTION_STOP -> {
                unregisterObserver()
                handler.removeCallbacks(sweepRunnable)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterObserver()
        handler.removeCallbacks(sweepRunnable)
        super.onDestroy()
    }

    private fun registerObserver() {
        if (observer != null) return
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(debounced)
                handler.postDelayed(debounced, 2000)
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"), true, observer!!)
    }

    private val debounced = Runnable { sweep() }

    private fun unregisterObserver() {
        observer?.let { contentResolver.unregisterContentObserver(it) }
        observer = null
    }

    private fun sweep() {
        if (eventName.isEmpty()) return
        try {
            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (!dcim.exists()) return
            dcim.walkTopDown().maxDepth(3).forEach { f ->
                if (f.isFile) maybeMove(f)
            }
        } catch (_: Exception) { }
    }

    private fun maybeMove(f: File) {
        val path = f.absolutePath
        if (processed.contains(path)) return
        val ext = f.extension.lowercase()
        if (ext !in MEDIA_EXT) return
        if (f.name.startsWith(".") || f.name.startsWith(eventName + "_")) return
        if (f.lastModified() < sinceMs) return

        // skip files still being written (e.g. video recording in progress)
        val s1 = f.length()
        try { Thread.sleep(700) } catch (_: Exception) { }
        if (!f.exists() || f.length() != s1 || s1 == 0L) return

        val destDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), eventName)
        if (!destDir.exists()) destDir.mkdirs()

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(f.lastModified()))
        var dest = File(destDir, "${eventName}_${stamp}.$ext")
        var n = 1
        while (dest.exists()) {
            dest = File(destDir, "${eventName}_${stamp}_$n.$ext")
            n++
        }

        if (f.renameTo(dest)) {
            counter++
            processed.add(path)
            MediaScannerConnection.scanFile(this,
                arrayOf(dest.absolutePath, path), null, null)
            updateNotification()
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL,
                "Event Mode", NotificationManager.IMPORTANCE_LOW))
        }
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("EVENT MODE: $eventName")
            .setContentText("$counter file(s) sorted into Pictures/$eventName")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(1, buildNotification())
    }
}
