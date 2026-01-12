package com.example.cinematch2.Repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.cinematch2.AuthApiService
import com.example.cinematch2.Domain.FilmItemModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainRepository {
    private val api: AuthApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(AuthApiService::class.java)
    }

    companion object {
        private val _latestMovies = MutableLiveData<List<FilmItemModel>>(emptyList())
        private val _watchedMovies = MutableLiveData<List<FilmItemModel>>(emptyList())
        private val _likedMovies = MutableLiveData<List<FilmItemModel>>(emptyList())
        private val _upcomingMovies = MutableLiveData<List<FilmItemModel>>(emptyList())
        private val _recommendedMovies = MutableLiveData<List<FilmItemModel>>(emptyList())
    }

    suspend fun fetchMovies(token: String) {
        try {
            val watchedResponse = api.getWatchedMovies("Bearer $token", page = 1, pageSize = 100)
            val watchedIds = mutableSetOf<Int>()
            val watchedMoviesList = mutableListOf<FilmItemModel>()

            if (watchedResponse.isSuccessful) {
                val watchedList = watchedResponse.body()?.movies ?: emptyList()
                watchedList.forEach { movie ->
                    movie.watched = true
                    watchedIds.add(movie.id)
                    watchedMoviesList.add(movie)
                }
                _watchedMovies.postValue(watchedMoviesList)
            } else {
                _watchedMovies.postValue(emptyList())
            }

            val likedResponse = api.getLikedMovies("Bearer $token", page = 1, pageSize = 100)
            val likedIds = mutableSetOf<Int>()
            val likedMoviesList = mutableListOf<FilmItemModel>()

            if (likedResponse.isSuccessful) {
                val likedList = likedResponse.body()?.movies ?: emptyList()
                likedList.forEach { movie ->
                    movie.like = true
                    movie.watched = true
                    likedIds.add(movie.id)
                    likedMoviesList.add(movie)
                }
                _likedMovies.postValue(likedMoviesList)
            } else {
                _likedMovies.postValue(emptyList())
            }

            val latestResponse = api.getLatestMovies(page = 1, pageSize = 10)
            if (latestResponse.isSuccessful) {
                val list = latestResponse.body()?.movies?.toMutableList() ?: mutableListOf()
                list.forEach { movie ->
                    movie.watched = watchedIds.contains(movie.id)
                    movie.like = likedIds.contains(movie.id)
                }
                _latestMovies.postValue(list)
            } else {
                _latestMovies.postValue(emptyList())
            }

            val upcomingResponse = api.getUpcomingMovies(page = 1, pageSize = 10)
            if (upcomingResponse.isSuccessful) {
                val list = upcomingResponse.body()?.movies?.toMutableList() ?: mutableListOf()
                list.forEach { movie ->
                    movie.watched = watchedIds.contains(movie.id)
                    movie.like = likedIds.contains(movie.id)
                }
                _upcomingMovies.postValue(list)
            } else {
                _upcomingMovies.postValue(emptyList())
            }

            val recommendedResponse = api.getRecommendedMovies("Bearer $token", page = 1, pageSize = 10)
            if (recommendedResponse.isSuccessful) {
                val list = recommendedResponse.body()?.movies?.toMutableList() ?: mutableListOf()
                list.forEach { movie ->
                    movie.watched = watchedIds.contains(movie.id)
                    movie.like = likedIds.contains(movie.id)
                }
                _recommendedMovies.postValue(list)
            } else {
                _recommendedMovies.postValue(emptyList())
            }

        } catch (e: Exception) {
            _watchedMovies.postValue(emptyList())
            _likedMovies.postValue(emptyList())
            _latestMovies.postValue(emptyList())
            _upcomingMovies.postValue(emptyList())
            _recommendedMovies.postValue(emptyList())
        }
    }

    suspend fun fetchMovieDetails(movieId: Int): FilmItemModel? {
        return try {
            val response = api.getMovieDetails(movieId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun toggleWatchStatus(token: String, movieId: Int, isMarkingAsWatched: Boolean): Boolean {
        return try {
            val response = if (isMarkingAsWatched) {
                api.markWatched(movieId, "Bearer $token", "")
            } else {
                api.unmarkWatched(movieId, "Bearer $token")
            }
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleLikeStatus(token: String, movieId: Int, currentlyLiked: Boolean): Boolean {
        return try {
            val response = if (currentlyLiked) {
                api.unmarkLiked(movieId, "Bearer $token")
            } else {
                api.markLiked(movieId, "Bearer $token")
            }
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    fun updateMovieInAllLists(movie: FilmItemModel) {
        val movieId = movie.id
        val isWatched = movie.watched
        val isFavorite = movie.like

        fun updateLiveDataList(liveData: MutableLiveData<List<FilmItemModel>>) {
            val currentList = liveData.value.orEmpty()
            val updatedList = currentList.map {
                if (it.id == movieId) {
                    it.copy(watched = isWatched, like = isFavorite)
                } else it
            }
            liveData.postValue(updatedList)
        }

        updateLiveDataList(_latestMovies)
        updateLiveDataList(_upcomingMovies)
        updateLiveDataList(_recommendedMovies)

        val currentWatched = _watchedMovies.value?.toMutableList() ?: mutableListOf()
        if (isWatched) {
            val existing = currentWatched.find { it.id == movieId }
            if (existing == null) {
                currentWatched.add(movie.copy(watched = true, like = isFavorite))
            } else {
                val index = currentWatched.indexOfFirst { it.id == movieId }
                currentWatched[index] = existing.copy(watched = true, like = isFavorite)
            }
        } else {
            currentWatched.removeAll { it.id == movieId }
        }
        _watchedMovies.postValue(currentWatched)

        val currentLiked = _likedMovies.value?.toMutableList() ?: mutableListOf()
        if (isFavorite) {
            val existing = currentLiked.find { it.id == movieId }
            if (existing == null) {
                currentLiked.add(movie.copy(like = true, watched = isWatched))
            } else {
                val index = currentLiked.indexOfFirst { it.id == movieId }
                currentLiked[index] = existing.copy(like = true, watched = isWatched)
            }
        } else {
            currentLiked.removeAll { it.id == movieId }
        }
        _likedMovies.postValue(currentLiked)
    }

    suspend fun searchMovies(query: String): List<FilmItemModel> {
        return try {
            val response = api.searchMovies(query = query, page = 1, pageSize = 10)
            if (response.isSuccessful) {
                response.body()?.movies ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun checkSpecificStatus(movieId: Int, token: String): AuthApiService.WatchResponse? {
        return try {
            val response = api.checkWatchStatus(movieId, "Bearer $token")
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getLatestMovies(): LiveData<List<FilmItemModel>> = _latestMovies
    fun getWatchedMovies(): LiveData<List<FilmItemModel>> = _watchedMovies
    fun getLikedMovies(): LiveData<List<FilmItemModel>> = _likedMovies
    fun getUpcomingMovies(): LiveData<List<FilmItemModel>> = _upcomingMovies
    fun getRecommendedMovies(): LiveData<List<FilmItemModel>> = _recommendedMovies
}