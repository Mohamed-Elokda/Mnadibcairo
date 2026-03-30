package com.example.myapplication

import Stakeholder
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityAddStakeholderBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
class AddStakeholderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStakeholderBinding
    private var type = "supplier" // افتراضي مورد

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStakeholderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. استقبال النوع (مورد أو عميل) من الشاشة السابقة
        type = intent.getStringExtra("TYPE") ?: "supplier"
        binding.tvHeaderTitle.text = if (type == "supplier") "إضافة مورد جديد" else "إضافة عميل جديد"

        // 2. زر الحفظ
        binding.btnSave.setOnClickListener {
            saveToSupabase()
        }
    }

    private fun saveToSupabase() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val initialAmount = binding.etInitialBalance.text.toString().toDoubleOrNull() ?: 0.0

        if (name.isEmpty()) {
            binding.etName.error = "يرجى إدخال الاسم"
            return
        }

        // تحديد الإشارة: المورد الدائن (له) موجب، والمدين (عليه) سالب
        // ملحوظة: تأكد من منطق rbOnHim و rbForHim بما يتناسب مع حساباتك
        val finalBalance = if (binding.rbOnHim.isChecked) initialAmount else -initialAmount


    }
}