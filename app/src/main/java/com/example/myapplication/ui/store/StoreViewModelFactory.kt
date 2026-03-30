package com.example.myapplication.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domin.useCase.GetStockUseCase

class StoreViewModelFactory(
    private val getStockUseCase: GetStockUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoreViewModel(getStockUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}