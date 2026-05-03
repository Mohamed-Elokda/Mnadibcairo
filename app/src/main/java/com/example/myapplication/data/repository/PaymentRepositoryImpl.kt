package com.example.myapplication.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.local.dao.PaymentDao
import com.example.myapplication.data.local.entity.PaymentEntity
import com.example.myapplication.data.remote.dto.PaymentDto
import com.example.myapplication.data.remote.dto.TransferDto
import com.example.myapplication.data.toDomainModel
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.PaymentItem
import com.example.myapplication.domin.repository.PaymentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.map

class PaymentRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val database: AppDatabase,
    private val paymentDao: PaymentDao,
    private val customerDao: CustomerDao
) : PaymentRepository {

    override suspend fun processPaymentAndUpdateBalance(
        customerId: Int,
        amount: Double,
        type: String,
        notes: String
    ): Boolean {
        return try {
            // استخدام Transaction لضمان سلامة البيانات
            database.withTransaction {
                // 1. تسجيل عملية التوريد في جدول التوريدات
                val payment = PaymentEntity(
                    customerId = customerId,
                    amount = amount,
                    paymentType = type,
                    notes=notes,
                    date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
                )
                paymentDao.insertPayment(payment)

                // 2. تحديث رصيد العميل (خصم المبلغ المورد من مديونيته)
                // نستخدم الدالة التي طرحناها سابقاً في CustomerDao
                customerDao.decreaseCustomerBalance(customerId, amount)
            }
            true // نجحت العملية بالكامل
        } catch (e: Exception) {
            Log.e("PaymentRepo", "Error processing payment: ${e.message}")
            false // فشلت العملية
        }
    }
    override suspend fun syncPaymentsFromServer(userId: String) {
        try {
            // 1. جلب كافة المدفوعات المرتبطة بالمندوب (عن طريق userId أو أي وسيلة ربط أخرى)
            val remotePayments = supabaseClient.from("payments")
                .select { filter { eq("user_id", userId) } }
                .decodeList<PaymentDto>()

            remotePayments.forEach { remoteDto ->
                // 2. جلب النسخة المحلية للتحقق
                val localPayment = paymentDao.getPaymentByIdSync(remoteDto.id)

                // 3. المزامنة إذا كان السجل جديداً أو بيانات السيرفر أحدث
                if (localPayment == null || (remoteDto.updated_at > localPayment.updatedAt)) {

                    // تحويل الـ DTO إلى Entity وحفظه في Room
                    // copy(isSynced = true) لضمان أن الموبايل لن يحاول رفعها مرة أخرى
                    val paymentToSave = remoteDto.toEntity().copy(
                        updatedAt = remoteDto.updated_at,
                    )

                    paymentDao.insertPayment(paymentToSave)

                    Log.d("Sync", "✅ تم تحديث سند قبض رقم ${remoteDto.id} من السيرفر")
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "❌ خطأ في جلب المدفوعات: ${e.message}")
        }
    }
    // ✅ يجب إضافة كلمة suspend هنا أيضاً
    override suspend fun getAllPayments(): List<PaymentItem> {
        val entities = paymentDao.getAllPayments()

        // ننشئ قائمة جديدة لتخزين النتائج
        val paymentItems = mutableListOf<PaymentItem>()

        // نستخدم for loop العادي لأنه يدعم استدعاء suspend functions بداخله
        for (entity in entities) {
            val name = customerDao.getCustomerNameById(entity.customerId) ?: "عميل غير معروف"
            paymentItems.add(entity.toDomainModel(name))
        }

        return paymentItems
    }}