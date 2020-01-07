package xyz.klinker.giphy

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

internal class DownloadGif @JvmOverloads constructor(var activity: Activity, var gifURL: String, var name: String, var saveLocation: String, var callback: GifSelectedCallback? = null) : AsyncTask<Void, Void, Uri>() {
    private lateinit var dialog: ProgressDialog

    public override fun onPreExecute() {
        dialog = ProgressDialog(activity)
        dialog.isIndeterminate = true
        dialog.setCancelable(false)
        dialog.setMessage(activity.getString(R.string.downloading))
        dialog.show()
    }

    override fun doInBackground(vararg arg0: Void): Uri? {
        try {
            return saveGiffy(activity, gifURL, name, saveLocation)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

    }

    override fun onPostExecute(downloadedTo: Uri?) {
        try {
            if (callback != null) {
                callback!!.onGifSelected(downloadedTo!!)
                dialog.dismiss()
            } else if (downloadedTo != null) {
                activity.setResult(Activity.RESULT_OK, Intent().setData(downloadedTo))
                activity.finish()

                try {
                    dialog.dismiss()
                } catch (e: Exception) {
                    Log.e("Exception", e.toString())
                }

            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Toast.makeText(activity, R.string.error_downloading_gif,
                            Toast.LENGTH_SHORT).show()
                    activity.finish()
                } else {
                    Toast.makeText(activity, R.string.error_downloading_gif_permission,
                            Toast.LENGTH_SHORT).show()
                    activity.finish()
                }
            }
        } catch (e: IllegalStateException) {
            Log.e("Exception", e.toString())
        }

    }

    @Throws(Exception::class)
    private fun saveGiffy(context: Context, gifURL: String, name: String, saveLocation: String?): Uri {
        var name = name
        var saveLocation = saveLocation
        name = "$name.gif"

        //Default save location to internal storage if no location set.
        if (saveLocation == null) {
            saveLocation = context.filesDir.path
        }

        //Create save location if not exist.
        val dir = File(saveLocation!!)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val saveGif = File(saveLocation, name)
        if (!saveGif.createNewFile()) {
            //File exists, return existing File URI.
            return Uri.fromFile(saveGif)
        } else {
            //Download GIF via Glide, then save to specified location.
            val gifDownload = Glide.with(context).downloadOnly().load(gifURL).submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get()
            val inStream = FileInputStream(gifDownload)
            val outStream = FileOutputStream(saveGif)
            val inChannel = inStream.channel
            val outChannel = outStream.channel
            inChannel.transferTo(0, inChannel.size(), outChannel)
            inStream.close()
            outStream.close()
        }

        return Uri.fromFile(saveGif)
    }
}