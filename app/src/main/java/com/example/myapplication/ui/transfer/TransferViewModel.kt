package com.example.myapplication.ui.transfer

import androidx.lifecycle.*
import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetailWithItemName
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.model.TransferWithStoreName
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.repository.ITransferRepository
import com.example.myapplication.domin.useCase.AddTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class TransferViewModel @Inject constructor(
    private val addTransferUseCase: AddTransferUseCase,
    private val suppliedrepository: IInboundRepository ,
    private val repository: ITransferRepository // أضفنا المستودع لجلب البيانات
) : ViewModel() {

    // جلب المخازن والأصناف لعرضها في الـ UI
    val allSupplied = suppliedrepository.getAllSupplied().asLiveData()
    val allItems = suppliedrepository.getAllItems().asLiveData()
    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status


    private val _details = MutableStateFlow<List<TransferDetailWithItemName>>(emptyList())
    val details: StateFlow<List<TransferDetailWithItemName>> = _details
    fun deleteTransfer(transferId: String) {
        viewModelScope.launch {
            try {
                repository.deleteFullTransfer(transferId)
                _status.postValue("تم حذف المناقلة بنجاح")
            } catch (e: Exception) {
                _status.postValue("فشل الحذف: ${e.message}")
            }
        }
    }
    fun loadTransferDetails(transferId: String) {
        viewModelScope.launch {
            repository.getTransferDetails(transferId).collect {
                _details.value = it
            }
        }
    }
    // داخل TransferViewModel
    fun getTransfers(userId: String): LiveData<List<TransferWithStoreName>> {
        return repository.getAllTransfers(userId).asLiveData()
    }
    fun executeTransfer(transfer: Transfer, details: List<TransferDetails>) {
        viewModelScope.launch {
            val result = addTransferUseCase(transfer, details)
            result.onSuccess {
                _status.postValue("تمت المناقلة بنجاح وتحديث المخازن")
                // اختياري: تشغيل المزامنة فوراً بعد الحفظ المحلي
            }
            result.onFailure {
                _status.postValue("خطأ: ${it.message}")
            }
        }
    }
}