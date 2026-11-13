package com.grimm.kin.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.grimm.kin.data.Library
import com.grimm.kin.data.Security
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val PIN_LEN = 4

/** 4-dot PIN entry with an on-screen keypad. Calls onComplete when 4 digits are entered. */
@Composable
fun PinPad(title: String, subtitle: String?, error: String?, onComplete: (String) -> Unit) {
    var entry by remember { mutableStateOf("") }
    LaunchedEffect(entry) {
        if (entry.length == PIN_LEN) {
            delay(120)
            val e = entry
            entry = ""
            onComplete(e)
        }
    }
    Column(
        Modifier.fillMaxSize().background(Bg).systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Lock, null, tint = Accent, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(PIN_LEN) { i ->
                Box(
                    Modifier.size(14.dp).clip(CircleShape)
                        .background(if (i < entry.length) Accent else MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(error ?: " ", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(26.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(vertical = 9.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (k.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(enabled = k.isNotEmpty()) {
                                entry = when (k) {
                                    "⌫" -> entry.dropLast(1)
                                    else -> if (entry.length < PIN_LEN) entry + k else entry
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(k, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val activity = LocalContext.current as? Activity
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler { activity?.moveTaskToBack(true) }
    // Swallow touches so nothing underneath is clickable.
    Box(
        Modifier.fillMaxSize().background(Bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        PinPad(title = "Kin is locked", subtitle = "Enter your PIN", error = error) { pin ->
            if (Security.check(pin)) onUnlock() else error = "Wrong PIN, try again"
        }
    }
}

/** Two-step flow: enter new PIN, confirm it. */
@Composable
private fun SetPinDialog(onDone: () -> Unit, onCancel: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Box {
                PinPad(
                    title = if (first == null) "Choose a PIN" else "Confirm PIN",
                    subtitle = if (first == null) "4 digits" else "Enter it again",
                    error = error,
                ) { pin ->
                    val f = first
                    if (f == null) {
                        first = pin; error = null
                    } else if (f == pin) {
                        Security.setPin(pin); onDone()
                    } else {
                        first = null; error = "PINs didn't match — start again"
                    }
                }
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(8.dp)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun VerifyPinDialog(title: String, onVerified: () -> Unit, onCancel: () -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Box {
                PinPad(title = title, subtitle = "Enter your current PIN", error = error) { pin ->
                    if (Security.check(pin)) onVerified() else error = "Wrong PIN"
                }
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(8.dp)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onSecureChanged: () -> Unit) {
    var settingPin by remember { mutableStateOf(false) }
    var removingPin by remember { mutableStateOf(false) }
    var changingPin by remember { mutableStateOf(false) }
    var bytes by remember { mutableStateOf<Long?>(null) }
    val photoCount = Library.photos.size

    LaunchedEffect(photoCount) { bytes = withContext(Dispatchers.IO) { Library.storageBytes() } }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionLabel("Privacy") }
            item {
                SettingCard {
                    SwitchRow(
                        title = "App lock",
                        subtitle = "Ask for a 4-digit PIN when opening Kin",
                        checked = Security.hasPin,
                        onChange = { on -> if (on) settingPin = true else removingPin = true },
                    )
                    if (Security.hasPin) {
                        Text(
                            "Change PIN",
                            color = Accent,
                            modifier = Modifier.fillMaxWidth().clickable { changingPin = true }.padding(16.dp),
                        )
                    }
                    SwitchRow(
                        title = "Private mode",
                        subtitle = "Block screenshots and hide Kin's preview in recent apps",
                        checked = Security.secureScreen,
                        onChange = { Security.setSecure(it); onSecureChanged() },
                    )
                }
            }
            item { SectionLabel("Storage") }
            item {
                SettingCard {
                    Column(Modifier.padding(16.dp)) {
                        Text("${people(Library.albums.size)} · ${plural(photoCount, "photo")}", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            bytes?.let { "%.1f MB used".format(it / 1_048_576.0) } ?: "Calculating…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                ) {
                    Icon(Icons.Filled.Favorite, null, tint = Accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Photos are copied into Kin's private storage, so they stay safe even if you delete them from your gallery. " +
                            "Uninstalling Kin deletes them — use \"Export to gallery\" in an album's menu to make a backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (settingPin) SetPinDialog(onDone = { settingPin = false }, onCancel = { settingPin = false })
    if (changingPin) VerifyPinDialog("Change PIN", onVerified = { changingPin = false; settingPin = true }, onCancel = { changingPin = false })
    if (removingPin) VerifyPinDialog("Turn off lock", onVerified = { Security.removePin(); removingPin = false }, onCancel = { removingPin = false })
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp),
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
    ) { content() }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
