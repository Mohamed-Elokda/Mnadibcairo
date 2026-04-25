package com.example.myapplication.domin.repository

import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetailWithItemName
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.model.TransferWithStoreName
import kotlinx.coroutines.flow.Flow

interface ITransferRepository {
    suspend fun saveTransfer(transfer: Transfer, details: List<TransferDetails>): Result<Unit>

    fun getAllTransfers(userId: String): Flow<List<TransferWithStoreName>>
    fun getTransferDetails(transferId: String): Flow<List<TransferDetailWithItemName>>
    suspend fun deleteFullTransfer(transferId: String)
    suspend fun syncTransferFromServer(userId: String)
}