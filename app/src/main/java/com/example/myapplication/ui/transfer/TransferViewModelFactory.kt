package com.example.myapplication.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.repository.ITransferRepository
import com.example.myapplication.domin.useCase.AddTransferUseCase

class TransferViewModelFactory(
    private val repository: ITransferRepository,
    private val suppliedrepository: IInboundRepository,
    private val addTransferUseCase: AddTransferUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransferViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransferViewModel(
                addTransferUseCase, suppliedrepository,repository,

            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}