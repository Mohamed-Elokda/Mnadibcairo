package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityAddCompanyBinding
import com.example.myapplication.ui.MainActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class AddCompanyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCompanyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCompanyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSaveCompany.setOnClickListener {
            saveCompanyToSupabase()
        }
    }

    private fun saveCompanyToSupabase() {
        val name = binding.etCompanyName.text.toString()
        val type = binding.etBusinessType.text.toString()
        val phone = binding.etPhone.text.toString()
        val address = binding.etAddress.text.toString()

        if (name.isEmpty()) {
            binding.etCompanyName.error = "هذا الحقل مطلوب"
            return
        }


    }
}