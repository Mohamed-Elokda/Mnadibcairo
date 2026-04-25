package com.example.myapplication.ui.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.useCase.GetStockFromServer
import com.example.myapplication.domin.useCase.GetStockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class StoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getStockUseCase: GetStockUseCase,
    private val getStockFromServer: GetStockFromServer
) : ViewModel() {
    private val userId: String = Prefs.getUserId(context) ?: ""
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val storeState = getStockUseCase()
        .map { list ->
            if (list.isEmpty()) {
                // إذا كانت القائمة فارغة محلياً، نحاول الجلب من السيرفر
                fetchInitialDataIfNeeded()
                StoreState.Error("المخزن فارغ، جاري محاولة التحديث...")
            } else {
                StoreState.Success(list)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreState.Loading
        )

    // دالة للجلب الأولي فقط إذا كان المحلي فارغاً
    private fun fetchInitialDataIfNeeded() {
        // نستخدم launch لكي لا نعطل الـ Flow
        viewModelScope.launch {
            // نتحقق مرة أخرى من الـ Repository (اختياري لزيادة التأكيد)
            refreshStock()
        }
    }

    // هذه الدالة تظل موجودة للمستخدم إذا أراد عمل "Swipe to Refresh" يدوياً
    fun refreshStock() {
        if (_isRefreshing.value) return // منع التكرار إذا كان هناك طلب يعمل بالفعل

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                if (userId.isNotEmpty()) {
                    getStockFromServer(userId)
                }
            } catch (e: Exception) {
                // معالجة الخطأ
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

    sealed class StoreState {
        object Idle : StoreState()
        object Loading : StoreState()
        data class Success(val items:List<Stock>) : StoreState()
        data class Error(val message: String) : StoreState()
    }
