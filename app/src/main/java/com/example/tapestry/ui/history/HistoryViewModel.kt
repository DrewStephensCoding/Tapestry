package com.example.tapestry.ui.history

import android.app.Application
import androidx.lifecycle.*
import com.example.tapestry.database.HistoryRepo
import com.example.tapestry.objects.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: HistoryRepo = HistoryRepo(application.applicationContext)
    val allHist: LiveData<List<HistoryItem>>

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteAll()
    }

    fun insert(history: HistoryItem) = viewModelScope.launch(Dispatchers.IO) {
        repo.insert(history)
    }

    fun deleteHist(history: HistoryItem) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteHist(history)
    }

    init {
        allHist = repo.allHistoryItems
    }

}