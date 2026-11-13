package com.grimm.kin.data

import android.content.ContentValues
import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class Album(
    val id: String,
    val name: String,
    val relation: String,
    val coverId: String?,
    val pinned: Boolean,
    val createdAt: Long,
)

data class Photo(
    val id: String,
    val albumId: String,
    val file: String,
    val addedAt: Long,
    val takenAt: Long,
    val favorite: Boolean,
) {
    val date: Long get() = if (takenAt > 0) takenAt else addedAt
}

/**
 * All albums + photos. Photos are COPIED into the app's private storage,
 * so they stay safe even if deleted from the phone gallery.
 * Index is a small JSON file (library.json).
 */
object Library {
    private lateinit var root: File
    private lateinit var dbFile: File
    lateinit var photosDir: File
        private set

    val albums = mutableStateListOf<Album>()
    val photos = mutableStateListOf<Photo>()
    var busy by mutableIntStateOf(0)

    fun init(context: Context) {
        if (::root.isInitialized) return
        root = context.applicationContext.filesDir
        photosDir = File(root, "photos").apply { mkdirs() }
        dbFile = File(root, "library.json")
        load()
    }

    // ---------- queries ----------

    fun sortedAlbums(): List<Album> =
        albums.sortedWith(compareByDescending<Album> { it.pinned }.thenBy { it.name.lowercase() })

    fun photosOf(albumId: String): List<Photo> =
        photos.filter { it.albumId == albumId }.sortedByDescending { it.date }

    fun countOf(albumId: String): Int = photos.count { it.albumId == albumId }

    fun coverOf(album: Album): Photo? =
        album.coverId?.let { id -> photos.firstOrNull { it.id == id && it.albumId == album.id } }
            ?: photos.filter { it.albumId == album.id }.maxByOrNull { it.date }

    fun fileOf(photo: Photo): File = File(photosDir, photo.file)

    fun album(id: String): Album? = albums.firstOrNull { it.id == id }

    // ---------- albums ----------

    fun createAlbum(name: String, relation: String): Album {
        val a = Album(UUID.randomUUID().toString(), name, relation, null, false, System.currentTimeMillis())
        albums.add(a)
        save()
        return a
    }

    fun updateAlbum(id: String, name: String, relation: String) = editAlbum(id) { it.copy(name = name, relation = relation) }

    fun togglePin(id: String) = editAlbum(id) { it.copy(pinned = !it.pinned) }

    fun setCover(albumId: String, photoId: String) = editAlbum(albumId) { it.copy(coverId = photoId) }

    fun deleteAlbum(id: String) {
        photos.filter { it.albumId == id }.forEach { fileOf(it).delete() }
        photos.removeAll { it.albumId == id }
        albums.removeAll { it.id == id }
        save()
    }

    private fun editAlbum(id: String, change: (Album) -> Album) {
        val i = albums.indexOfFirst { it.id == id }
        if (i >= 0) {
            albums[i] = change(albums[i]); save()
        }
    }

    // ---------- photos ----------

    suspend fun import(context: Context, albumId: String, uris: List<Uri>): Int {
        busy++
        try {
            val added = withContext(Dispatchers.IO) {
                val cr = context.contentResolver
                uris.mapNotNull { uri ->
                    runCatching {
                        val mime = cr.getType(uri) ?: "image/jpeg"
                        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
                        val id = UUID.randomUUID().toString()
                        val name = "$id.$ext"
                        val out = File(photosDir, name)
                        cr.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
                        if (out.length() == 0L) { out.delete(); error("empty") }
                        Photo(id, albumId, name, System.currentTimeMillis(), readTakenAt(out), false)
                    }.getOrNull()
                }
            }
            photos.addAll(added)
            save()
            return added.size
        } finally {
            busy--
        }
    }

    fun deletePhotos(ids: Collection<String>) {
        val set = ids.toSet()
        photos.filter { it.id in set }.forEach { fileOf(it).delete() }
        photos.removeAll { it.id in set }
        save()
    }

    fun movePhotos(ids: Collection<String>, targetAlbumId: String) {
        val set = ids.toSet()
        for (i in photos.indices) {
            if (photos[i].id in set) photos[i] = photos[i].copy(albumId = targetAlbumId)
        }
        save()
    }

    fun toggleFavorite(id: String) {
        val i = photos.indexOfFirst { it.id == id }
        if (i >= 0) {
            photos[i] = photos[i].copy(favorite = !photos[i].favorite); save()
        }
    }

    /** Copies photos to Pictures/Kin/<folder>. Returns count, or -1 if Android < 10. */
    suspend fun exportToGallery(context: Context, list: List<Photo>, folder: String): Int =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) exportQ(context, list, folder) else -1
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportQ(context: Context, list: List<Photo>, folder: String): Int {
        val cr = context.contentResolver
        val safe = folder.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifEmpty { "Album" }
        var n = 0
        for (p in list) {
            runCatching {
                val f = fileOf(p)
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension) ?: "image/jpeg"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, f.name)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kin/$safe")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                cr.openOutputStream(uri)!!.use { o -> f.inputStream().use { it.copyTo(o) } }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                cr.update(uri, values, null, null)
                n++
            }
        }
        return n
    }

    fun storageBytes(): Long = photosDir.listFiles()?.sumOf { it.length() } ?: 0L

    // ---------- persistence ----------

    private fun readTakenAt(f: File): Long = runCatching {
        val exif = ExifInterface(f.absolutePath)
        val s = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME) ?: return@runCatching 0L
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(s)?.time ?: 0L
    }.getOrDefault(0L)

    private fun load() {
        if (!dbFile.exists()) return
        runCatching {
            val o = JSONObject(dbFile.readText())
            val a = o.optJSONArray("albums") ?: JSONArray()
            for (i in 0 until a.length()) {
                val j = a.getJSONObject(i)
                albums.add(
                    Album(
                        id = j.getString("id"),
                        name = j.getString("name"),
                        relation = j.optString("relation", ""),
                        coverId = if (j.has("coverId")) j.getString("coverId") else null,
                        pinned = j.optBoolean("pinned", false),
                        createdAt = j.optLong("createdAt", 0L),
                    )
                )
            }
            val p = o.optJSONArray("photos") ?: JSONArray()
            for (i in 0 until p.length()) {
                val j = p.getJSONObject(i)
                photos.add(
                    Photo(
                        id = j.getString("id"),
                        albumId = j.getString("albumId"),
                        file = j.getString("file"),
                        addedAt = j.optLong("addedAt", 0L),
                        takenAt = j.optLong("takenAt", 0L),
                        favorite = j.optBoolean("favorite", false),
                    )
                )
            }
        }
    }

    private fun save() {
        val o = JSONObject()
        o.put("version", 1)
        o.put("albums", JSONArray().apply {
            albums.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("name", a.name); put("relation", a.relation)
                    if (a.coverId != null) put("coverId", a.coverId)
                    put("pinned", a.pinned); put("createdAt", a.createdAt)
                })
            }
        })
        o.put("photos", JSONArray().apply {
            photos.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("albumId", p.albumId); put("file", p.file)
                    put("addedAt", p.addedAt); put("takenAt", p.takenAt); put("favorite", p.favorite)
                })
            }
        })
        val text = o.toString()
        val tmp = File(root, "library.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(dbFile)) {
            dbFile.writeText(text); tmp.delete()
        }
    }
}
