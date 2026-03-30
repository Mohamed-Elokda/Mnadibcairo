package com.example.myapplication.core

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import java.net.UnknownHostException

// داخل ملف SyncWorker.kt
class SyncWorker(appContext: Context, workerParams: WorkerParameters)
    : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Log للتأكد من بدء العمل
        Log.d("SyncWorker", "المزامنة بدأت الآن...")

        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repo = InboundRepositoryImpl(
                db.inboundDao(),
                db.inboundDetailesDao(),
                db.stockDao(),
                db.suppliedDao(),
                db.itemsDao(),
            )
            // تأكد من تمرير كل المعاملات يدوياً هنا
            val repository = OutboundRepoImpl(
                db.outboundDao(),
                db.outboundDetailesDao(),
                db.stockDao(),
                db.itemsDao(),
                db.customerDao()
            )
            val returnedRepo = ReturnedRepoImpl(
                db,
                db.returnedDao(),
                db.returnedDetailsDao(),
                db.outboundDetailesDao(),
                db.customerDao(),
                stockDao = db.stockDao()
            )
            repository.syncEverything()
            returnedRepo.syncReturnsWithServer()
            val result = repo.syncUnsynced()



            if (result.isSuccess) {
                Log.e("SyncWorker", "لا يوجد اتصال بالإنترنت حالياً، سيتم إعادة المحاولة لاحقاً.")

                Result.success()
            } else {
                // إذا كان الخطأ بسبب الشبكة، نطلب إعادة المحاولة
                Result.retry()
            }
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