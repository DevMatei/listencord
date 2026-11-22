package com.devmatei.listencord.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.devmatei.listencord.Prefs

private enum class Tab(val label: String, val on: ImageVector, val off: ImageVector) {
    NOW("now", Icons.Rounded.GraphicEq, Icons.Outlined.GraphicEq),
    APPS("apps", Icons.Rounded.Apps, Icons.Outlined.Apps),
    PRESENCE("presence", Icons.Rounded.Tune, Icons.Outlined.Tune),
    ART("art", Icons.Rounded.Album, Icons.Outlined.Album),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Screen(prefs: Prefs, granted: Boolean, onGrant: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.NOW) }
    BackHandler(tab != Tab.NOW) { tab = Tab.NOW }

    Scaffold(
        bottomBar = {
            ShortNavigationBar {
                Tab.entries.forEach {
                    ShortNavigationBarItem(
                        selected = tab == it,
                        onClick = { tab = it },
                        icon = { Icon(if (tab == it) it.on else it.off, null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding()).consumeWindowInsets(pad)) {
            // fade through, the m3 pattern for switching between top level tabs
            AnimatedContent(
                tab,
                transitionSpec = {
                    (fadeIn(tween(220, delayMillis = 90)) + scaleIn(tween(220, delayMillis = 90), initialScale = 0.96f)) togetherWith
                        fadeOut(tween(90))
                },
                label = "tab",
            ) {
                when (it) {
                    Tab.NOW -> NowPage(prefs, granted, onGrant) { tab = Tab.PRESENCE }
                    Tab.APPS -> AppsPage(prefs)
                    Tab.PRESENCE -> PresencePage(prefs)
                    Tab.ART -> ArtPage(prefs)
                }
            }
        }
    }
}
