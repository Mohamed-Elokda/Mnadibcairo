package com.example.myapplication.domin.useCase

import android.content.Context
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.model.CustomerModel
import com.example.myapplication.domin.repository.CustomerRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllCustomersUseCase @Inject constructor(
    private val repository: CustomerRepo,
    @ApplicationContext private val context: Context // Hilt هيجيب الـ Context هنا تلقائياً
) {
    // شيلنا الـ Context من هنا عشان الـ ViewModel ميتعاملش معاه
    fun execute(): Flow<List<CustomerModel>> {
        val userId = Prefs.getUserId(context) ?: ""
        return repository.getAllCustomers(userId)
    }
}