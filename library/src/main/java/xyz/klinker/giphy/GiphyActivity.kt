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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.klinker.common.InfiniteRecyclerViewScrollListener

class GiphyActivity : AppCompatActivity(), TextWatcher {

    private var saveLocation: String? = null
    private var useStickers: Boolean = false
    private var downloadFile: Boolean = false
    private var queried = false

    private var helper: GiphyApiHelper? = null
    private var recycler: RecyclerView? = null
    private var adapter: GiphyAdapter? = null
    private var progressSpinner: View? = null
    private var searchView: EditText? = null

    private var infiniteRecyclerViewScrollListener: InfiniteRecyclerViewScrollListener? = null
    private var layoutManager: LinearLayoutManager? = null
    private var searchBtn: ImageButton? = null
    private var stickerBtn: ImageButton? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.extras == null || !intent.extras!!.containsKey(EXTRA_API_KEY)) {
            throw RuntimeException("EXTRA_API_KEY is required!")
        }

        saveLocation = intent.extras!!.getString(EXTRA_SAVE_LOCATION, null)
        useStickers = intent.extras!!.getBoolean(EXTRA_USE_STICKERS, false)
        downloadFile = intent.extras!!.getBoolean(EXTRA_DOWNLOAD_FILE, false)

        helper = GiphyApiHelper(intent.extras!!.getString(EXTRA_API_KEY)!!,
                intent.extras!!.getInt(EXTRA_GIF_LIMIT, GiphyApiHelper.NO_SIZE_LIMIT),
                intent.extras!!.getInt(EXTRA_PREVIEW_SIZE, Giphy.PREVIEW_SMALL),
                intent.extras!!.getLong(EXTRA_SIZE_LIMIT, GiphyApiHelper.NO_SIZE_LIMIT.toLong()))
        helper!!.useStickers(useStickers)

        try {
            window.requestFeature(Window.FEATURE_NO_TITLE)
        } catch (e: Exception) {
        }

        setContentView(R.layout.giphy_search_activity)

        recycler = findViewById<View>(R.id.recycler_view) as RecyclerView
        progressSpinner = findViewById(R.id.list_progress)

        searchBtn = findViewById(R.id.search_Btn)
        stickerBtn = findViewById(R.id.sticker_Btn)

        searchBtn!!.setOnClickListener {
            executeQuery(searchView!!.text.toString())
            dismissKeyboard()
        }

        stickerBtn!!.setOnClickListener {
            stickerBtn!!.isActivated = !stickerBtn!!.isActivated
            useStickers = stickerBtn!!.isActivated
            helper!!.useStickers(useStickers)

            if (queried) {
                executeQuery(searchView!!.text.toString())
            } else {
                loadTrending()
            }
        }

        searchView = findViewById<View>(R.id.search_view) as EditText
        searchView!!.addTextChangedListener(this)

        searchView!!.setOnEditorActionListener(
                TextView.OnEditorActionListener { v, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        executeQuery(searchView!!.text.toString())
                        dismissKeyboard()

                        return@OnEditorActionListener true
                    }
                    false
                }
        )

        Handler().postDelayed({ loadTrending() }, 180)
    }

    public override fun onStart() {
        super.onStart()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable) {
        if (s.toString().isEmpty()) {
            loadTrending()
        } else {
            executeQuery(searchView!!.text.toString())
        }
    }


    override fun onBackPressed() {
        if (queried) {
            queried = false
            searchView!!.removeTextChangedListener(this)
            searchView!!.setText("")
            loadTrending()
            searchView!!.addTextChangedListener(this)
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun loadTrending() {
        queried = false
        progressSpinner!!.visibility = View.VISIBLE
        helper!!.trends(object : GiphyApiHelper.Callback {
            override fun onResponse(gifs: List<GiphyApiHelper.Gif>) {
                setAdapter(gifs)
            }
        })
    }

    private fun executeQuery(query: String) {
        queried = true
        progressSpinner!!.visibility = View.VISIBLE

        helper!!.search(query, object : GiphyApiHelper.Callback {
            override fun onResponse(gifs: List<GiphyApiHelper.Gif>) {
                setAdapter(gifs)
            }
        })
    }

    private fun setAdapter(gifs: List<GiphyApiHelper.Gif>) {
        progressSpinner!!.visibility = View.GONE
        if (gifs.isNotEmpty() && (adapter == null || adapter!!.gifs != gifs)) {
            adapter = GiphyAdapter(gifs, object : GiphyAdapter.Callback {
                override fun onClick(item: GiphyApiHelper.Gif) {
                    if (downloadFile) {
                        DownloadGif(this@GiphyActivity, item.gifUrl, item.name, saveLocation).execute()
                    } else {
                        setResult(Activity.RESULT_OK, Intent().setData(Uri.parse(item.gifUrl)))
                        finish()
                    }
                }
            })

            layoutManager = LinearLayoutManager(this@GiphyActivity)

            if (queried && infiniteRecyclerViewScrollListener == null) {
                infiniteRecyclerViewScrollListener = getInfiniteScrollListener(layoutManager!!)
                recycler?.addOnScrollListener(infiniteRecyclerViewScrollListener!!)
            } else if (infiniteRecyclerViewScrollListener != null) {
                recycler?.removeOnScrollListener(infiniteRecyclerViewScrollListener!!)
                infiniteRecyclerViewScrollListener = null
            }

            recycler?.layoutManager = layoutManager
            recycler?.adapter = adapter
        }
    }

    private fun dismissKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView!!.windowToken, 0)

        searchView!!.clearFocus()
    }

    private fun getInfiniteScrollListener(layoutManager: LinearLayoutManager): InfiniteRecyclerViewScrollListener {
        return object : InfiniteRecyclerViewScrollListener(layoutManager) {
            override fun onDataHunger() {}

            override fun requestData(offset: Int) {
                onLoadNextPage(offset)
            }
        }
    }

    private fun onLoadNextPage(offset: Int) {
        progressSpinner!!.visibility = View.VISIBLE

        helper!!.loadMoreSearchResults(searchView!!.text.toString(), offset, object : GiphyApiHelper.Callback {
            override fun onResponse(gifs: List<GiphyApiHelper.Gif>) {
                progressSpinner!!.visibility = View.GONE
                addGifsToList(gifs)
            }
        })
    }

    private fun addGifsToList(gifs: List<GiphyApiHelper.Gif>) {
        adapter?.let {
            if (gifs.isNotEmpty()) {
                val loadedGifs = arrayListOf<GiphyApiHelper.Gif>()

                loadedGifs.addAll(it.gifs)
                loadedGifs.addAll(gifs)

                setAdapter(loadedGifs)
            }
        }
    }

    companion object {

        val EXTRA_API_KEY = "api_key"
        val EXTRA_GIF_LIMIT = "gif_limit"
        val EXTRA_PREVIEW_SIZE = "preview_size"
        val EXTRA_SIZE_LIMIT = "size_limit"
        val EXTRA_SAVE_LOCATION = "save_location"
        val EXTRA_USE_STICKERS = "use_stickers"
        val EXTRA_DOWNLOAD_FILE = "download_file"
    }
}
