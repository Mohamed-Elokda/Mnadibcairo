package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.local.dao.OutboundDao
import com.example.myapplication.data.local.dao.PaymentDao
import com.example.myapplication.data.local.dao.ReturnedDao
import com.example.myapplication.data.local.entity.Customer
import com.example.myapplication.data.remote.dto.CustomerDto
import com.example.myapplication.data.toDomain
import com.example.myapplication.domin.model.CustomerModel


import com.example.myapplication.domin.repository.CustomerRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class CustomerRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context, // أضف هذا
    private val customerDao: CustomerDao,
    private val outboundDao: OutboundDao,
    private val returnedDao: ReturnedDao,
    private val paymentDao: PaymentDao,
    private val supabase: SupabaseClient) : CustomerRepo {

    override suspend fun insertCustomer(customerModel: CustomerModel):Int {
        val entity = Customer(
            id = customerModel.id,
            userId = customerModel.userId, // تأكد أنها userId وليست id العميل
            customerName = customerModel.customerName,
            customerNum = customerModel.customerNum,
            customerDebt = customerModel.customerDebt,

            isSync = false,
            firstCustomerDebt = customerModel.firstCustomerDebt,
            updatedAt = customerModel.updatedAt
        )
      return  customerDao.insertCustomer(entity).toInt()
    }

    override suspend fun updateCustomer(customerModel: CustomerModel) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCustomer(customerModel: CustomerModel) {
        TODO("Not yet implemented")
    }

    override suspend fun getCustomerById(id: Int): CustomerModel {
        TODO("Not yet implemented")
    }
    // داخل كلاس CustomerRepoImpl
    override suspend fun syncCustomersFromServer(userId: String) {
        try {
            // 1. جلب العملاء من Supabase
            val remoteCustomers = supabase.from("customers")
                .select {
                    filter {
                        eq("user_id", userId)
                        // ملاحظة: يفضل جلب الكل أو المحدثين فقط وليس فقط is_sync = false
                        // لأن السيرفر قد لا يعرف حالة المزامنة لدى العميل دائماً
                    }
                }
                .decodeList<CustomerDto>()

            if (remoteCustomers.isEmpty()) {
                Log.d("SYNC_SUCCESS", "تمت إضاف")

                return
            }



            // 2. جلب جميع أسماء العملاء الموجودين حالياً في Room لتجنب التكرار
            // (تأكد من وجود دالة getAllCustomerNames في الـ DAO تعيد List<String>)
            val localNames = customerDao.getAllCustomerNames()

            // 3. تحويل وفلترة: سنضيف فقط العملاء الذين ليس لديهم اسم مطابق محلياً
            val newEntities = remoteCustomers
                .filter { dto -> !localNames.contains(dto.customer_name) } // فلترة الأسماء غير الموجودة
                .map { dto ->
                    Customer(
                        id = dto.id,
                        userId = dto.user_id,
                        customerName = dto.customer_name,
                        customerNum = dto.customer_num,
                        customerDebt = dto.customer_debt,
                        isSync = true,
                        firstCustomerDebt = dto.firstCustomerDebt,
                        updatedAt = dto.updated_at
                    )
                }

            // 4. التخزين في Room
            if (newEntities.isNotEmpty()) {
                customerDao.insertAll(newEntities)
                Log.d("SYNC_SUCCESS", "تمت إضافة ${newEntities.size} عملاء جدد")
            } else {
                Log.d("SYNC_INFO", "لا يوجد عملاء جدد لإضافتهم (الكل موجود بالفعل)")
            }

        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "فشل جلب العملاء: ${e.message}")
            FileLogger.logError( "خطأ", e)

        }
    }
    override fun getAllCustomers(userId: String): Flow<List<CustomerModel>> {
        return combine(
            customerDao.getAllCustomers(userId),
            outboundDao.getAllOutboundsTotal(),
            outboundDao.getAllOutboundsResiveTotal(),
            paymentDao.getAllPaymentsTotal(),   // إجمالي تحصيلات كل عميل
            returnedDao.getAllReturnsTotal()    // إجمالي مرتجعات كل عميل
        ) { customers, outbounds, recieve,payments, returns ->

            customers.map { entity ->
                val cid = entity.id

                // 1. جلب الإجماليات من واقع الجداول (لو مفيش عمليات بنفترض 0)
                val totalInvoices = outbounds.find { it.customerId == cid }?.totalAmount ?: 0.0
                val totalPayments = payments.find { it.customerId == cid }?.totalAmount ?: 0.0
                val totalReturns = returns.find { it.customerId == cid }?.totalAmount ?: 0.0
                val totalRecieve = recieve.find { it.customerId == cid }?.totalAmount ?: 0.0

                // 2. المعادلة الحسابية:
                // (الرصيد الافتتاحي + إجمالي الفواتير) - (المرتجع + التحصيل)
                val calculatedDebt = (entity.firstCustomerDebt + totalInvoices) - (totalReturns + totalPayments+totalRecieve)
                Log.d("TAG", "getAllCustomers: "+totalPayments)
                // 3. تحويل الـ Entity لـ Domain ومده بالرصيد المحسوب لحظياً
                entity.toDomain().copy(customerDebt = calculatedDebt)
            }
        }
    }

    // يمكنك إضافة هذه الدالة لتحديث المديونية من الـ UseCase
    suspend fun updateDebt(customerId: Int, amount: Double) {
        customerDao.updateCustomerDebt(customerId, amount)
    }

    // باقي الدوال (update, delete, getById) يمكن تنفيذها لاحقاً
}