package com.example.myapplication.ui.store

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.StockRepoImpl
import com.example.myapplication.domin.useCase.GetItemMovementUseCase

// ملف: StockMovementViewModelFactory.kt
class StockMovementViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockMovementViewModel::class.java)) {
            // هنا تقوم بإنشاء الـ Repo والـ UseCase يدوياً (أو عبر الـ Database class)
            val db = AppDatabase.getDatabase(application)
            val repo = StockRepoImpl(
                db.inboundDao(),
                db.outboundDao(),
                db.returnedDao(),
                stockDao = db.stockDao()
            )
            val useCase = GetItemMovementUseCase(repo)
            return StockMovementViewModel(useCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}