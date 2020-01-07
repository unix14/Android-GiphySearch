package xyz.klinker.giphy

import android.net.Uri

interface GifSelectedCallback {
    fun onGifSelected(uri: Uri)
}