package com.example.tgforwarder

import android.content.Context

/** Penyimpanan konfigurasi sederhana lewat SharedPreferences. */
object Prefs {
    private const val FILE = "tg_forwarder_prefs"

    fun save(ctx: Context, sourceToken: String, targetToken: String, targetChatId: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("source", sourceToken)
            .putString("target", targetToken)
            .putString("chat", targetChatId)
            .apply()
    }

    fun sourceToken(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("source", "") ?: ""

    fun targetToken(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("target", "") ?: ""

    fun targetChatId(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("chat", "") ?: ""
}
