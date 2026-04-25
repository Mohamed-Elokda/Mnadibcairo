package com.example.myapplication.ui.outbound

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.entity.OutboundDetailWithItemName
import com.example.myapplication.domin.model.CustomerModel
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.Resource
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.useCase.FetchRemoteOutboundsUseCase
import com.example.myapplication.domin.useCase.UpdateOutboundUseCase
import com.example.myapplication.domin.useCase.outboundUseCases.DeleteOutboundUseCase
import com.example.myapplication.domin.useCase.outboundUseCases.GetAllOutboundDetails
import com.example.myapplication.domin.useCase.outboundUseCases.GetAllOutboundInvoices
import com.example.myapplication.domin.useCase.outboundUseCases.ProcessOutboundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OutboundViewModel @Inject constructor(

    private val updateOutboundUseCase: UpdateOutboundUseCase,
    private val getAllOutboundDetails: GetAllOutboundDetails,
    private val deleteOutboundUseCase: DeleteOutboundUseCase,
    private val getAllOutboundInvoices: GetAllOutboundInvoices,
    private val fetchRemoteUseCase: FetchRemoteOutboundsUseCase,
    private val processOutboundUseCase: ProcessOutboundUseCase,
    private val outboundRepo: OutboundRepo,
    private val customerRepo: CustomerRepo,

    ) : ViewModel() {

    private val _allInvoices = MutableLiveData<List<Outbound>>()
    val allInvoices: LiveData<List<Outbound>> = _allInvoices

    private val _InvoiceDetails = MutableLiveData<List<OutboundDetailWithItemName>>()
    val invoiceDetails: LiveData<List<OutboundDetailWithItemName>> = _InvoiceDetails
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()


    val allItems: LiveData<List<Stock>> = outboundRepo.getAllItems().asLiveData()
    fun allCustomers(context: Context): LiveData<List<CustomerModel>> {
       return customerRepo.getAllCustomers(Prefs.getUserId(context)?:"").asLiveData() // افترضنا userId = 1

    }

    fun loadInvoices(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            getAllOutboundInvoices(Prefs.getUserId(context)?:"").collect { list ->
                _allInvoices.postValue(list)
            }
        }
    }

    fun loadInvoiceDetails(outboundId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getAllOutboundDetails(outboundId).collect { list ->
                _InvoiceDetails.postValue(list)
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
    fun deleteInvoice(outbound: Outbound,context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteOutboundUseCase(outbound)

                loadInvoices(context)

                withContext(Dispatchers.Main) {
                }
            } catch (e: Exception) {
            }
        }
    }
    suspend fun addCustomerQuickly(name: String,context: Context): Int {
        val newCustomerModel = CustomerModel(
            id = 0,
            customerName = name,
            userId = Prefs.getUserId(context) ?: "",
            customerNum = (1000..9999).random(),

            customerDebt = 0.0,
            firstCustomerDebt = 0.0,
            updatedAt = System.currentTimeMillis()
        )

        // افترضنا أن Repo يعيد ID العميل الجديد بعد الحفظ
        return customerRepo.insertCustomer(newCustomerModel)
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
    fun updateInvoice(outbound: Outbound, details: List<OutboundDetails>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateOutboundUseCase.execute(outbound, details)
            } catch (e: Exception) {
                _saveResult.postValue(Resource.Error(e.message ?: "خطأ في التحديث"))
            }
        }
    }
    fun checkAndSyncItemsIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {

            val count = outboundRepo.getItemsCount()

            if (count == 0) {
                outboundRepo.syncItemsFromServer()
            }
        }
    }




}