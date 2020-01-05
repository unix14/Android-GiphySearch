/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.giphy

import android.app.Activity
import android.content.Intent

/**
 * Entry class for creating a new GiphyActivity and downloading gifs from the service.
 *
 *
 * Simply create a new Giphy object and call start. Then in your activity, look for the results in
 * onActivityResult(). The uri to the downloaded image will be available at intent.getData() if the
 * result is set to Activity.RESULT_OK.
 */
class Giphy private constructor(private val activity: Activity, private val apiKey: String) {
    private var saveLocation: String? = null
    private var limit: Int = 0
    private var previewSize: Int = 0
    private var maxFileSize: Long = 0
    private var useStickers: Boolean = false
    private var downloadFile: Boolean = false

    fun start(requestCode: Int) {
        val intent = Intent(activity, GiphyActivity::class.java)
        intent.putExtra(GiphyActivity.EXTRA_API_KEY, apiKey)
        intent.putExtra(GiphyActivity.EXTRA_GIF_LIMIT, limit)
        intent.putExtra(GiphyActivity.EXTRA_PREVIEW_SIZE, previewSize)
        intent.putExtra(GiphyActivity.EXTRA_SIZE_LIMIT, maxFileSize)
        intent.putExtra(GiphyActivity.EXTRA_SAVE_LOCATION, saveLocation)
        intent.putExtra(GiphyActivity.EXTRA_USE_STICKERS, useStickers)
        intent.putExtra(GiphyActivity.EXTRA_DOWNLOAD_FILE, downloadFile)
        activity.startActivityForResult(intent, requestCode)
    }

    class Builder(activity: Activity, apiKey: String) {

        private val giphy: Giphy

        init {
            this.giphy = Giphy(activity, apiKey)
        }

        fun setSaveLocation(saveLocation: String): Giphy.Builder {
            giphy.saveLocation = saveLocation
            return this
        }

        fun maxFileSize(maxFileSize: Long): Giphy.Builder {
            giphy.maxFileSize = maxFileSize
            return this
        }

        fun setQueryLimit(limit: Int): Giphy.Builder {
            giphy.limit = limit
            return this
        }

        fun setPreviewSize(previewSize: Int): Giphy.Builder {
            giphy.previewSize = previewSize
            return this
        }

        fun useStickers(useStickers: Boolean): Giphy.Builder {
            giphy.useStickers = useStickers
            return this
        }

        fun downloadFile(download: Boolean): Giphy.Builder {
            giphy.downloadFile = download
            return this
        }

        fun build(): Giphy {
            return giphy
        }

        fun start() {
            build().start(REQUEST_GIPHY)
        }
    }

    companion object {

        val REQUEST_GIPHY = 10012
        val PREVIEW_SMALL = 0
        val PREVIEW_MEDIUM = 1
        val PREVIEW_LARGE = 2
    }

}
