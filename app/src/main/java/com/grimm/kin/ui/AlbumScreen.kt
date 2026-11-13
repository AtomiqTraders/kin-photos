package com.grimm.kin.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grimm.kin.data.Library
import com.grimm.kin.data.Photo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(albumId: String, onBack: () -> Unit, onOpenPhoto: (String, Boolean) -> Unit) {
    val album = Library.album(albumId)
    if (album == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var favOnly by rememberSaveable { mutableStateOf(false) }
    val all = Library.photosOf(albumId)
    val photos = if (favOnly) all.filter { it.favorite } else all
    val selected = remember { mutableStateListOf<String>() }
    val selecting = selected.isNotEmpty()

    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDeleteAlbum by remember { mutableStateOf(false) }
    var confirmDeletePhotos by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val n = Library.import(ctx, albumId, uris)
            snack.showSnackbar(if (n == uris.size) "Added ${plural(n, "photo")}" else "Added $n of ${uris.size} photos")
        }
    }

    fun toggle(id: String) {
        if (id in selected) selected.remove(id) else selected.add(id)
    }

    BackHandler(enabled = selecting) { selected.clear() }

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                title = {
                    Text(
                        if (selecting) "${selected.size} selected" else album.name,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selecting) selected.clear() else onBack() }) {
                        if (selecting) Icon(Icons.Filled.Close, "Cancel")
                        else Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selecting) {
                        IconButton(onClick = {
                            if (selected.size == photos.size) selected.clear()
                            else { selected.clear(); selected.addAll(photos.map { it.id }) }
                        }) { Icon(Icons.Filled.CheckCircle, "Select all") }
                        IconButton(onClick = { sharePhotos(ctx, photos.filter { it.id in selected }) }) {
                            Icon(Icons.Filled.Share, "Share")
                        }
                        IconButton(onClick = { moving = true }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Move") }
                        IconButton(onClick = { confirmDeletePhotos = true }) { Icon(Icons.Filled.Delete, "Delete") }
                    } else {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit person") }, leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                onClick = { menu = false; editing = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Export to gallery") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                                onClick = {
                                    menu = false
                                    scope.launch {
                                        val n = Library.exportToGallery(ctx, all, album.name)
                                        snack.showSnackbar(
                                            if (n < 0) "Export needs Android 10 or newer"
                                            else "Saved ${plural(n, "photo")} to Pictures/Kin"
                                        )
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete person") }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menu = false; confirmDeleteAlbum = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!selecting) {
                ExtendedFloatingActionButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Add photos") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { pad ->
        Box(Modifier.padding(top = pad.calculateTopPadding()).fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = pad.calculateBottomPadding() + 100.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(album, 48.dp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(
                                album.relation.ifBlank { "Person" },
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "${plural(all.size, "photo")} · ${all.count { it.favorite }} favorites",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilterChip(
                            selected = favOnly,
                            onClick = { favOnly = !favOnly; selected.clear() },
                            label = { Text("Favorites") },
                            leadingIcon = {
                                Icon(
                                    if (favOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    null, Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
                if (photos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 70.dp, start = 32.dp, end = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (favOnly) "No favorites yet" else "No photos yet",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (favOnly) "Tap the heart on a photo to add it here."
                                else "Tap Add photos to save pictures of ${album.name}. You can also share photos to Kin from your gallery or WhatsApp.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                items(photos, key = { it.id }) { p ->
                    PhotoCell(
                        photo = p,
                        isSelected = p.id in selected,
                        selecting = selecting,
                        onClick = { if (selecting) toggle(p.id) else onOpenPhoto(p.id, favOnly) },
                        onLongClick = { toggle(p.id) },
                    )
                }
            }
            if (Library.busy > 0) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }

    if (editing) {
        AlbumEditDialog(
            initial = album,
            onDismiss = { editing = false },
            onSave = { n, r -> Library.updateAlbum(album.id, n, r); editing = false },
        )
    }
    if (confirmDeleteAlbum) {
        ConfirmDialog(
            title = "Delete ${album.name}?",
            text = "This permanently deletes the album and its ${plural(all.size, "photo")} from Kin.",
            confirmLabel = "Delete",
            onConfirm = { confirmDeleteAlbum = false; Library.deleteAlbum(album.id) },
            onDismiss = { confirmDeleteAlbum = false },
        )
    }
    if (confirmDeletePhotos) {
        ConfirmDialog(
            title = "Delete ${plural(selected.size, "photo")}?",
            text = "They'll be removed from Kin permanently.",
            confirmLabel = "Delete",
            onConfirm = { Library.deletePhotos(selected.toList()); selected.clear(); confirmDeletePhotos = false },
            onDismiss = { confirmDeletePhotos = false },
        )
    }
    if (moving) {
        AlbumPickerDialog(
            title = "Move to…",
            excludeId = album.id,
            onPick = { target ->
                val n = selected.size
                Library.movePhotos(selected.toList(), target)
                selected.clear(); moving = false
                scope.launch { snack.showSnackbar("Moved ${plural(n, "photo")} to ${Library.album(target)?.name}") }
            },
            onNew = null,
            onDismiss = { moving = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCell(
    photo: Photo,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val inset by animateDpAsState(if (isSelected) 10.dp else 0.dp, label = "inset")
    Box(Modifier.aspectRatio(1f).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        AsyncImage(
            model = Library.fileOf(photo), contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(inset)
                .clip(RoundedCornerShape(if (isSelected) 12.dp else 0.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        if (photo.favorite) {
            Icon(
                Icons.Filled.Favorite, null, tint = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(14.dp),
            )
        }
        if (selecting) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.35f))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

