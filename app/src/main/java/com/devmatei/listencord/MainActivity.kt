package com.devmatei.listencord

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import com.devmatei.listencord.ui.AppTheme
import com.devmatei.listencord.ui.Screen

class MainActivity : ComponentActivity() {
    private val granted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        setContent {
            AppTheme {
                Screen(prefs, granted.value, ::openAccess)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        granted.value = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
    }

    private fun openAccess() {
        val component = ComponentName(this, MediaListener::class.java).flattenToString()
        val direct = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component)
        try {
            startActivity(if (Build.VERSION.SDK_INT >= 30) direct else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: Exception) {
            // some roms don't have the detail page
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
