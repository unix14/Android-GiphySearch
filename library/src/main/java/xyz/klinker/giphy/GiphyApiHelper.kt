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

import android.os.AsyncTask
import android.util.Log

import org.json.JSONArray
import org.json.JSONObject

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.ArrayList
import java.util.Scanner

/**
 * Helper for working with Giphy data. To use, create a new object and pass in your api key and
 * max size, then call search() or trends().
 *
 *
 * new GiphyHelper(apiKey, 1024 * 1024)
 * .search(this);
 */
class GiphyApiHelper(private val apiKey: String, val limit: Int, private val previewSize: Int, private val maxSize: Long) {
    private var useStickers = false

    fun useStickers(useStickers: Boolean) {
        this.useStickers = useStickers
    }

    interface Callback {
        fun onResponse(gifs: List<Gif>)
    }

    fun search(query: String, callback: Callback) {
        SearchGiphy(apiKey, limit, previewSize, maxSize, query, callback, useStickers).execute()
    }

    fun loadMoreSearchResults(query: String, offset: Int, callback: Callback) {
        SearchGiphy(apiKey, limit, previewSize, maxSize, query, callback, useStickers, offset).execute()
    }

    fun trends(callback: Callback) {
        GiphyTrends(apiKey, previewSize, maxSize, callback, useStickers).execute()
    }

    private class GiphyTrends internal constructor(apiKey: String, previewSize: Int, maxSize: Long, callback: Callback, useStickers: Boolean) : SearchGiphy(apiKey, -1, previewSize, maxSize, null, callback, useStickers) {

        @Throws(UnsupportedEncodingException::class)
        override fun buildSearchUrl(query: String?): String {
            return "https://api.giphy.com/v1/" + (if (useStickers) "stickers" else "gifs") + "/trending?api_key=" + apiKey
        }
    }

    private open class SearchGiphy : AsyncTask<Void, Void, List<Gif>> {

        private var offset = -1
        internal var apiKey: String? = null
            private set
        private var limit: Int = 0
        private var previewSize: Int = 0
        private var maxSize: Long = 0
        private var query: String? = null
        private var callback: Callback? = null
        protected var useStickers: Boolean = false

        internal constructor(apiKey: String, limit: Int, previewSize: Int, maxSize: Long, query: String?, callback: Callback, useStickers: Boolean) {
            this.apiKey = apiKey
            this.limit = limit
            this.previewSize = previewSize
            this.maxSize = maxSize
            this.query = query
            this.callback = callback
            this.useStickers = useStickers
        }

        constructor(apiKey: String, limit: Int, previewSize: Int, maxSize: Long, query: String, callback: Callback, useStickers: Boolean, offset: Int) {
            this.apiKey = apiKey
            this.limit = limit
            this.previewSize = previewSize
            this.maxSize = maxSize
            this.query = query
            this.callback = callback
            this.useStickers = useStickers
            this.offset = offset
        }

        override fun doInBackground(vararg arg0: Void): List<Gif> {
            val gifList = ArrayList<Gif>()

            try {
                // create the connection
                val urlToRequest = URL(buildSearchUrl(query))
                val urlConnection = urlToRequest.openConnection() as HttpURLConnection

                // create JSON object from content
                val `in` = BufferedInputStream(
                        urlConnection.inputStream)
                val root = JSONObject(getResponseText(`in`))
                val data = root.getJSONArray("data")

                try {
                    `in`.close()
                } catch (e: Exception) {
                }

                try {
                    urlConnection.disconnect()
                } catch (e: Exception) {
                }

                for (i in 0 until data.length()) {
                    val gif = data.getJSONObject(i)
                    val name = gif.getString("slug")
                    Log.d("GIF Name", name)
                    val images = gif.getJSONObject("images")
                    val previewImage = images.getJSONObject("downsized_still")
                    val previewGif = images.getJSONObject(PREVIEW_SIZE[previewSize])
                    val originalSize = images.getJSONObject("original")
                    var downsized: JSONObject? = null

                    // Return the highest quality GIF under MaxSizeLimit.
                    for (size in SIZE_OPTIONS) {
                        downsized = images.getJSONObject(size)
                        Log.v("giphy", size + ": " + downsized!!.getString("size") + " bytes")

                        if (java.lang.Long.parseLong(downsized.getString("size")) < maxSize || maxSize == NO_SIZE_LIMIT.toLong()) {
                            break
                        } else {
                            downsized = null
                        }
                    }

                    if (downsized != null) {
                        gifList.add(
                                Gif(name,
                                        previewImage.getString("url"),
                                        previewGif.getString("url"),
                                        downsized.getString("url"),
                                        originalSize.getString("mp4"))
                        )
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return gifList
        }

        override fun onPostExecute(result: List<Gif>) {
            if (callback != null) {
                callback!!.onResponse(result)
            }
        }

        @Throws(UnsupportedEncodingException::class)
        protected open fun buildSearchUrl(query: String?): String {
            return "https://api.giphy.com/v1/" + (if (useStickers) "stickers" else "gifs") + "/search?q=" + URLEncoder.encode(query, "UTF-8") + "&limit=" + limit + "&api_key=" + apiKey + if (offset != -1) "&offset=$offset" else ""
        }

        private fun getResponseText(inStream: InputStream): String {
            return Scanner(inStream).useDelimiter("\\A").next()
        }
    }

    class Gif(name: String, previewImage: String, previewGif: String, gifUrl: String, mp4Url: String) {
        var name: String = ""
        var previewImage: String = ""
        var previewGif: String = ""
        var gifUrl: String = ""
        var mp4Url: String = ""
        var previewDownloaded = false
        var gifDownloaded = false

        init {
            try {
                this.name = URLDecoder.decode(name, "UTF-8")
                this.previewImage = URLDecoder.decode(previewImage, "UTF-8")
                this.previewGif = URLDecoder.decode(previewGif, "UTF-8")
                this.gifUrl = URLDecoder.decode(gifUrl, "UTF-8")
                this.mp4Url = URLDecoder.decode(mp4Url, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

        }
    }

    companion object {

        val NO_SIZE_LIMIT = -1

        private val PREVIEW_SIZE = arrayOf("fixed_width_downsampled", "fixed_width", "downsized")

        private val SIZE_OPTIONS = arrayOf("original", "downsized_large", "downsized_medium", "downsized", "fixed_height", "fixed_width", "fixed_height_small", "fixed_width_small")
    }
}
