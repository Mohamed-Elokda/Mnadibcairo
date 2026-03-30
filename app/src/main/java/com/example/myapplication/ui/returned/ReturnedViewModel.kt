package com.example.myapplication.ui.returned

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.domin.model.Customer
import com.example.myapplication.domin.model.ItemHistoryProjectionModel
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.useCase.AddReturnedUseCase
import com.example.myapplication.domin.useCase.GetAllCustomersUseCase
import com.example.myapplication.domin.useCase.GetAllReturnedUseCase
import com.example.myapplication.domin.useCase.GetCustomerItemsUseCase
import com.example.myapplication.domin.useCase.GetItemHistoryUseCase
import com.example.myapplication.domin.useCase.GetLastPriceUseCase
import com.example.myapplication.domin.useCase.GetReturnedDetailsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class ReturnedViewModel(
    private val getCustomerItemsUseCase: GetCustomerItemsUseCase,
    private val getLastPriceUseCase: GetLastPriceUseCase,
    private val getAllReturnedUseCase: GetAllReturnedUseCase,
    private val gettReturnedDetailsUseCase: GetReturnedDetailsUseCase,
    private val addReturnedUseCase: AddReturnedUseCase,
    private val getAllCustomersUseCase: GetAllCustomersUseCase,
    private val getItemHistoryUseCase: GetItemHistoryUseCase // أضف هذا
) : ViewModel() {

    // حالة عرض القائمة (للشاشة الرئيسية للمرتجعات)
    private val _returnedList = MutableStateFlow<List<ReturnedWithNameModel>>(emptyList())
    val returnedList: StateFlow<List<ReturnedWithNameModel>> = _returnedList.asStateFlow()

    // حالة العملية (نجاح / خطأ / تحميل)
    private val _isOperationSuccess = MutableStateFlow<Boolean?>(null)
    val isOperationSuccess: StateFlow<Boolean?> = _isOperationSuccess.asStateFlow()
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()
    private val _customerItems = MutableStateFlow<List<ItemsEntity>>(emptyList())
    val customerItems: StateFlow<List<ItemsEntity>> = _customerItems.asStateFlow()
    // داخل ReturnedViewModel
// ... داخل الـ ViewModel ...
// تحويل الـ Flow القادم من الـ UseCase إلى StateFlow مباشرة
    fun allCustomers(context: Context): StateFlow<List<Customer>> {return getAllCustomersUseCase.execute(context)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )}
    init {
        loadAllReturned()
    }
    private val _lastPrice = MutableStateFlow<Double>(0.0)
    val lastPrice: StateFlow<Double> = _lastPrice.asStateFlow()
    private val _returnedDetails = MutableStateFlow<List<ReturnedDetailsModel>>(emptyList())
    val returnedDetails = _returnedDetails.asStateFlow()

    fun loadReturnedDetails(id: Int) {
        viewModelScope.launch {
            gettReturnedDetailsUseCase(id).collect {
                _returnedDetails.value = it
            }
        }
    }
    // 1. استدعاء هذه الدالة عند اختيار عميل
    fun loadItemsForCustomer(customerId: Int) {
        viewModelScope.launch {
            getCustomerItemsUseCase(customerId).collect { items ->
                _customerItems.value = items
            }
        }
    }

    private val _itemHistory = MutableStateFlow<List<ItemHistoryProjectionModel>>(emptyList())
    val itemHistory: StateFlow<List<ItemHistoryProjectionModel>> = _itemHistory.asStateFlow()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()
    fun loadItemHistory(customerId: Int, itemId: Int) {
        viewModelScope.launch {
            getItemHistoryUseCase(customerId, itemId).collect { history ->
                _itemHistory.value = history
            }
        }
    }

    // تأكد من تصفير السجل عند إغلاق النافذة
    fun clearHistory() {
        _itemHistory.value = emptyList()
    }

    // 2. استدعاء هذه الدالة عند اختيار صنف
    fun loadLastPrice(customerId: Int, itemId: Int) {
        viewModelScope.launch {
            val price = getLastPriceUseCase(customerId, itemId)
            _lastPrice.value = price ?: 0.0
        }
    }
    // 1. جلب كافة المرتجعات
    private fun loadAllReturned() {
        viewModelScope.launch { // احذف Dispatchers.IO من هنا
            getAllReturnedUseCase.execute().collect { list ->
                _returnedList.value = list
            }
        }
    }
    // 2. إضافة مرتجع جديد
    fun addReturned(returned: ReturnedModel, details: List<ReturnedDetailsModel>,debtAmount: Double,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                addReturnedUseCase.saveLocally(returned, details,debtAmount)
                _isOperationSuccess.value = true
            } catch (e: Exception) {
                _isOperationSuccess.value = false
            }
        }
    }

    // إعادة تعيين الحالة بعد إتمام العملية
    fun resetStatus() {
        _isOperationSuccess.value = null
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = addReturnedUseCase.syncWithServer()
            result.onFailure {
                _uiEvent.emit("فشلت المزامنة: ${it.message}")
            }
            _isSyncing.value = false
        }
    }
}