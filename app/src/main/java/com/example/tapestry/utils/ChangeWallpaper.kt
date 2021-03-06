package com.example.tapestry.utils

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.tapestry.activities.MainActivity
import com.example.tapestry.database.FavoriteRepo
import com.example.tapestry.database.HistoryRepo
import com.example.tapestry.objects.HistoryItem
import com.example.tapestry.ui.settings.SettingsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ChangeWallpaper : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "CHANGE_WALL") {
            changeWall(context)
        }
    }

    companion object {
        fun changeWall(con: Context) {
            val pref = con.getSharedPreferences(MainActivity.SharedPrefFile, Context.MODE_PRIVATE)
            val location = pref!!.getInt(SettingsFragment.RANDOM_LOCATION, 2)
            val width = pref.getInt(SettingsFragment.IMG_WIDTH, 1920)
            val height = pref.getInt(SettingsFragment.IMG_HEIGHT, 1080)
            val favs = FavoriteRepo(con).favAsList
            if (favs.isEmpty()) {
                Toast.makeText(con, "No Favorites", Toast.LENGTH_SHORT).show()
                return
            }

            var randomNum = (0..favs.size).random()
            while (randomNum >= favs.size || randomNum < 0) {
                randomNum = (0..favs.size).random()
            }
            if (randomNum == favs.size) randomNum = favs.size - 1
            val current = favs[randomNum]
            Toast.makeText(con, "Changing Wallpaper...", Toast.LENGTH_SHORT).show()
            val wall: WallpaperManager? =
                con.applicationContext.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
            Glide.with(con)
                .asBitmap()
                .load(current.imgUrl)
                .override(width, height)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || location == 2) {
                            wall?.setBitmap(resource)
                        } else {
                            if (location == 0) {
                                wall?.setBitmap(resource, null, true, WallpaperManager.FLAG_SYSTEM)
                            } else if (location == 1) {
                                wall?.setBitmap(resource, null, true, WallpaperManager.FLAG_LOCK)
                            }
                        }

                        val histItem = HistoryItem(
                            subName = current.subName,
                            setDate = Date().time,
                            source = HistoryItem.REFRESH,
                            imgUrl = current.imgUrl,
                            postLink = current.postLink,
                            previewUrl = current.previewUrl
                        )
                        refreshHistory(histItem, con)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }
                })
        }

        fun refreshHistory(history: HistoryItem, con: Context) =
            CoroutineScope(Dispatchers.IO).launch {
                HistoryRepo(con).insert(history)
            }
    }
}