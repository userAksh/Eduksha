package com.dobi.app.data.local

import android.content.Context

object LanguagePreference {
    private const val PREFS_NAME = "lang_prefs"
    private const val KEY_LANG = "app_language"
    const val LANG_EN = "en"
    const val LANG_HI = "hi"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, LANG_EN) ?: LANG_EN

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, lang).apply()
    }
}
