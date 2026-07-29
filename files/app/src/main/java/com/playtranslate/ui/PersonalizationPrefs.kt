package com.playtranslate.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

class PersonalizationPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    val backgroundUri: Uri?
        get() = prefs.getString(KEY_BACKGROUND, null)?.let(Uri::parse)
    val avatarUri: Uri?
        get() = prefs.getString(KEY_AVATAR, null)?.let(Uri::parse)

    fun setBackground(uri: Uri?) = prefs.edit {
        if (uri == null) remove(KEY_BACKGROUND) else putString(KEY_BACKGROUND, uri.toString())
    }
    fun setAvatar(uri: Uri?) = prefs.edit {
        if (uri == null) remove(KEY_AVATAR) else putString(KEY_AVATAR, uri.toString())
    }

    companion object {
        private const val NAME = "personalization"
        private const val KEY_BACKGROUND = "background_uri"
        private const val KEY_AVATAR = "avatar_uri"
    }
}
