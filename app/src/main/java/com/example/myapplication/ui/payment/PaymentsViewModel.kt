package com.example.myapplication.ui.payment

import android.content.Context
import androidx.lifecycle.*
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.model.CustomerModel
import com.example.myapplication.domin.model.PaymentItem
import com.example.myapplication.domin.model.PaymentMethod
import com.example.myapplication.domin.model.Resource
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.PaymentRepository
import com.example.myapplication.domin.useCase.CollectPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class PaymentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PaymentRepository,
    private val collectPaymentUseCase: CollectPaymentUseCase,
    private val customerRepo: CustomerRepo
) : ViewModel() {

    // قائمة التوريدات الأصلية
    private val _allPayments = MutableLiveData<List<PaymentItem>>()

    // القائمة التي تظهر في الواجهة (بعد الفلترة)
    private val _filteredPayments = MutableLiveData<List<PaymentItem>>()
    val allPayments: LiveData<List<PaymentItem>> get() = _filteredPayments

    // حالة حفظ التوريد الجديد
    private val _saveStatus = MutableLiveData<Resource<String>>()
    val saveStatus: LiveData<Resource<String>> get() = _saveStatus
    fun allCustomers(context: Context): LiveData<List<CustomerModel>> {
        return customerRepo.getAllCustomers(Prefs.getUserId(context)?:"").asLiveData() // افترضنا userId = 1

    }

    // 1. جلب البيانات من قاعدة البيانات
    fun loadPayments() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAllPayments()
            _allPayments.postValue(list)
            _filteredPayments.postValue(list)
        }
    }

    // 2. منطق البحث اللحظي
    fun filterPayments(query: String) {
        val currentList = _allPayments.value ?: emptyList()
        if (query.isEmpty()) {
            _filteredPayments.value = currentList
        } else {
            _filteredPayments.value = currentList.filter {
                it.customerName.contains(query, ignoreCase = true)
            }
        }
    }

    // 3. حفظ توريد جديد وتعديل حساب العميل

    fun savePayment(customerId: Int, amount: Double, type: String, method: PaymentMethod,notes: String) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // تنفيذ العملية في الـ Repository (حفظ التوريد + خصم من مديونية العميل)
                val success = repository.processPaymentAndUpdateBalance(customerId, amount, type,notes)
                collectPaymentUseCase(
                    amount = amount,
                    method = method,
                    notes = notes,
                )
                if (success) {
                    loadPayments() // تحديث القائمة بعد الحفظ
                    _saveStatus.postValue(Resource.Success("تم تسجيل التوريد وتحديث الحساب"))
                } else {
                    _saveStatus.postValue(Resource.Error("فشل في تحديث بيانات العميل"))
                }
            } catch (e: Exception) {
                _saveStatus.postValue(Resource.Error(e.message ?: "خطأ غير معروف"))
            }
        }
    }
}