package com.example.myapplication.ui.inbound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.useCase.AddInboundUseCase
import com.example.myapplication.domin.useCase.GetInboundDetailsUseCase

class InboundViewModelFactory(
    private val addInboundUseCase: AddInboundUseCase,
    private val getInboundDetailsUseCase: GetInboundDetailsUseCase, // أضفه هنا
    private val repository: IInboundRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InboundViewModel(addInboundUseCase, getInboundDetailsUseCase, repository) as T
    }
}