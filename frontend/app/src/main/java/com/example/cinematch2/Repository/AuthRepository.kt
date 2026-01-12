package com.example.cinematch2.Repository

import com.example.cinematch2.AuthApiService
import com.example.cinematch2.Domain.UserSignupRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8000") // Zamenjaj s svojim FastApi Url
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(AuthApiService::class.java)

    suspend fun signUp(user: String, email: String, pass: String): Boolean {
        return try {
            val response = api.signup(UserSignupRequest(user, email, pass))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun login(user: String, pass: String): String? {
        return try {
            val response = api.login(user, pass)
            if (response.isSuccessful) {
                response.body()?.access_token
            } else null
        } catch (e: Exception) {
            null
        }
    }
}