package com.example.myapplication.ui.outbound

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.domin.model.Customer
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Resource
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.useCase.FetchRemoteOutboundsUseCase
import com.example.myapplication.domin.useCase.ProcessOutboundUseCase
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// OutboundViewModel.kt
// OutboundViewModel.kt
class OutboundViewModel(
    private val fetchRemoteUseCase: FetchRemoteOutboundsUseCase,
    private val processOutboundUseCase: ProcessOutboundUseCase,
    private val outboundRepo: OutboundRepo,
    private val customerRepo: CustomerRepo,

    ) : ViewModel() {

    // هذا هو المتغير الذي كان ينقصك
    private val _allInvoices = MutableLiveData<List<Outbound>>()
    val allInvoices: LiveData<List<Outbound>> = _allInvoices

    // حالة المزامنة
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()
    // جلب الأصناف والعملاء (تحويل Flow إلى LiveData)
    val allItems: LiveData<List<Stock>> = outboundRepo.getAllItems().asLiveData()
    fun allCustomers(context: Context): LiveData<List<com.example.myapplication.domin.model.Customer>> {
       return customerRepo.getAllCustomers(Prefs.getUserId(context)?:"").asLiveData() // افترضنا userId = 1

    }

    fun loadInvoices(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            // استدعاء الدالة من الـ Repo (تأكد من وجودها في OutboundRepo)
            outboundRepo.getAllOutbounds(Prefs.getUserId(context)?:"").collect { list ->
                _allInvoices.postValue(list)
            }
        }
    }
    fun syncWithServer(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true

            val result = fetchRemoteUseCase(Prefs.getUserId(context)?:"")

            result.onFailure { error ->
                _errorMessage.emit("فشل جلب البيانات: ${error.message}")
            }

            _isSyncing.value = false
        }
    }
    // مثال سريع داخل الـ ViewModel أو الـ Repository قبل الحفظ
    fun deleteInvoice(outbound: Outbound,context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // تنفيذ عملية الحذف وإرجاع المخزن
                outboundRepo.deleteInvoiceAndRollbackAll(outbound)

                // إعادة تحميل الفواتير لتحديث الجدول في الواجهة
                loadInvoices(context)

                withContext(Dispatchers.Main) {
                    // إظهار رسالة نجاح (يمكنك استخدام SingleLiveEvent هنا)
                }
            } catch (e: Exception) {
                // معالجة الخطأ
            }
        }
    }
    suspend fun addCustomerQuickly(name: String,context: Context): Int {
        val newCustomer = com.example.myapplication.domin.model.Customer(
            id = 0,
            customerName = name,
            userId = Prefs.getUserId(context)?:"", // معرف المندوب الحالي
            CustomerNum = (1000..9999).random(),

            customerDebt = 0.0
        )

        // افترضنا أن Repo يعيد ID العميل الجديد بعد الحفظ
        return customerRepo.insertCustomer(newCustomer)
    }
    // دالة حفظ الفاتورة (التي أصلحناها سابقاً)
    private val _saveResult = MutableLiveData<Resource<Long>>()
    val saveResult: LiveData<Resource<Long>> = _saveResult

    fun saveInvoice(outbound: Outbound, details: List<OutboundDetails>) {
        viewModelScope.launch(Dispatchers.IO) {
            _saveResult.postValue(Resource.Loading)
            val result = processOutboundUseCase(outbound, details)
            _saveResult.postValue(result)
        }
    }
    fun refreshCustomers(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            customerRepo.syncCustomersFromServer(userId)
        }
    }

    fun checkAndSyncItemsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            // فحص العدد في Room
            val count = outboundRepo.getItemsCount()

            if (count == 0) {
                // الجدول فارغ، نسحب من السيرفر
                outboundRepo.syncItemsFromServer()
            }
        }
    }
}