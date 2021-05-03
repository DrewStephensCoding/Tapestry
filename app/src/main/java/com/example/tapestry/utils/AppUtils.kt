package com.example.tapestry.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import com.bumptech.glide.Glide
import com.example.tapestry.activities.MainActivity
import com.example.tapestry.activities.PostActivity
import com.example.tapestry.activities.WallpaperActivity
import com.example.tapestry.objects.HistoryItem
import com.example.tapestry.objects.ProgressNotify
import com.example.tapestry.objects.WallImage
import com.example.tapestry.ui.settings.SettingsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AppUtils {
    companion object {

        fun getListRange(p: Int, listLength: Int): IntArray {
            val start = if (p >= 10) p - 10 else 0
            val end = if (p + 10 < listLength) p + 10 else listLength
            val index = if (p >= 10) 10 else p
            return intArrayOf(start, end, index)
        }

        fun getDimensions(con: Context): IntArray {
            val disp = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = con.display
                display?.getRealMetrics(disp)
            } else {
                @Suppress("DEPRECATION")
                (con as Activity).windowManager.defaultDisplay.getMetrics(disp)
            }
            return intArrayOf(disp.widthPixels, disp.heightPixels)
        }

        fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(MainActivity.SharedPrefFile, Context.MODE_PRIVATE)
        }

        fun networkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

        fun getWallDimensions(context: Context): IntArray {
            val dimensions = getDimensions(context)
            val prefs = getPreferences(context)
            return intArrayOf(
                prefs.getInt(SettingsFragment.IMG_WIDTH, dimensions[0]),
                prefs.getInt(SettingsFragment.IMG_HEIGHT, dimensions[1])
            )
        }

        fun getGridImageScale(context: Context): Int {
            return (context.getSharedPreferences(MainActivity.SharedPrefFile, Context.MODE_PRIVATE)
                .getInt(SettingsFragment.LOAD_SCALE, 2) + 1) * 2
        }

        fun getGridImageBitmap(context: Context, url: String): Bitmap {
            val scale = getGridImageScale(context)
            val dimensions = getDimensions(context)
            return Glide.with(context).asBitmap()
                .load(url).error(ColorDrawable(Color.GRAY)).placeholder(ColorDrawable(Color.GRAY))
                .override(dimensions[0] / scale, dimensions[1] / 4).centerCrop().submit().get()
        }

        fun startWallActivity(context: Context, selectedImage: WallImage) {
            val wallActivityIntent = Intent(context, WallpaperActivity::class.java)
            wallActivityIntent.apply {
                putExtra(WallpaperActivity.IMAGE_URL, selectedImage.imgUrl)
                putExtra(WallpaperActivity.SUB_NAME, selectedImage.subName)
                putExtra(WallpaperActivity.PREVIEW_URL, selectedImage.previewUrl)
                putExtra(PostActivity.POST_LINK, selectedImage.postLink)
            }

            context.startActivity(wallActivityIntent)
        }

        fun convertUTC(utc: Long): String {
            val format = SimpleDateFormat("MM-dd-yyyy 'at' HH:mm:ss", Locale.US).format(Date(utc))
            val tempDate = format.trim().split(" at ")
            val date = tempDate[0].trim().split("-")
            val time = tempDate[1].trim().split(":")
            val month = HistoryItem.months[Integer.parseInt(date[0])]
            var hours = Integer.parseInt(time[0])
            val pmam = if (hours > 12) {
                hours -= 12
                "p.m"
            } else {
                "a.m"
            }
            return "$month ${HistoryItem.getOrdinal(Integer.parseInt(date[1]))}, ${date[2]} | ${hours}:${time[1]} $pmam"
        }

        suspend fun downloadAllImages(context: Context, images: ArrayList<String>) {
            val downloadOriginal =
                getPreferences(context).getBoolean(SettingsFragment.DOWNLOAD_ORIGIN, false)
            val notify = ProgressNotify(context, images.size)
            notify.sendNotification()
            val downloadDimensions = getWallDimensions(context)

            withContext(Dispatchers.IO) {
                for (i in images.indices) {
                    val bitmap = if (downloadOriginal) {
                        Glide.with(context).asBitmap().load(images[i]).submit().get()
                    } else {
                        Glide.with(context).asBitmap().load(images[i])
                            .override(downloadDimensions[1]).submit().get()
                    }
                    saveBitmap(bitmap, context)
                    withContext(Dispatchers.Main) {
                        notify.updateProgress(i + 1)
                    }
                }
            }
            notify.finish()
        }

        fun getDate(): String {
            return SimpleDateFormat("MM-dd-yyyy 'at' HH:mm:ss", Locale.US).format(Date())
        }

        fun saveBitmap(bitmap: Bitmap, context: Context): String {
            val fName = (0..999999999).random().toString().replace(" ", "") + ".jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativeLocation: String =
                    Environment.DIRECTORY_PICTURES + File.separator + "Tapestry"

                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fName)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)

                val resolver = context.contentResolver

                var stream: OutputStream? = null
                var uri: Uri? = null

                try {
                    val contentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    uri = resolver.insert(contentUri, contentValues)
                    if (uri == null) {
                        throw IOException("Failed to create new MediaStore record.")
                    }
                    stream = resolver.openOutputStream(uri)
                    if (stream == null) {
                        throw IOException("Failed to get output stream.")
                    }
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                        throw IOException("Failed to save bitmap.")
                    }

                } catch (e: IOException) {
                    if (uri != null) {
                        resolver.delete(uri, null, null)
                    }
                    throw e
                } finally {
                    stream?.close()
                }
            } else {
                val root = Environment.getExternalStorageDirectory().toString()
                val myDir = File("$root/Tapestry")
                myDir.mkdirs()
                val file = File(myDir, fName)
                if (file.exists())
                    file.delete()
                try {
                    val out = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    out.flush()
                    out.close()
                    MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        file.absolutePath,
                        file.name,
                        file.name
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return fName
        }

        suspend fun getSubInfo(subName: String): JSONObject {
            var subInfo = JSONObject()
            withContext(Dispatchers.Default) {
                val endpoint = "https://www.reddit.com/r/$subName/about/.json"
                val jsonString = async { getJsonData(endpoint) }
                try {
                    subInfo = JSONObject(jsonString.await())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            return subInfo
        }

        suspend fun getJsonData(endpoint: String): String {
            var jsonString = ""
            withContext(Dispatchers.IO) {
                var urlConnection: HttpURLConnection? = null
                var reader: BufferedReader? = null
                try {
                    val requestURL = URL(endpoint)

                    urlConnection = requestURL.openConnection() as HttpURLConnection
                    urlConnection.requestMethod = "GET"
                    urlConnection.connect()

                    val inputStream = urlConnection.inputStream
                    reader = BufferedReader(InputStreamReader(inputStream))
                    val builder = StringBuilder()

                    var line: String? = reader.readLine()
                    while (line != null) {
                        if (!isActive) {
                            break
                        }

                        builder.append(line)
                        builder.append("\n")
                        line = reader.readLine()
                    }

                    if (!isActive) {
                        jsonString = ""
                    }

                    if (builder.isEmpty()) {
                        jsonString = ""
                    }

                    jsonString = builder.toString()

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    urlConnection?.disconnect()
                    if (reader != null) {
                        try {
                            reader.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            return jsonString
        }
    }
}
