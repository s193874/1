package com.playtranslate.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.edit

/** Small, non-secret preference store for the user-supplied floating-ball skin. */
class FloatingIconStylePrefs(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    val imageUri: Uri?
        get() = prefs.getString(KEY_URI, null)?.takeIf { it.isNotBlank() }?.let(Uri::parse)

    val opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY).coerceIn(0.2f, 1f)

    val scale: Float
        get() = prefs.getFloat(KEY_SCALE, DEFAULT_SCALE).coerceIn(0.55f, 1f)

    fun save(uri: Uri?, opacity: Float, scale: Float) {
        prefs.edit {
            if (uri == null) remove(KEY_URI) else putString(KEY_URI, uri.toString())
            putFloat(KEY_OPACITY, opacity.coerceIn(0.2f, 1f))
            putFloat(KEY_SCALE, scale.coerceIn(0.55f, 1f))
        }
    }

    fun reset() = save(null, DEFAULT_OPACITY, DEFAULT_SCALE)

    companion object {
        private const val NAME = "floating_icon_style"
        private const val KEY_URI = "image_uri"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_SCALE = "scale"
        const val DEFAULT_OPACITY = 0.72f
        const val DEFAULT_SCALE = 0.9f
    }
}


/** Decode a user-selected skin without loading a huge phone photo at full resolution. */
fun decodeFloatingIconBitmap(context: Context, uri: Uri, maxSidePx: Int = 512): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    }.getOrNull()
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (bounds.outWidth / sample > maxSidePx * 2 || bounds.outHeight / sample > maxSidePx * 2) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}
