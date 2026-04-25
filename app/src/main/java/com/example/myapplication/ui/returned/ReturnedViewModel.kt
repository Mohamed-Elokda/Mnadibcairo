package com.example.myapplication.ui.returned

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.domin.model.CustomerModel
import com.example.myapplication.domin.model.ItemHistoryProjectionModel
import com.example.myapplication.domin.model.Resource
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.useCase.AddReturnedUseCase
import com.example.myapplication.domin.useCase.DeleteReturnedUsecase
import com.example.myapplication.domin.useCase.GetAllCustomersUseCase
import com.example.myapplication.domin.useCase.GetAllReturnedUseCase
import com.example.myapplication.domin.useCase.GetItemHistoryUseCase
import com.example.myapplication.domin.useCase.GetLastPriceUseCase
import com.example.myapplication.domin.useCase.GetReturnedDetailsUseCase
import com.example.myapplication.domin.useCase.UpdateReturnedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.emptyList
@HiltViewModel
class ReturnedViewModel @Inject constructor(
    private val deleteReturnedUsecase: DeleteReturnedUsecase,
    private val getLastPriceUseCase: GetLastPriceUseCase,
    private val getAllReturnedUseCase: GetAllReturnedUseCase,
    private val gettReturnedDetailsUseCase: GetReturnedDetailsUseCase,
    private val addReturnedUseCase: AddReturnedUseCase,
    private val updateReturnedUseCase: UpdateReturnedUseCase, // أضف الـ UseCase الجديد هنا
    private val getAllCustomersUseCase: GetAllCustomersUseCase,
    private val getItemHistoryUseCase: GetItemHistoryUseCase,
    private val outboundRepo: OutboundRepo // لجلب كل الأصناف
) : ViewModel() {

    // 1. القوائم الأساسية
    private val _returnedList = MutableStateFlow<List<ReturnedWithNameModel>>(emptyList())
    val returnedList = _returnedList.asStateFlow()

    private val _returnedDetails = MutableStateFlow<List<ReturnedDetailsModel>>(emptyList())
    val returnedDetails = _returnedDetails.asStateFlow()

    // تحويل من List<Stock> إلى List<ItemsEntity>
    val allStockItems: StateFlow<List<ItemsEntity>> = outboundRepo.getAllItems()
        .map { stockList ->
            stockList.map { stock ->
                ItemsEntity(
                    id = stock.ItemId, // تأكد من مطابقة الـ IDs
                    itemName = stock.itemName,
                    itemNum = 0,
                    // أضف باقي الحقول المطلوبة لـ ItemsEntity هنا
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    // 3. حالة العملية (استبدل Boolean بـ Resource لنتائج أدق)
    private val _saveStatus = MutableSharedFlow<Resource<Unit>>()
    val saveStatus = _saveStatus.asSharedFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // 4. بيانات مساعدة للـ UI
    private val _lastPrice = MutableStateFlow(0.0)
    val lastPrice = _lastPrice.asStateFlow()

    private val _itemHistory = MutableStateFlow<List<ItemHistoryProjectionModel>>(emptyList())
    val itemHistory = _itemHistory.asStateFlow()

    init {
        loadAllReturned()
    }

    // تعديل: إزالة الـ Context واستخدام الـ Repository المحقون
    fun allCustomers(): StateFlow<List<CustomerModel>> {
        return getAllCustomersUseCase.execute() // افترضنا أن الـ UseCase بيجيب الـ UserId داخلياً
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // 5. منطق الحفظ والتعديل الموحد
    fun saveOrUpdateReturned(
        isEditMode: Boolean,
        returned: ReturnedModel,
        details: List<ReturnedDetailsModel>,
        newDebtAmount: Double
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isEditMode) {
                    // استدعاء UseCase التعديل اللي بيصفر الحسابات القديمة
                    updateReturnedUseCase(returned, details, newDebtAmount)
                } else {
                    addReturnedUseCase.saveLocally(returned, details, newDebtAmount)
                }
                _saveStatus.emit(Resource.Success(Unit))
            } catch (e: Exception) {
                _saveStatus.emit(Resource.Error(e.message ?: "فشلت العملية"))
            }
        }
    }

    fun loadReturnedDetails(id: String) {
        viewModelScope.launch {
            gettReturnedDetailsUseCase(id).collect {
                _returnedDetails.value = it
            }
        }
    }

    fun deleteReturned(returned:  ReturnedWithNameModel) {
        viewModelScope.launch {
            deleteReturnedUsecase.invoke(returned)
        }
    }

    fun loadItemHistory(customerId: Int, itemId: Int) {
        viewModelScope.launch {
            getItemHistoryUseCase(customerId, itemId).collect { history ->
                _itemHistory.value = history
            }
        }
    }

    fun loadLastPrice(customerId: Int, itemId: Int) {
        viewModelScope.launch {
            val price = getLastPriceUseCase(customerId, itemId)
            _lastPrice.value = price ?: 0.0
        }
    }

    private fun loadAllReturned() {
        viewModelScope.launch {
            getAllReturnedUseCase.execute().collect { list ->
                _returnedList.value = list
            }
        }
    }

    fun syncData() {
//        viewModelScope.launch {
//            _isSyncing.value = true
//            val result = addReturnedUseCase.syncWithServer()
//            result.onFailure { _uiEvent.emit("فشلت المزامنة: ${it.message}") }
//            _isSyncing.value = false
//        }
    }

    fun clearHistory() { _itemHistory.value = emptyList() }
}
