package com.example.tgforwarder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service yang berjalan di latar depan (foreground) dan terus memeriksa
 * pesan baru ke bot sumber, lalu meneruskannya ke tujuan.
 */
class ForwarderService : Service() {

    companion object {
        const val ACTION_STOP = "com.example.tgforwarder.STOP"
        private const val CHANNEL_ID = "forwarder_channel"
        private const val NOTIF_ID = 1
        private const val POLL_TIMEOUT = 30 // detik
        private const val API = "https://api.telegram.org/bot%s/%s"
    }

    @Volatile
    private var running = false
    private var worker: Thread? = null

    private lateinit var sourceToken: String
    private lateinit var targetToken: String
    private lateinit var targetChatId: String

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForwarding()
            stopSelf()
            return START_NOT_STICKY
        }

        sourceToken = Prefs.sourceToken(this).trim()
        targetToken = Prefs.targetToken(this).trim().ifEmpty { sourceToken }
        targetChatId = Prefs.targetChatId(this).trim()

        startForegroundWithNotification()
        startForwarding()
        return START_STICKY
    }

    // ------------------------------------------------------------------
    // Notifikasi foreground
    // ------------------------------------------------------------------
    private fun startForegroundWithNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Forwarder", NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val tapIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif: Notification = Notification.Builder(this).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
            setContentTitle("Telegram Forwarder aktif")
            setContentText("Mendengarkan pesan masuk...")
            setSmallIcon(R.drawable.ic_launcher)
            setOngoing(true)
            setContentIntent(tapIntent)
        }.build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    // ------------------------------------------------------------------
    // Loop polling
    // ------------------------------------------------------------------
    private fun startForwarding() {
        if (running) return

        if (sourceToken.isEmpty() || targetChatId.isEmpty()) {
            ForwarderState.log("Konfigurasi belum lengkap. Berhenti.")
            stopSelf()
            return
        }

        running = true
        ForwarderState.isRunning = true
        ForwarderState.log("Mulai mendengarkan pesan masuk...")

        worker = Thread { pollLoop() }.also { it.start() }
    }

    private fun stopForwarding() {
        running = false
        ForwarderState.isRunning = false
        worker?.interrupt()
        worker = null
        ForwarderState.log("Forwarder dihentikan.")
    }

    private fun pollLoop() {
        var offset: Long? = null
        while (running) {
            val updates = getUpdates(offset)
            if (updates == null) {
                sleepQuietly(3000)
                continue
            }
            for (i in 0 until updates.length()) {
                if (!running) break
                val upd = updates.getJSONObject(i)
                offset = upd.getLong("update_id") + 1

                val msg = upd.optJSONObject("message")
                    ?: upd.optJSONObject("channel_post") ?: continue

                val preview = (msg.optString("text").ifEmpty { msg.optString("caption") }
                    .ifEmpty { "[media]" }).take(50)
                ForwarderState.log("Pesan masuk: $preview")

                if (relay(msg)) ForwarderState.log("  -> diteruskan ke $targetChatId")
                else ForwarderState.log("  -> GAGAL meneruskan")
            }
        }
    }

    // ------------------------------------------------------------------
    // Pemanggilan Telegram API
    // ------------------------------------------------------------------
    private fun callApi(token: String, method: String, params: JSONObject): JSONObject? {
        return try {
            val conn = (URL(API.format(token, method)).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = (POLL_TIMEOUT + 15) * 1000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(params.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(text)
            if (!json.optBoolean("ok")) {
                ForwarderState.log("API menolak $method: ${json.optString("description")}")
                null
            } else json
        } catch (e: Exception) {
            ForwarderState.log("Gangguan saat $method: ${e.message}")
            null
        }
    }

    private fun getUpdates(offset: Long?): JSONArray? {
        val params = JSONObject().apply {
            offset?.let { put("offset", it) }
            put("timeout", POLL_TIMEOUT)
            put("allowed_updates", JSONArray(listOf("message", "channel_post")))
        }
        val res = callApi(sourceToken, "getUpdates", params) ?: return null
        return res.optJSONArray("result")
    }

    private fun relay(msg: JSONObject): Boolean {
        val sameBot = targetToken == sourceToken

        // Bot sama: copyMessage menyalin semua jenis konten dengan rapi.
        if (sameBot) {
            val params = JSONObject().apply {
                put("chat_id", targetChatId)
                put("from_chat_id", msg.getJSONObject("chat").get("id"))
                put("message_id", msg.getInt("message_id"))
            }
            if (callApi(targetToken, "copyMessage", params) != null) return true
        }

        // Lintas-bot atau fallback: kirim ulang sebagai teks.
        val params = JSONObject().apply {
            put("chat_id", targetChatId)
            put("text", buildText(msg))
            put("parse_mode", "HTML")
            put("disable_web_page_preview", true)
        }
        return callApi(targetToken, "sendMessage", params) != null
    }

    private fun buildText(msg: JSONObject): String {
        val from = msg.optJSONObject("from")
        val name = buildString {
            from?.optString("first_name")?.let { if (it.isNotEmpty()) append(it) }
            from?.optString("last_name")?.let { if (it.isNotEmpty()) append(" $it") }
            from?.optString("username")?.let { if (it.isNotEmpty()) append(" (@$it)") }
        }.trim().ifEmpty { "Tidak diketahui" }

        var body = msg.optString("text").ifEmpty { msg.optString("caption") }
        if (body.isEmpty()) {
            val kind = listOf("photo", "document", "sticker", "video", "voice", "audio")
                .firstOrNull { msg.has(it) } ?: "media"
            body = "[$kind tanpa teks]"
        }

        return "📩 <b>Notifikasi baru</b>\nDari: ${escape(name)}\n\n${escape(body)}"
    }

    private fun escape(s: String) =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun sleepQuietly(ms: Long) {
        try {
            Thread.sleep(ms)
        } catch (_: InterruptedException) {
        }
    }

    override fun onDestroy() {
        stopForwarding()
        super.onDestroy()
    }
}
