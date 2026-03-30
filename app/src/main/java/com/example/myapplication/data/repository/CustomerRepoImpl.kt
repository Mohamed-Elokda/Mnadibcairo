package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.remote.dto.CustomerDto
import com.example.myapplication.data.remote.manager.supabase
import com.example.myapplication.data.toDomain
import com.example.myapplication.domin.model.Customer


import com.example.myapplication.domin.repository.CustomerRepo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepoImpl(private val customerDao: CustomerDao) : CustomerRepo {

    override suspend fun insertCustomer(customer: com.example.myapplication.domin.model.Customer):Int {
        val entity = com.example.myapplication.data.local.entity.Customer(
            id = customer.id,
            userId = customer.userId, // تأكد أنها userId وليست id العميل
            customerName = customer.customerName,
            CustomerNum = customer.CustomerNum,
            customerDebt = customer.customerDebt,

            isSync = false
        )
      return  customerDao.insertCustomer(entity).toInt()
    }

    override suspend fun updateCustomer(customer: Customer) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCustomer(customer: Customer) {
        TODO("Not yet implemented")
    }

    override suspend fun getCustomerById(id: Int): Customer {
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
                    com.example.myapplication.data.local.entity.Customer(
                        id = dto.id,
                        userId = dto.user_id,
                        customerName = dto.customer_name,
                        CustomerNum = dto.customer_num,
                        customerDebt = dto.customer_debt,
                        isSync = true
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
        }
    }    override  fun getAllCustomers(userId: String): Flow<List<com.example.myapplication.domin.model.Customer>> {
        // بدلاً من TODO()، نقوم باستدعاء الـ DAO وتحويل البيانات
        return customerDao.getAllCustomers(userId).map { list ->
            list.map { entity ->
                // استخدام دالة التحويل toDomain التي أنشأناها في ملف Mappers
                entity.toDomain()
            }
        }
    }

    // يمكنك إضافة هذه الدالة لتحديث المديونية من الـ UseCase
    suspend fun updateDebt(customerId: Int, amount: Double) {
        customerDao.updateCustomerDebt(customerId, amount)
    }

    // باقي الدوال (update, delete, getById) يمكن تنفيذها لاحقاً
}