package com.example.tapestry.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tapestry.objects.WallImage

@Database(entities = [WallImage::class], version = 1, exportSchema = false)
abstract class FavoriteRoomDB : RoomDatabase() {

    abstract fun favDAO(): FavDAO

    companion object {
        @Volatile
        private var SINGLETON: FavoriteRoomDB? = null

        fun getDatabase(context: Context): FavoriteRoomDB? {
            if (SINGLETON == null) {
                synchronized(FavoriteRoomDB::class.java) {
                    if (SINGLETON == null) {
                        // Create database here
                        SINGLETON = Room.databaseBuilder(
                            context.applicationContext,
                            FavoriteRoomDB::class.java, "fav_database"
                        ) // Wipes and rebuilds instead of migrating
                            // if no Migration object.
                            .fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
            return SINGLETON
        }
    }
}