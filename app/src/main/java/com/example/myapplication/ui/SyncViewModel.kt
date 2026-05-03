package com.example.myapplication.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.TransferRepositoryImpl
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.ITransferRepository
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.repository.PaymentRepository
import com.example.myapplication.domin.repository.ReturnedRepo
import com.example.myapplication.domin.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val customerRepo: CustomerRepo,
    private val outboundRepo: OutboundRepo,
    private val returnedRepo: ReturnedRepo,
    private val stockRepo: StockRepository,
    private val paymentRepository: PaymentRepository,
    private val transferRepository: ITransferRepository,
    @ApplicationContext private val context: Context,
    private val inbound: InboundRepositoryImpl,

    ) : ViewModel() {
    private val userId: String = Prefs.getUserId(context) ?: ""
    private val _syncStatus = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncStatus = _syncStatus.asStateFlow()

    // الحالة العامة للمزامنة
    sealed class SyncState {
        object Idle : SyncState()
        object Loading : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
        data class Progress(val step: String) : SyncState()
    }

    fun startFullSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncStatus.value = SyncState.Loading

            try {
                // 1. جلب العملاء أولاً (لأن الفواتير تعتمد عليهم)
                _syncStatus.value = SyncState.Progress("جاري تحميل بيانات العملاء...")
                customerRepo.syncCustomersFromServer(userId)

                // 2. جلب الأصناف والمخزن
                _syncStatus.value = SyncState.Progress("جاري تحديث قائمة الأصناف...")
                outboundRepo.syncItemsFromServer()

                // 3. جلب فواتير الصادر وتفاصيلها
                _syncStatus.value = SyncState.Progress("جاري تحميل فواتير المبيعات...")
                outboundRepo.syncOutboundsFromServer(userId)

                // 4. جلب فواتير المرتجعات
                _syncStatus.value = SyncState.Progress("جاري تحميل فواتير المرتجعات...")
                returnedRepo.syncReturnsFromServer(userId)

                // 4. جلب فواتير المرتجعات
                _syncStatus.value = SyncState.Progress("جاري تحميل فواتير الاخراج...")
                transferRepository.syncTransferFromServer(userId)


                _syncStatus.value = SyncState.Progress("جاري تحميل فواتير  الوارد...")
                inbound.syncInboundFromServer(userId)

                _syncStatus.value = SyncState.Progress("جاري تحميل توريدات العملاء ...")
                paymentRepository.syncPaymentsFromServer(userId)


                _syncStatus.value = SyncState.Progress("جاري تحميل بيانات الموردين...")
                inbound.syncSuppliedFromServer() // استدعاء الدالة الجديدة
                 _syncStatus.value = SyncState.Progress("جاري تحميل بيانات المخازن...")
                  stockRepo.syncStockFromServer(userId) // استدعاء الدالة الجديدة

                _syncStatus.value = SyncState.Success("تمت مزامنة كافة البيانات بنجاح!")
            } catch (e: Exception) {
                _syncStatus.value = SyncState.Error("فشل المزامنة: ${e.message}")
            }
        }
    }
}