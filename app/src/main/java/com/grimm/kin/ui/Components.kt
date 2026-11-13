package com.grimm.kin.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.grimm.kin.data.Album
import com.grimm.kin.data.Library
import com.grimm.kin.data.Photo

val RELATIONS = listOf("Family", "Partner", "Best friend", "Friend", "Mom", "Dad", "Sibling")

private val avatarColors = listOf(
    Color(0xFF7A4B3A), Color(0xFF3A5A7A), Color(0xFF5A3A7A), Color(0xFF3A7A5E),
    Color(0xFF7A6A3A), Color(0xFF7A3A55), Color(0xFF3A6E7A),
)

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** Gradient + initials, used when an album has no photos yet. */
@Composable
fun InitialsFill(name: String, modifier: Modifier = Modifier, fontSize: Int = 44) {
    val base = avatarColors[(name.hashCode() and 0x7fffffff) % avatarColors.size]
    Box(
        modifier.background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.45f), Color(0xFF15151A)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), color = Color.White.copy(alpha = 0.92f), fontSize = fontSize.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Avatar(album: Album, size: Dp) {
    val cover = Library.coverOf(album)
    Box(Modifier.size(size).clip(CircleShape)) {
        if (cover != null) {
            AsyncImage(
                model = Library.fileOf(cover), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else {
            InitialsFill(album.name, Modifier.fillMaxSize(), fontSize = (size.value * 0.38f).toInt())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumEditDialog(initial: Album?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var relation by remember { mutableStateOf(initial?.relation ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(if (initial == null) "New person" else "Edit person") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = relation, onValueChange = { relation = it },
                    label = { Text("Relation (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    RELATIONS.forEach { r ->
                        FilterChip(
                            selected = relation == r,
                            onClick = { relation = if (relation == r) "" else r },
                            label = { Text(r) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), relation.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AlbumPickerDialog(
    title: String,
    excludeId: String?,
    onPick: (String) -> Unit,
    onNew: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val list = Library.sortedAlbums().filter { it.id != excludeId }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                if (onNew != null) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onNew() }.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary) }
                            Spacer(Modifier.width(14.dp))
                            Text("New person", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (list.isEmpty() && onNew == null) {
                    item { Text("No other albums yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(list, key = { it.id }) { a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(a.id) }.padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(a, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(a.relation, plural(Library.countOf(a.id), "photo")).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

fun sharePhotos(context: Context, photos: List<Photo>) {
    if (photos.isEmpty()) return
    val uris: List<Uri> = photos.map {
        FileProvider.getUriForFile(context, context.packageName + ".files", Library.fileOf(it))
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, uris[0]) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply { putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris)) }
    }
    intent.type = "image/*"
    val clip = ClipData.newRawUri("photos", uris[0])
    uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
    intent.clipData = clip
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share"))
}
