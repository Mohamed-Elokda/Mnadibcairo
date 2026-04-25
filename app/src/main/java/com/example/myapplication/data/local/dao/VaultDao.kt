package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.VaultEntity

@Dao
interface VaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: VaultEntity)
    @Query("SELECT * FROM user_vault")
    suspend fun getAllOperationsDirect(): List<VaultEntity>
    // حساب إجمالي الرصيد الحالي للمندوب (مجموع المبالغ)
    @Query("SELECT SUM(amount) FROM user_vault ")
    suspend fun getCurrentBalance(): Double?

    // حساب الرصيد النقدي فقط (كاش)
    @Query("SELECT SUM(amount) FROM user_vault where payment_method = 'cash'")
    suspend fun getCashBalance(): Double?

    // حساب رصيد المحفظة الإلكترونية
    @Query("SELECT SUM(amount) FROM user_vault where  payment_method = 'e-wallet'")
    suspend fun getEWalletBalance(): Double?

    // جلب جميع حركات الخزنة لعرضها في سجل (History)
    @Query("SELECT * FROM user_vault  ORDER BY created_at DESC")
    suspend fun getAllOperations(): List<VaultEntity>
}