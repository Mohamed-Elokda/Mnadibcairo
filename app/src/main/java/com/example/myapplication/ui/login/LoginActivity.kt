package com.example.myapplication.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.MainActivity
import com.example.myapplication.R
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.remote.manager.supabase
import com.example.myapplication.data.repository.UserRepository

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. التحقق من تسجيل الدخول أولاً
        if (Prefs.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 2. تجهيز الـ Repository والـ Factory
        val repository = UserRepository(supabase)
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(repository) as T
            }
        }

        // 3. تعريف الـ ViewModel (هذا السطر يجب أن يسبق أي استخدام للـ viewModel)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        // الآن يمكنك تعريف العناصر والـ Listeners
        val etUsername = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // 4. البدء في مراقبة الحالة (الآن الـ viewModel جاهزة)
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        btnLogin.isEnabled = false
                    }
                    is LoginState.Success -> {
                        progressBar.visibility = View.GONE
                        goToMainActivity()
                    }
                    is LoginState.Error -> {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is LoginState.Idle -> {
                        progressBar.visibility = View.GONE
                        btnLogin.isEnabled = true
                    }
                }
            }
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.login(this, username, password)
            }
        }
    }

    private fun goToMainActivity() {

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}