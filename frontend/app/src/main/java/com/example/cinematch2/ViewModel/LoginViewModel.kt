package com.example.cinematch2.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinematch2.Repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()
    fun login(user: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.login(user, pass)
            val success = result != null
            onResult(success)
        }
    }
}