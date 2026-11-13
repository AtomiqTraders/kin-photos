package com.grimm.kin.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grimm.kin.data.Library
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewerScreen(albumId: String, photoId: String, favoritesOnly: Boolean, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val all = Library.photosOf(albumId)
    val photos = if (favoritesOnly) all.filter { it.favorite } else all
    if (photos.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val initial = remember { photos.indexOfFirst { it.id == photoId }.coerceAtLeast(0) }
    val pager = rememberPagerState(initialPage = initial) { photos.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    val current = photos[pager.currentPage.coerceIn(0, photos.lastIndex)]
    val dateFmt = remember { SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault()) }

    LaunchedEffect(pager.currentPage) { zoomed = false }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pager,
            key = { photos[it].id },
            userScrollEnabled = !zoomed,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableImage(
                file = Library.fileOf(photos[page]),
                isCurrent = page == pager.currentPage,
                onTap = { chrome = !chrome },
                onZoomChanged = { z -> if (page == pager.currentPage) zoomed = z },
            )
        }

        AnimatedVisibility(
            visible = chrome, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(Library.album(albumId)?.name ?: "", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Text(
                        dateFmt.format(Date(current.date)) + "  ·  ${pager.currentPage + 1}/${photos.size}",
                        color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chrome, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ViewerAction(
                    if (current.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Favorite", tint = if (current.favorite) Accent else Color.White,
                ) { Library.toggleFavorite(current.id) }
                ViewerAction(Icons.Filled.AccountCircle, "Cover") {
                    Library.setCover(albumId, current.id)
                    Toast.makeText(ctx, "Set as album cover", Toast.LENGTH_SHORT).show()
                }
                ViewerAction(Icons.Filled.Share, "Share") { sharePhotos(ctx, listOf(current)) }
                ViewerAction(Icons.AutoMirrored.Filled.ArrowForward, "Move") { moving = true }
                ViewerAction(Icons.Filled.Delete, "Delete") { confirmDelete = true }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this photo?",
            text = "It will be removed from Kin permanently.",
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; Library.deletePhotos(listOf(current.id)) },
            onDismiss = { confirmDelete = false },
        )
    }
    if (moving) {
        AlbumPickerDialog(
            title = "Move to…",
            excludeId = albumId,
            onPick = { target ->
                moving = false
                Library.movePhotos(listOf(current.id), target)
                Toast.makeText(ctx, "Moved to ${Library.album(target)?.name}", Toast.LENGTH_SHORT).show()
            },
            onNew = null,
            onDismiss = { moving = false },
        )
    }
}

@Composable
private fun ViewerAction(icon: ImageVector, label: String, tint: Color = Color.White, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ZoomableImage(
    file: File,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val tap by rememberUpdatedState(onTap)
    val zoomCb by rememberUpdatedState(onZoomChanged)

    LaunchedEffect(isCurrent) {
        if (!isCurrent) { scale = 1f; offset = Offset.Zero }
    }

    fun clamp(o: Offset, s: Float): Offset {
        val mx = box.width * (s - 1f) / 2f
        val my = box.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
    }

    AsyncImage(
        model = file,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tap() },
                    onDoubleTap = { p ->
                        if (scale > 1f) {
                            scale = 1f; offset = Offset.Zero
                        } else {
                            val target = 2.5f
                            val center = Offset(box.width / 2f, box.height / 2f)
                            scale = target
                            offset = clamp((center - p) * (target - 1f), target)
                        }
                        zoomCb(scale > 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1 || scale > 1f) {
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            offset = if (newScale <= 1f) Offset.Zero else clamp(offset + event.calculatePan(), newScale)
                            scale = newScale
                            zoomCb(scale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offset.x; translationY = offset.y
            },
    )
}

