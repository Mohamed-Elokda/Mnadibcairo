package com.example.myapplication.domin.repository

import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetails

interface ITransferRepository {
    suspend fun saveTransfer(transfer: Transfer, details: List<TransferDetails>): Result<Unit>
    suspend fun syncTransfers(): Result<Unit>
}