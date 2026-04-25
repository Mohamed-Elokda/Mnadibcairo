package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.myapplication.data.local.Prefs
import com.example.myapplication.data.local.dao.VaultDao
import com.example.myapplication.data.local.entity.VaultEntity
import com.example.myapplication.data.toEntity
import com.example.myapplication.domin.model.Vault
import com.example.myapplication.domin.repository.VaultRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class VaultRepositoryImpl @Inject constructor(

    @ApplicationContext  private val context: Context,
    private val vaultDao: VaultDao,
    private val supabaseClient: SupabaseClient // افترض وجوده
) : VaultRepository {

    override suspend fun collectPayment(operation: VaultEntity): Result<Unit> {
        return try {
            // 1. حفظ محلي أولاً (Offline-first)
            vaultDao.insertOperation(operation)

            // 2. الرفع للسيرفر (اختياري هنا لأننا عملنا Trigger في السيرفر)
            // لكن يفضل عمل مزامنة يدوية إذا كانت العملية مستقلة عن الفاتورة
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // داخل VaultRepositoryImpl
    override suspend fun syncWithTimestampStrategy(): Result<Unit> {
        return try {
            val userId = Prefs.getUserId(context)?:""

            // 1. جلب البيانات من السيرفر أولاً لنرى ما هو الأحدث هناك
            val remoteData = supabaseClient.postgrest["user_vault"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<VaultEntity>()

            // 2. جلب البيانات المحلية
            val localData = vaultDao.getAllOperationsDirect()

            // 3. مقارنة ودمج (Merge)
            localData.forEach { localItem ->
                val remoteItem = remoteData.find { it.id == localItem.id }

                if (remoteItem == null || localItem.updated_at > remoteItem.updated_at) {
                    // إذا لم يكن موجوداً على السيرفر أو كان المحلي أحدث -> ارفعه
                    supabaseClient.postgrest["user_vault"].upsert(localItem) {
                        onConflict = "id" // تحديث بناءً على الـ ID
                    }
                } else if (remoteItem.updated_at > localItem.updated_at) {
                    // إذا كان السيرفر أحدث -> حدث الهاتف
                    vaultDao.insertOperation(remoteItem)
                }
            }

            // 4. جلب أي سجلات جديدة تماماً من السيرفر ليست في الهاتف
            val localIds = localData.map { it.id }.toSet()
            val newFromRemote = remoteData.filter { it.id !in localIds }
            newFromRemote.forEach { vaultDao.insertOperation(it) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addVaultOperation(vaultOperation: Vault) {
        vaultDao.insertOperation(vaultOperation.toEntity())
    }

    override suspend fun getBalance(userId: String): Double {
        return vaultDao.getCurrentBalance() ?: 0.0
    }

    override suspend fun getHistory(userId: String): List<VaultEntity> {
        return vaultDao.getAllOperations()
    }

    override suspend fun depositToCompany(operation: VaultEntity): Result<Unit> {
        // منطق توريد المبلغ (سالب)
        vaultDao.insertOperation(operation.copy(amount = -operation.amount))
        return Result.success(Unit)
    }
}