package com.example.tapestry.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import com.example.tapestry.activities.MainActivity
import com.example.tapestry.ui.settings.SettingsFragment

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences(MainActivity.SharedPrefFile, Context.MODE_PRIVATE)
        val setAlarm = prefs.getBoolean(SettingsFragment.RANDOM_ENABLED, false)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && setAlarm) {
            val wallpaperAlarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val wallpaperChangeIntent = Intent(context, ChangeWallpaper::class.java).apply {
                action = "CHANGE_WALL"
            }
            val pending = PendingIntent.getBroadcast(
                context, 2, wallpaperChangeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val chosenTime = prefs.getInt(SettingsFragment.RANDOM_INTERVAL, 0)
            val interval = (chosenTime + 1) * 60 * 60 * 1000
            val triggerTime = SystemClock.elapsedRealtime() + interval
            Toast.makeText(context, "Random refresh enabled", Toast.LENGTH_SHORT).show()
            wallpaperAlarm.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                interval.toLong(),
                pending
            )
            ChangeWallpaper.changeWall(context)
        }
    }
}