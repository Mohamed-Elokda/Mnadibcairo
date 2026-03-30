package com.example.myapplication.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.PaymentRepository

class PaymentsViewModelFactory(private val repository: PaymentRepository,private  val customer : CustomerRepo) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // التأكد أن الـ ViewModel المطلوب هو فعلاً PaymentsViewModel
        if (modelClass.isAssignableFrom(PaymentsViewModel::class.java)) {
            return PaymentsViewModel(repository,customer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}