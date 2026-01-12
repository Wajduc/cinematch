package com.example.cinematch2.ViewModel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinematch2.Domain.FilmItemModel
import com.example.cinematch2.Repository.MainRepository
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val repository = MainRepository()
    val latestMovies: LiveData<List<FilmItemModel>> = repository.getLatestMovies()
    val watchedMovies: LiveData<List<FilmItemModel>> = repository.getWatchedMovies()
    val upcomingMovies: LiveData<List<FilmItemModel>> = repository.getUpcomingMovies()
    val recommendedMovies: LiveData<List<FilmItemModel>> = repository.getRecommendedMovies()
    val likedMovies: LiveData<List<FilmItemModel>> = repository.getLikedMovies()

    var searchText by mutableStateOf("")
    private val _searchResults = mutableStateListOf<FilmItemModel>()
    val searchResults: List<FilmItemModel> get() = _searchResults

    fun onSearchTextChange(newText: String) {
        searchText = newText
        if (newText.length >= 2) {
            viewModelScope.launch {
                val results = repository.searchMovies(newText)

                val watchedIds = watchedMovies.value?.map { it.id }.orEmpty()
                val likedIds = likedMovies.value?.map { it.id }.orEmpty()

                results.forEach { movie ->
                    if (watchedIds.contains(movie.id)) {
                        movie.watched = true
                    }
                    if (likedIds.contains(movie.id)) {
                        movie.like = true
                    }
                }

                _searchResults.clear()
                _searchResults.addAll(results)
            }
        } else {
            _searchResults.clear()
        }
    }

    fun updateMovieWatchState(movie: FilmItemModel) {
        repository.updateMovieInAllLists(movie)

        val searchIndex = _searchResults.indexOfFirst { it.id == movie.id }
        if (searchIndex != -1) {
            _searchResults[searchIndex] = _searchResults[searchIndex].copy(
                watched = movie.watched,
                like = movie.like
            )
        }
    }

    fun updateMovieLikeState(movie: FilmItemModel) {
        repository.updateMovieInAllLists(movie)

        val searchIndex = _searchResults.indexOfFirst { it.id == movie.id }
        if (searchIndex != -1) {
            _searchResults[searchIndex] = _searchResults[searchIndex].copy(
                watched = movie.watched,
                like = movie.like
            )
        }
    }

    fun loadInitialData(token: String) {
        viewModelScope.launch {
            try {
                repository.fetchMovies(token)
            } catch (e: Exception) {}
        }
    }
}