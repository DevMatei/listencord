package com.devmatei.listencord.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.Artwork
import com.devmatei.listencord.NowPlaying
import com.devmatei.listencord.Prefs
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Screen(prefs: Prefs, granted: Boolean, onGrant: () -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val status by NowPlaying.status.collectAsState()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("listencord") },
                subtitle = { Text(status) },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (!granted) AccessCard(onGrant)
            NowPlayingCard()
            General(prefs)
            Discord(prefs)
            Art(prefs)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccessCard(onGrant: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("needs notification access", style = MaterialTheme.typography.titleMedium)
            Text(
                "android only lets apps see what's playing through this. nothing from your notifications is read or kept. " +
                    "if it's greyed out, go to app info, tap the three dots and allow restricted settings first.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onGrant, shapes = ButtonDefaults.shapes()) { Text("allow") }
        }
    }
}

@Composable
private fun NowPlayingCard() {
    val track by NowPlaying.track.collectAsState()
    val art by NowPlaying.art.collectAsState()

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val t = track
            if (t == null) {
                Text("nothing playing", style = MaterialTheme.typography.titleLarge)
                Text("play something in one of your picked apps", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Text(t.title, style = MaterialTheme.typography.titleLarge)
            val sub = listOf(t.artist, t.album).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${if (t.playing) "playing" else "paused"} in ${t.app}, ${if (art != null) "with art" else "no art"}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun General(prefs: Prefs) {
    val ctx = LocalContext.current
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var apps by remember { mutableStateOf(prefs.apps) }
    var picking by remember { mutableStateOf(false) }
    val names = remember(apps) { apps.map { appLabel(ctx, it) }.sorted().joinToString(", ") }

    Section("general") {
        Toggle("show on discord", null, enabled) { enabled = it; prefs.enabled = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("apps", style = MaterialTheme.typography.bodyLarge)
                Text(
                    names.ifEmpty { "none picked yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { picking = true }) { Text("pick") }
        }
    }

    if (picking) {
        AppPicker(apps) {
            apps = it
            prefs.apps = it
            picking = false
        }
    }
}

@Composable
private fun Discord(prefs: Prefs) {
    var type by remember { mutableStateOf(prefs.type) }
    var display by remember { mutableStateOf(prefs.display) }
    var timestamps by remember { mutableStateOf(prefs.timestamps) }
    var paused by remember { mutableStateOf(prefs.showPaused) }
    var links by remember { mutableStateOf(prefs.links) }

    Section("discord") {
        Field(
            "application id", prefs.appId,
            hint = "make an app at discord.com/developers and paste its id",
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
        ) { prefs.appId = it }

        Text(
            "you can use {title} {artist} {album} {app} in these",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Field("name", prefs.name) { prefs.name = it }
        Field("first line", prefs.details) { prefs.details = it }
        Field("second line", prefs.state) { prefs.state = it }
        Field("album art hover text", prefs.largeText) { prefs.largeText = it }

        Label("activity")
        Choice(listOf(2 to "listening", 0 to "playing", 3 to "watching", 5 to "competing"), type) {
            type = it
            prefs.type = it
        }

        Label("member list shows")
        Choice(listOf(0 to "name", 2 to "first line", 1 to "second line"), display) {
            display = it
            prefs.display = it
        }

        Toggle("progress bar", "shows time left on the song", timestamps) { timestamps = it; prefs.timestamps = it }
        Toggle("show while paused", null, paused) { paused = it; prefs.showPaused = it }
        Toggle("last.fm links", "makes the song and artist clickable", links) { links = it; prefs.links = it }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Art(prefs: Prefs) {
    val scope = rememberCoroutineScope()
    var showArt by remember { mutableStateOf(prefs.showArt) }
    var connected by remember { mutableStateOf(prefs.hasNavidrome) }
    var url by rememberSaveable { mutableStateOf(prefs.ndUrl) }
    var user by rememberSaveable { mutableStateOf(prefs.ndUser) }
    var pass by remember { mutableStateOf("") }
    var ndBusy by remember { mutableStateOf(false) }
    var ndMsg by remember { mutableStateOf<String?>(null) }
    var lbBusy by remember { mutableStateOf(false) }
    var lbMsg by remember { mutableStateOf<String?>(null) }

    Section("album art") {
        Toggle("album art", null, showArt) { showArt = it; prefs.showArt = it }

        Label("navidrome", "checked first if it's set up")
        if (connected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "connected as ${prefs.ndUser}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    prefs.ndToken = ""
                    prefs.ndSalt = ""
                    connected = false
                }) { Text("forget") }
            }
        } else {
            OutlinedTextField(
                url, { url = it }, label = { Text("server url") }, singleLine = true,
                placeholder = { Text("https://music.example.com") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                user, { user = it }, label = { Text("username") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                pass, { pass = it }, label = { Text("password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Busy(ndBusy, ndMsg, "connect", enabled = url.isNotBlank() && user.isNotBlank() && pass.isNotEmpty()) {
                scope.launch {
                    ndBusy = true
                    val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }.hex()
                    prefs.ndUrl = url.trim().trimEnd('/')
                    prefs.ndUser = user.trim()
                    prefs.ndSalt = salt
                    prefs.ndToken = md5(pass + salt)
                    val err = Artwork.testNavidrome(prefs)
                    if (err == null) {
                        pass = ""
                        connected = true
                        ndMsg = null
                    } else {
                        prefs.ndToken = ""
                        ndMsg = err
                    }
                    ndBusy = false
                }
            }
        }

        Label("listenbrainz", "grab your token from listenbrainz.org/settings")
        var token by rememberSaveable { mutableStateOf(prefs.lbToken) }
        OutlinedTextField(
            token, { token = it.trim(); prefs.lbToken = it.trim(); lbMsg = null },
            label = { Text("user token") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Busy(lbBusy, lbMsg, "check", enabled = token.isNotBlank()) {
            scope.launch {
                lbBusy = true
                lbMsg = Artwork.testListenbrainz(token) ?: "looks good"
                lbBusy = false
            }
        }

        Field("fallback image url", prefs.fallbackArt, hint = "used when nothing's found, leave empty for the app icon") {
            prefs.fallbackArt = it.trim()
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun Label(text: String, hint: String? = null) {
    Column(Modifier.padding(top = 4.dp)) {
        Text(text, style = MaterialTheme.typography.titleSmall)
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Toggle(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            hint?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Field(
    label: String,
    initial: String,
    hint: String? = null,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it; onSave(it) },
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = keyboard,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Choice(options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { i, (value, label) ->
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Busy(busy: Boolean, message: String?, action: String, enabled: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onClick, enabled = enabled && !busy, shapes = ButtonDefaults.shapes()) { Text(action) }
        if (busy) LoadingIndicator(Modifier.size(32.dp))
        else message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

private fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray()).hex()
