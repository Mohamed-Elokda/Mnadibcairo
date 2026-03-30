package com.example.myapplication.ui.customerState


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domin.useCase.GetCustomerStatementUseCase

class StatementViewModelFactory(
    private val getCustomerStatementUseCase: GetCustomerStatementUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatementViewModel(getCustomerStatementUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}