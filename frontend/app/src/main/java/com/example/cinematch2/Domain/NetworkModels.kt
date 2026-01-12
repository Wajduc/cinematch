package com.example.cinematch2.Domain

data class UserSignupRequest(
    val username: String,
    val email: String,
    val password: String
)

data class TokenResponse(
    val access_token: String,
    val token_type: String
)