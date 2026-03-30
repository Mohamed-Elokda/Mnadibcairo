package com.example.myapplication.ui.transfer

import androidx.lifecycle.*
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.repository.ITransferRepository
import com.example.myapplication.domin.useCase.AddTransferUseCase
import kotlinx.coroutines.launch

class TransferViewModel(
    private val addTransferUseCase: AddTransferUseCase,
    private val suppliedrepository: IInboundRepository ,
    private val repository: ITransferRepository // أضفنا المستودع لجلب البيانات
) : ViewModel() {

    // جلب المخازن والأصناف لعرضها في الـ UI
    val allSupplied = suppliedrepository.getAllSupplied().asLiveData()
    val allItems = suppliedrepository.getAllItems().asLiveData()
    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    fun executeTransfer(transfer: Transfer, details: List<TransferDetails>) {
        viewModelScope.launch {
            val result = addTransferUseCase(transfer, details)
            result.onSuccess {
                _status.postValue("تمت المناقلة بنجاح وتحديث المخازن")
                // اختياري: تشغيل المزامنة فوراً بعد الحفظ المحلي
                repository.syncTransfers()
            }
            result.onFailure {
                _status.postValue("خطأ: ${it.message}")
            }
        }
    }
}