package com.example.myapplication.ui.returned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.useCase.AddReturnedUseCase
import com.example.myapplication.domin.useCase.GetAllCustomersUseCase
import com.example.myapplication.domin.useCase.GetAllReturnedUseCase
import com.example.myapplication.domin.useCase.GetCustomerItemsUseCase
import com.example.myapplication.domin.useCase.GetItemHistoryUseCase
import com.example.myapplication.domin.useCase.GetLastPriceUseCase
import com.example.myapplication.domin.useCase.GetReturnedDetailsUseCase

class ReturnedViewModelFactory(
    private val getCustomerItemsUseCase: GetCustomerItemsUseCase,
    private val getLastPriceUseCase: GetLastPriceUseCase,
    private val getAllReturnedUseCase: GetAllReturnedUseCase,
    private val getReturnedDetailsUseCase: GetReturnedDetailsUseCase,
    private val addReturnedUseCase: AddReturnedUseCase,
    private val getAllCustomersUseCase: GetAllCustomersUseCase,
    private val getItemHistoryUseCase: GetItemHistoryUseCase

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReturnedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReturnedViewModel(getCustomerItemsUseCase, getLastPriceUseCase,getAllReturnedUseCase,
                getReturnedDetailsUseCase,addReturnedUseCase,getAllCustomersUseCase,getItemHistoryUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}