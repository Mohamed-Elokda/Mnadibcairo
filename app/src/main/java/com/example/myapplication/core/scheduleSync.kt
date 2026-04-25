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
    // 1. القيود: لازم يكون فيه إنترنت
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    // 2. إنشاء الطلب (OneTime)
    val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .build()

    // 3. الإرسال كـ "Unique Work" (الحل السحري)
    WorkManager.getInstance(context).enqueueUniqueWork(
        "MnadibCairoSync", // اسم فريد للمهمة
        ExistingWorkPolicy.REPLACE, // لو فيه مهمة قديمة مستنية، استبدلها بالجديدة
        syncRequest
    )
}