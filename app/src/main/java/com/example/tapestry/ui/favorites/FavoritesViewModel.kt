package com.example.tapestry.ui.favorites

import android.app.Application
import androidx.lifecycle.*
import com.example.tapestry.database.FavoriteRepo
import com.example.tapestry.objects.WallImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: FavoriteRepo = FavoriteRepo(application.applicationContext)
    val allFav: LiveData<List<WallImage>>

    val favList: List<WallImage>
        get() = repo.favAsList

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteAll()
    }

    fun insert(saved: WallImage) = viewModelScope.launch(Dispatchers.IO) {
        repo.insert(saved)
    }

    fun deleteFavImage(saved: WallImage) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteFav(saved)
    }

    init {
        allFav = repo.allFav
    }

}