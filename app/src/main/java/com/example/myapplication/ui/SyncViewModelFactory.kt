package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.repository.ReturnedRepo
import com.example.myapplication.domin.repository.StockRepository

class SyncViewModelFactory(
    private val customerRepo: CustomerRepo,
    private val outboundRepo: OutboundRepo,
    private val returnedRepo: ReturnedRepo,
    private val stockReop: StockRepository,
    private val userId: String,
    private val iboundRepo: InboundRepositoryImpl
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SyncViewModel(customerRepo, outboundRepo, returnedRepo, stockReop,userId,iboundRepo) as T
    }
}