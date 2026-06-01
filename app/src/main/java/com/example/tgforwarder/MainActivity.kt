package com.example.tgforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var sourceToken: EditText
    private lateinit var targetToken: EditText
    private lateinit var targetChatId: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* lanjut apa pun hasilnya */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sourceToken = findViewById(R.id.sourceToken)
        targetToken = findViewById(R.id.targetToken)
        targetChatId = findViewById(R.id.targetChatId)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)

        // Muat konfigurasi tersimpan
        sourceToken.setText(Prefs.sourceToken(this))
        targetToken.setText(Prefs.targetToken(this))
        targetChatId.setText(Prefs.targetChatId(this))

        startButton.setOnClickListener { startForwarder() }
        stopButton.setOnClickListener { stopForwarder() }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // Tampilkan log yang sudah ada dan dengarkan log baru
        logText.text = ForwarderState.snapshot().joinToString("\n")
        ForwarderState.listener = { line ->
            runOnUiThread {
                logText.append(if (logText.text.isEmpty()) line else "\n$line")
            }
        }
        refreshStatus()
    }

    override fun onPause() {
        super.onPause()
        ForwarderState.listener = null
    }

    private fun startForwarder() {
        val src = sourceToken.text.toString().trim()
        val chat = targetChatId.text.toString().trim()
        if (src.isEmpty() || chat.isEmpty()) {
            statusText.text = "Status: isi token sumber dan tujuan dulu"
            return
        }

        Prefs.save(
            this, src,
            targetToken.text.toString().trim(),
            chat
        )

        val intent = Intent(this, ForwarderService::class.java)
        ContextCompat.startForegroundService(this, intent)
        refreshStatusDelayed()
    }

    private fun stopForwarder() {
        val intent = Intent(this, ForwarderService::class.java).apply {
            action = ForwarderService.ACTION_STOP
        }
        startService(intent)
        refreshStatusDelayed()
    }

    private fun refreshStatus() {
        val running = ForwarderState.isRunning
        statusText.text = if (running) "Status: berjalan" else "Status: berhenti"
        startButton.isEnabled = !running
        stopButton.isEnabled = running
    }

    private fun refreshStatusDelayed() {
        statusText.postDelayed({ refreshStatus() }, 400)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
