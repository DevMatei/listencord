package com.devmatei.listencord

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class Prefs(context: Context) {
    val sp: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    private fun str(key: String, def: String = "") = sp.getString(key, def) ?: def
    private fun put(key: String, value: String) = sp.edit { putString(key, value) }
    private fun put(key: String, value: Boolean) = sp.edit { putBoolean(key, value) }
    private fun put(key: String, value: Int) = sp.edit { putInt(key, value) }

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = put("enabled", v)

    var apps: Set<String>
        get() = sp.getStringSet("apps", emptySet())!!
        set(v) = sp.edit { putStringSet("apps", v) }

    // discord
    var appId: String
        get() = str("app_id")
        set(v) = put("app_id", v)
    var name: String
        get() = str("name", "music")
        set(v) = put("name", v)
    var details: String
        get() = str("details", "{title}")
        set(v) = put("details", v)
    var state: String
        get() = str("state", "{artist}")
        set(v) = put("state", v)
    var largeText: String
        get() = str("large_text", "{album}")
        set(v) = put("large_text", v)
    var type: Int
        get() = sp.getInt("type", 2)
        set(v) = put("type", v)
    var display: Int
        get() = sp.getInt("display", 2)
        set(v) = put("display", v)
    var timestamps: Boolean
        get() = sp.getBoolean("timestamps", true)
        set(v) = put("timestamps", v)
    var showPaused: Boolean
        get() = sp.getBoolean("show_paused", false)
        set(v) = put("show_paused", v)
    var links: Boolean
        get() = sp.getBoolean("links", false)
        set(v) = put("links", v)
    var showArt: Boolean
        get() = sp.getBoolean("show_art", true)
        set(v) = put("show_art", v)
    var fallbackArt: String
        get() = str("fallback_art")
        set(v) = put("fallback_art", v)

    // artwork sources
    var ndUrl: String
        get() = str("nd_url")
        set(v) = put("nd_url", v)
    var ndUser: String
        get() = str("nd_user")
        set(v) = put("nd_user", v)
    // we keep the subsonic token + salt, never the password itself
    var ndToken: String
        get() = str("nd_token")
        set(v) = put("nd_token", v)
    var ndSalt: String
        get() = str("nd_salt")
        set(v) = put("nd_salt", v)
    var lbToken: String
        get() = str("lb_token")
        set(v) = put("lb_token", v)

    val hasNavidrome get() = ndUrl.isNotBlank() && ndUser.isNotBlank() && ndToken.isNotBlank()

    companion object {
        val ART_KEYS = setOf("nd_url", "nd_user", "nd_token", "nd_salt", "lb_token")
    }
}
