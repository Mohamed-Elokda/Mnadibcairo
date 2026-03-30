package com.example.myapplication.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.useCase.GetStockUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StoreViewModel(private val getStockUseCase: GetStockUseCase) : ViewModel() {

    private val _storeState = MutableStateFlow<StoreState>(StoreState.Idle)
    val storeState = _storeState.asStateFlow()

    init {
        loadStock()
    }

    private fun loadStock() {
        viewModelScope.launch {
            _storeState.value = StoreState.Loading
            try {
                getStockUseCase().collect { stockList ->
                    if (stockList.isEmpty()) {
                        _storeState.value = StoreState.Error("المخزن فارغ حالياً")
                    } else {
                        _storeState.value = StoreState.Success(stockList)
                    }
                }
            } catch (e: Exception) {
                _storeState.value = StoreState.Error("حدث خطأ: ${e.message}")
            }
        }
    }
}


    sealed class StoreState {
        object Idle : StoreState()
        object Loading : StoreState()
        data class Success(val items:List<Stock>) : StoreState()
        data class Error(val message: String) : StoreState()
    }
