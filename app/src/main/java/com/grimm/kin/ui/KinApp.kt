package com.grimm.kin.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.grimm.kin.data.Library
import com.grimm.kin.data.Security
import kotlinx.coroutines.launch

val Accent = Color(0xFFFF9E7A)
val Bg = Color(0xFF0D0D0F)

private val KinColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF2B1209),
    primaryContainer = Color(0xFF3A2219),
    onPrimaryContainer = Accent,
    secondaryContainer = Color(0xFF2A211E),
    onSecondaryContainer = Accent,
    background = Bg,
    onBackground = Color(0xFFF2F0EE),
    surface = Bg,
    onSurface = Color(0xFFF2F0EE),
    surfaceVariant = Color(0xFF1C1C21),
    onSurfaceVariant = Color(0xFFA9A4A0),
    surfaceContainerLowest = Color(0xFF0A0A0C),
    surfaceContainerLow = Color(0xFF131316),
    surfaceContainer = Color(0xFF17171B),
    surfaceContainerHigh = Color(0xFF1E1E23),
    surfaceContainerHighest = Color(0xFF26262C),
    outline = Color(0xFF4A4644),
    outlineVariant = Color(0xFF2E2C2B),
    error = Color(0xFFFF6B6B),
)

@Composable
fun KinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KinColors, content = content)
}

sealed interface Screen {
    data object Home : Screen
    data class AlbumS(val id: String) : Screen
    data class Viewer(val albumId: String, val photoId: String, val favoritesOnly: Boolean) : Screen
    data object Settings : Screen
}

@Composable
fun KinApp(incoming: MutableState<List<Uri>>, onSecureChanged: () -> Unit) {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Only pops if the caller is still the top screen (avoids double-pops during animations).
    fun popIf(s: Screen) {
        if (stack.size > 1 && stack.last() == s) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        AnimatedContent(
            targetState = stack.last(),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "nav",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(
                    onOpenAlbum = { stack.add(Screen.AlbumS(it)) },
                    onOpenSettings = { stack.add(Screen.Settings) },
                )
                is Screen.AlbumS -> AlbumScreen(
                    albumId = screen.id,
                    onBack = { popIf(screen) },
                    onOpenPhoto = { pid, fav -> stack.add(Screen.Viewer(screen.id, pid, fav)) },
                )
                is Screen.Viewer -> ViewerScreen(
                    albumId = screen.albumId,
                    photoId = screen.photoId,
                    favoritesOnly = screen.favoritesOnly,
                    onBack = { popIf(screen) },
                )
                Screen.Settings -> SettingsScreen(
                    onBack = { popIf(screen) },
                    onSecureChanged = onSecureChanged,
                )
            }
        }

        // Photos shared into Kin from another app
        if (incoming.value.isNotEmpty() && !Security.locked) {
            var creating by remember { mutableStateOf(false) }
            fun doImport(albumId: String) {
                val uris = incoming.value
                incoming.value = emptyList()
                scope.launch {
                    val n = Library.import(ctx, albumId, uris)
                    Toast.makeText(ctx, "Saved ${plural(n, "photo")} to ${Library.album(albumId)?.name}", Toast.LENGTH_SHORT).show()
                    stack.removeAll { it !is Screen.Home }
                    stack.add(Screen.AlbumS(albumId))
                }
            }
            if (creating) {
                AlbumEditDialog(
                    initial = null,
                    onDismiss = { creating = false },
                    onSave = { name, rel -> creating = false; doImport(Library.createAlbum(name, rel).id) },
                )
            } else {
                AlbumPickerDialog(
                    title = "Save ${plural(incoming.value.size, "photo")} to…",
                    excludeId = null,
                    onPick = { doImport(it) },
                    onNew = { creating = true },
                    onDismiss = { incoming.value = emptyList() },
                )
            }
        }

        if (Security.locked) LockScreen(onUnlock = { Security.locked = false })
    }
}

fun plural(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"
fun people(n: Int) = if (n == 1) "1 person" else "$n people"
