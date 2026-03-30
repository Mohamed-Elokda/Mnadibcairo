package com.example.myapplication.domin.useCase

import android.content.Context
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.model.Customer
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.ReturnedRepo
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.flow.Flow

class GetAllCustomersUseCase(private val repository: CustomerRepo) {

    // نستخدم operator invoke لتسهيل استدعاء الكلاس كدالة
    fun execute(context: Context): Flow<List<Customer>> {
        return repository.getAllCustomers(Prefs.getUserId(context)?:"")
    }
}