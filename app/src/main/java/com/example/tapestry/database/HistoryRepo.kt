package com.example.tapestry.database

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.tapestry.objects.HistoryItem

class HistoryRepo constructor(application: Context) {
    private val historyDao: HistoryDAO
    val allHistoryItems: LiveData<List<HistoryItem>>

    suspend fun insert(history: HistoryItem) {
        historyDao.insert(history)
    }

    suspend fun deleteAll() {
        historyDao.deleteAll()
    }

    suspend fun deleteHist(history: HistoryItem) {
        historyDao.deleteHistoryItem(history)
    }

    init {
        val db = HistoryRoomDB.getDatabase(application)
        historyDao = db!!.historyDAO()
        allHistoryItems = historyDao.allHistoryItems
    }

}