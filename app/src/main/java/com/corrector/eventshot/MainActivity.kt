package com.corrector.eventshot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etEvent: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etEvent = findViewById(R.id.etEvent)
        tvStatus = findViewById(R.id.tvStatus)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener { startEvent() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopEvent() }
        findViewById<Button>(R.id.btnCamera).setOnClickListener { openCamera() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun hasAllFiles(): Boolean = Environment.isExternalStorageManager()

    private fun askAllFiles() {
        Toast.makeText(this, "Allow \"All files access\" for Event Shot, then come back", Toast.LENGTH_LONG).show()
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private fun startEvent() {
        val name = etEvent.text.toString().trim().uppercase()
            .replace(Regex("[^A-Z0-9 _-]"), "").replace(Regex("\\s+"), "_")
        if (name.isEmpty()) {
            Toast.makeText(this, "Type an event name first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasAllFiles()) { askAllFiles(); return }

        val i = Intent(this, WatcherService::class.java)
            .setAction(WatcherService.ACTION_START)
            .putExtra(WatcherService.EXTRA_EVENT, name)
        startForegroundService(i)

        getSharedPreferences("es", MODE_PRIVATE).edit()
            .putString("event", name)
            .putLong("since", System.currentTimeMillis())
            .apply()

        refreshStatus()
        Toast.makeText(this, "EVENT MODE ON: $name", Toast.LENGTH_SHORT).show()
        openCamera()
    }

    private fun stopEvent() {
        startService(Intent(this, WatcherService::class.java).setAction(WatcherService.ACTION_STOP))
        getSharedPreferences("es", MODE_PRIVATE).edit().remove("event").remove("since").apply()
        refreshStatus()
        Toast.makeText(this, "Event Mode stopped", Toast.LENGTH_SHORT).show()
    }

    private fun openCamera() {
        try {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        } catch (e: Exception) {
            Toast.makeText(this, "Open your camera app manually", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshStatus() {
        val ev = getSharedPreferences("es", MODE_PRIVATE).getString("event", null)
        tvStatus.text = if (ev != null)
            "EVENT ACTIVE: $ev\nUse your normal camera. Every photo/video is\nauto-moved to Pictures/$ev/ within seconds."
        else
            "No event active.\nType a name and press START EVENT."
        if (ev != null) etEvent.setText(ev)
    }
}
