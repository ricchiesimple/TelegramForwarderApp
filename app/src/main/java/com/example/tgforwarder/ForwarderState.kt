package com.example.tgforwarder

import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * State yang dibagikan antara Service dan Activity (proses sama).
 * Menyimpan status berjalan/tidak dan buffer log terakhir.
 */
object ForwarderState {

    @Volatile
    var isRunning = false

    private val logs = ArrayDeque<String>()
    private const val MAX_LOGS = 200
    private val main = Handler(Looper.getMainLooper())
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /** Dipanggil Activity untuk menerima baris log baru secara real-time. */
    var listener: ((String) -> Unit)? = null

    fun log(line: String) {
        val stamped = "${timeFmt.format(Date())}  $line"
        synchronized(logs) {
            logs.addLast(stamped)
            while (logs.size > MAX_LOGS) logs.removeFirst()
        }
        main.post { listener?.invoke(stamped) }
    }

    fun snapshot(): List<String> = synchronized(logs) { logs.toList() }

    fun clear() = synchronized(logs) { logs.clear() }
}
