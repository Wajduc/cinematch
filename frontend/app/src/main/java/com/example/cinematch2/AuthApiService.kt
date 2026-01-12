package com.example.cinematch2

import com.example.cinematch2.Domain.FilmItemModel
import com.example.cinematch2.Domain.MovieResponse
import com.example.cinematch2.Domain.TokenResponse
import com.example.cinematch2.Domain.UserSignupRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApiService {
    data class WatchResponse(
        val watched: Boolean,
        val like: Boolean
    )

    @POST("/auth/signup")
    suspend fun signup(@Body request: UserSignupRequest): Response<Unit>

    @FormUrlEncoded
    @POST("/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @GET("/movies/search")
    suspend fun searchMovies(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<MovieResponse>

    @GET("/movies/latest")
    suspend fun getLatestMovies(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<MovieResponse>

    @GET("/movies/upcoming")
    suspend fun getUpcomingMovies(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<MovieResponse>

    @GET("/movies/recommended")
    suspend fun getRecommendedMovies(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<MovieResponse>

    @GET("/movies/watch/me")
    suspend fun getWatchedMovies(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<MovieResponse>

    @GET("/movies/like/me")
    suspend fun getLikedMovies(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): Response<MovieResponse>

    @GET("/movies/watch/{movie_id}")
    suspend fun checkWatchStatus(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") token: String
    ): Response<WatchResponse>

    @POST("/movies/watch/{movie_id}")
    suspend fun markWatched(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") token: String,
        @Body body: String = ""
    ): Response<Unit>

    @DELETE("/movies/watch/{movie_id}")
    suspend fun unmarkWatched(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    @POST("/movies/like/{movie_id}")
    suspend fun markLiked(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") token: String,
        @Body body: String = ""
    ): Response<Unit>

    @DELETE("/movies/like/{movie_id}")
    suspend fun unmarkLiked(
        @Path("movie_id") movieId: Int,
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("/movies/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int
    ): Response<FilmItemModel>
}
