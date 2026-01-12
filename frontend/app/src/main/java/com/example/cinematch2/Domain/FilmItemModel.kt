package com.example.cinematch2.Domain

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FilmItemModel(
    val id: Int = 0,
    val title: String = "",
    val year: Int = 0,
    val rating: Double = 0.0,
    val runtime: Int = 0,
    @SerializedName("poster_url") val posterPath: String = "",
    @SerializedName("backdrop_url") val backdropPath: String = "",
    @SerializedName("overview") val description: String = "",
    var like: Boolean = false,
    var watched: Boolean = false
) : Serializable

data class MovieResponse(
    val movies: List<FilmItemModel>,
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int
)

