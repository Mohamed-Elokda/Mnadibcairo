package com.example.myapplication.ui.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.useCase.FetchRemoteOutboundsUseCase
import com.example.myapplication.domin.useCase.ProcessOutboundUseCase
import com.example.myapplication.ui.outbound.OutboundViewModel

class OutboundViewModelFactory(
    private val fetchRemoteUseCase: FetchRemoteOutboundsUseCase,
    private val processUseCase: ProcessOutboundUseCase,
    private val outboundRepo: OutboundRepo,
    private val customerRepo: CustomerRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OutboundViewModel(fetchRemoteUseCase,processUseCase, outboundRepo, customerRepo) as T
    }
}