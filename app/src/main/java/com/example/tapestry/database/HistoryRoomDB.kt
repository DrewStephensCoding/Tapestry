package com.example.tapestry.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tapestry.objects.HistoryItem

@Database(entities = [HistoryItem::class], version = 1, exportSchema = false)
abstract class HistoryRoomDB : RoomDatabase() {

    abstract fun historyDAO(): HistoryDAO

    companion object {
        private var SINGLETON: HistoryRoomDB? = null
        fun getDatabase(context: Context): HistoryRoomDB? {
            if (SINGLETON == null) {
                synchronized(HistoryRoomDB::class.java) {
                    if (SINGLETON == null) {
                        //Create Database Here....
                        SINGLETON = Room.databaseBuilder(
                            context.applicationContext,
                            HistoryRoomDB::class.java, "hist_database"
                        )
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return SINGLETON
        }
    }
}