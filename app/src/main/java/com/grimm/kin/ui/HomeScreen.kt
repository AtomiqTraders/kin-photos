package com.grimm.kin.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grimm.kin.data.Album
import com.grimm.kin.data.Library

@Composable
fun HomeScreen(onOpenAlbum: (String) -> Unit, onOpenSettings: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Album?>(null) }
    var deleting by remember { mutableStateOf<Album?>(null) }

    val all = Library.sortedAlbums()
    val albums = all.filter {
        query.isBlank() || it.name.contains(query.trim(), true) || it.relation.contains(query.trim(), true)
    }

    Scaffold(
        containerColor = Bg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New person") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 8.dp,
                bottom = pad.calculateBottomPadding() + 100.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Kin", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${people(all.size)} · ${plural(Library.photos.size, "photo")}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings") }
                    }
                    if (all.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search people") },
                            leadingIcon = { Icon(Icons.Filled.Search, null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Clear") }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (all.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 80.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.Favorite, null, tint = Accent, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Your people, in one place", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Create an album for each person close to you and keep their photos safe — even if they're deleted from your gallery.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { creating = true }) { Text("Add your first person") }
                    }
                }
            } else if (albums.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No one matches \"$query\"",
                        Modifier.padding(top = 40.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onOpenAlbum(album.id) },
                    onEdit = { editing = album },
                    onPin = { Library.togglePin(album.id) },
                    onDelete = { deleting = album },
                )
            }
        }
    }

    if (creating) {
        AlbumEditDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { name, rel ->
                creating = false
                onOpenAlbum(Library.createAlbum(name, rel).id)
            },
        )
    }
    editing?.let { a ->
        AlbumEditDialog(
            initial = a,
            onDismiss = { editing = null },
            onSave = { name, rel -> Library.updateAlbum(a.id, name, rel); editing = null },
        )
    }
    deleting?.let { a ->
        ConfirmDialog(
            title = "Delete ${a.name}?",
            text = "This permanently deletes the album and its ${plural(Library.countOf(a.id), "photo")} from Kin. Export to gallery first if you want a copy.",
            confirmLabel = "Delete",
            onConfirm = { Library.deleteAlbum(a.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    val count = Library.countOf(album.id)
    val cover = Library.coverOf(album)
    var menu by remember { mutableStateOf(false) }

    Box(
        Modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = { menu = true })
    ) {
        if (cover != null) {
            AsyncImage(
                model = Library.fileOf(cover), contentDescription = album.name,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else {
            InitialsFill(album.name, Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text(
                album.name, color = Color.White, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(album.relation, plural(count, "photo")).filter { it.isNotBlank() }.joinToString(" · "),
                color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (album.pinned) {
            Icon(
                Icons.Filled.Star, "Pinned", tint = Accent,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(18.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Edit") }, leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { menu = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text(if (album.pinned) "Unpin" else "Pin to top") }, leadingIcon = { Icon(Icons.Filled.Star, null) },
                onClick = { menu = false; onPin() },
            )
            DropdownMenuItem(
                text = { Text("Delete") }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = { menu = false; onDelete() },
            )
        }
    }
}
