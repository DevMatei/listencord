package com.devmatei.listencord.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.devmatei.listencord.Artwork
import com.devmatei.listencord.Prefs
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtPage(prefs: Prefs, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showArt by remember { mutableStateOf(prefs.showArt) }
    var connected by remember { mutableStateOf(prefs.hasNavidrome) }
    var url by rememberSaveable { mutableStateOf(prefs.ndUrl) }
    var user by rememberSaveable { mutableStateOf(prefs.ndUser) }
    var pass by remember { mutableStateOf("") }
    var ndBusy by remember { mutableStateOf(false) }
    var ndMsg by remember { mutableStateOf<String?>(null) }
    var token by rememberSaveable { mutableStateOf(prefs.lbToken) }
    var lbBusy by remember { mutableStateOf(false) }
    var lbMsg by remember { mutableStateOf<String?>(null) }

    Page("album art", onBack = onBack) {
        Group {
            SwitchItem(0, 1, "show album art", "navidrome is checked first, then listenbrainz", showArt) {
                showArt = it
                prefs.showArt = it
            }
        }

        AnimatedVisibility(showArt) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Source(Icons.Rounded.Dns, "navidrome", if (connected) "connected as ${prefs.ndUser}" else "your own server") {
                    if (connected) {
                        OutlinedButton(
                            onClick = {
                                prefs.ndToken = ""
                                prefs.ndSalt = ""
                                connected = false
                            },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.align(Alignment.End),
                        ) { Text("forget") }
                        return@Source
                    }
                    OutlinedTextField(
                        url, { url = it }, label = { Text("server url") }, singleLine = true,
                        placeholder = { Text("https://music.example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        user, { user = it }, label = { Text("username") }, singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        pass, { pass = it }, label = { Text("password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = MaterialTheme.shapes.large,
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

                Source(Icons.Rounded.Headphones, "listenbrainz", "grab your token from listenbrainz.org/settings") {
                    OutlinedTextField(
                        token, { token = it.trim(); prefs.lbToken = it.trim(); lbMsg = null },
                        label = { Text("user token") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Busy(lbBusy, lbMsg, "check", enabled = token.isNotBlank()) {
                        scope.launch {
                            lbBusy = true
                            lbMsg = Artwork.testListenbrainz(token) ?: "looks good"
                            lbBusy = false
                        }
                    }
                }

                Source(Icons.Rounded.Image, "fallback", "used when nothing's found, leave empty for none") {
                    Field("image url", prefs.fallbackArt) { prefs.fallbackArt = it.trim() }
                }
            }
        }
    }
}

@Composable
private fun Source(icon: ImageVector, title: String, sub: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Badge(icon)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.Busy(busy: Boolean, message: String?, action: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.align(Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (busy) LoadingIndicator(Modifier.size(36.dp))
        else message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false)) }
        Button(onClick = onClick, enabled = enabled && !busy, shapes = ButtonDefaults.shapes()) { Text(action) }
    }
}

private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

private fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray()).hex()
