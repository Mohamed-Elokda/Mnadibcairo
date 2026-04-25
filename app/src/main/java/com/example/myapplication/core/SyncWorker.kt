package com.example.myapplication.core

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.repository.ReturnedRepo
import com.example.myapplication.domin.repository.VaultRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.net.UnknownHostException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context, @Assisted workerParams: WorkerParameters,
    private val outboundRepo: OutboundRepo,
    private val vaultRepository: VaultRepository,
    private val returnedRepo: ReturnedRepo,
    private val inboundRepositoryImpl: IInboundRepository)
    : CoroutineWorker(appContext, workerParams) {


    override suspend fun doWork(): Result {
        // Log للتأكد من بدء العمل
        Log.d("SyncWorker", "المزامنة بدأت الآن...")

        return try {
//            outboundRepo.syncWithConflictResolution()
            outboundRepo.syncEverything()
//            vaultRepository.syncWithTimestampStrategy()
//            returnedRepo.syncReturnsWithServer()
//
//          inboundRepositoryImpl.syncWithConflictResolution()
           // inboundRepositoryImpl.syncUnsynced()




                Result.success()

        } catch (e: Exception) {
            if (e is UnknownHostException) {
                Log.e("SyncWorker", "لا يوجد اتصال بالإنترنت حالياً، سيتم إعادة المحاولة لاحقاً.")
                Result.retry() // يطلب من WorkManager المحاولة في الدورة القادمة
            } else {
                Log.e("SyncWorker", "خطأ غير متوقع: ${e.message}")
                Result.failure()
            }
        }
    }
}