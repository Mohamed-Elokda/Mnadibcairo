package com.example.myapplication.core

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import androidx.work.Constraints

fun scheduleSync(context: Context) {
    // 1. تحديد القيود (الإنترنت فقط)
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // 2. إنشاء طلب المهمة (مرة واحدة OneTime)
    val periodicSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>(
    )
        .setConstraints(constraints)
.build()

    // 3. إرسال المهمة للـ WorkManager كـ "UniqueWork" لضمان عدم تكرار المهمة
    WorkManager.getInstance(context).enqueue(
        periodicSyncRequest
    )
}