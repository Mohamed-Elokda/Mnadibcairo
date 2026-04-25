package com.example.myapplication.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LoginViewModel @Inject constructor(private val repository: UserRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    fun login(context: Context, username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading

            val result = repository.signInWithUsername(username, password)

            result.onSuccess {
                profile ->
                _loginState.value = LoginState.Success
                Prefs.setLoggedIn( context, true)
                Prefs.saveUserData(
                    context,
                    profile.id.toString(),
                    profile.profile.toString()
                )
            }.onFailure { error ->
                _loginState.value = LoginState.Error(error.message ?: "حدث خطأ غير متوقع")
            }
        }
    }
}

// كلاس لتمثيل حالات الشاشة
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}