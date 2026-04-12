package com.example.myapplication.ui.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.useCase.GetStockFromServer
import com.example.myapplication.domin.useCase.GetStockUseCase

class StoreViewModelFactory(
    private val context: Context,
    private val getStockUseCase: GetStockUseCase,
    private val getStockFromServer: GetStockFromServer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // استخرج الـ ID هنا ومرره للـ ViewModel كـ String
        val userId = Prefs.getUserId(context) ?: ""
        return StoreViewModel(userId, getStockUseCase, getStockFromServer) as T
    }
}