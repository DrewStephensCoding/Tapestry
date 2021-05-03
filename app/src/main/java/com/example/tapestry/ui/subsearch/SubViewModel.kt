package com.example.tapestry.ui.subsearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tapestry.objects.Subreddit
import com.example.tapestry.utils.RedditAPI
import kotlinx.coroutines.launch

class SubViewModel : ViewModel() {
    var subreddits = MutableLiveData<ArrayList<Subreddit>>()
    var isLoading = MutableLiveData<Boolean>()

    init {
        subreddits.value = ArrayList()
        isLoading.value = false
    }

    fun searchSubs(query: String) {
        subreddits.value?.clear()
        isLoading.value = true
        subreddits.value = subreddits.value

        viewModelScope.launch {
            RedditAPI.loadSearchedSubs(query) {
                subreddits.value?.addAll(it)
                isLoading.postValue(false)
                subreddits.postValue(subreddits.value)
            }
        }
    }
}