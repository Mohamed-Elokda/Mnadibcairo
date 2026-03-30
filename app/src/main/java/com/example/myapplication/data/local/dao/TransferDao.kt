package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.TransferDetailsEntity
import com.example.myapplication.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransferDetail(detail: TransferDetailsEntity)

    @Query("SELECT * FROM Transfer WHERE userId = :userId")
    fun getAllTransfers(userId: String): Flow<List<TransferEntity>>

        @Query("SELECT * FROM Transfer WHERE isSynced = 0")
        suspend fun getUnsyncedTransfers(): List<TransferEntity>

        @Query("SELECT * FROM TransferDetails WHERE transferId = :tId")
        suspend fun getDetailsByTransferIdSync(tId: Int): List<TransferDetailsEntity>

        @Query("UPDATE Transfer SET isSynced = :status WHERE id = :id")
        suspend fun updateSyncStatus(id: Int, status: Boolean)



        // ... الدوال السابقة (insertTransfer, etc)
    }
