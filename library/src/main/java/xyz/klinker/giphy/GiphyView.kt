package xyz.klinker.giphy

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GiphyView : FrameLayout {

    private var callback: GifSelectedCallback? = null

    private var helper: GiphyApiHelper? = null
    private var recycler: RecyclerView? = null
    private var adapter: GiphyAdapter? = null
    private var progressSpinner: View? = null
    private var searchView: EditText? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val gifView = LayoutInflater.from(context).inflate(R.layout.giphy_search_activity, this, false)
        addView(gifView)

        recycler = findViewById<View>(R.id.recycler_view) as RecyclerView
        progressSpinner = findViewById(R.id.list_progress)


        searchView = findViewById<View>(R.id.search_view) as EditText
        searchView!!.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executeQuery(searchView!!.text.toString())
                return@OnEditorActionListener true
            }
            false
        })
    }

    @JvmOverloads
    fun initializeView(apiKey: String, sizeLimit: Long, useStickers: Boolean = false) {
        helper = GiphyApiHelper(apiKey, 100, Giphy.PREVIEW_SMALL, sizeLimit)
        helper!!.useStickers(useStickers)

        loadTrending()

        if (useStickers) {
            searchView!!.setHint(R.string.find_a_sticker)
        }
    }

    fun setSelectedCallback(callback: GifSelectedCallback) {
        this.callback = callback
    }

    private fun loadTrending() {
        progressSpinner!!.visibility = View.VISIBLE
        helper!!.trends(object : GiphyApiHelper.Callback {
            override fun onResponse(gifs: List<GiphyApiHelper.Gif>) {
                setAdapter(gifs)
            }
        })
    }

    private fun executeQuery(query: String) {
        progressSpinner!!.visibility = View.VISIBLE
        dismissKeyboard()

        helper!!.search(query, object : GiphyApiHelper.Callback {
            override fun onResponse(gifs: List<GiphyApiHelper.Gif>) {
                setAdapter(gifs)
            }
        })
    }

    private fun setAdapter(gifs: List<GiphyApiHelper.Gif>) {
        progressSpinner!!.visibility = View.GONE
        adapter = GiphyAdapter(gifs, object : GiphyAdapter.Callback {
            override fun onClick(item: GiphyApiHelper.Gif) {
                DownloadGif(context as Activity, item.gifUrl, item.name, context.cacheDir.absolutePath, callback).execute()
            }
        }, true)

        recycler!!.layoutManager = GridLayoutManager(context, context.resources.getInteger(R.integer.grid_count))
        recycler!!.adapter = adapter
    }

    private fun dismissKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView!!.windowToken, 0)
    }
}
