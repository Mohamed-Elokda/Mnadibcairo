package com.example.myapplication.ui.inbound

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.entity.InboundDetailWithItemName
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.InboundDetails
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.SuppliedModel
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.useCase.AddInboundUseCase
import com.example.myapplication.domin.useCase.GetInboundDetailsUseCase
import kotlinx.coroutines.launch

class InboundViewModel(
    private val addInboundUseCase: AddInboundUseCase,
    private val getInboundDetailsUseCase: GetInboundDetailsUseCase, // أضفه هنا
    private val repository: IInboundRepository // ستحتاجه لباقي الدوال
) : ViewModel() {

    // لغرض عرض المخزن في الشاشة
    val stockData = repository.getStock().asLiveData()
    val allItems: LiveData<List<Items>> = repository.getAllItems().asLiveData()
    // لغرض عرض الفواتير السابقة
    fun getInbounds(userId: String) = repository.getAllInbounds(userId).asLiveData()

    val allSupplied: LiveData<List<SuppliedModel>> = repository.getAllSupplied().asLiveData()
    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status
    fun saveInvoice(inbound: Inbound, details: List<InboundDetails>) {
        viewModelScope.launch {
            val result = addInboundUseCase(inbound, details)
            result.onSuccess {
                _status.postValue("تم الحفظ وتحديث المخزن بنجاح")
            }
            result.onFailure {
                _status.postValue("خطأ: ${it.message}")
            }
        }
    }
    fun getInboundDetails(inboundId: Long): LiveData<List<InboundDetailWithItemName>> {
        return getInboundDetailsUseCase(inboundId).asLiveData()
    }
    fun deleteInboundWithDetails(inbound: Inbound) {
        viewModelScope.launch {
            val result = repository.deleteFullInbound(inbound)
            if (result.isSuccess) {
                _status.postValue("تم حذف الفاتورة وكافة تفاصيلها")
            } else {
                _status.postValue("فشل الحذف: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    fun importBulkItems(items: List<Items>) {
        viewModelScope.launch {
            // تحويل القائمة بالكامل باستخدام المابر (toEntity)
            val entities = items.map { it.toEntity() }
            repository.insertItemsList(entities)
        }
    }
}