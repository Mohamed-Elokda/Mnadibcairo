package com.example.myapplication

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.myapplication.core.SyncWorker
import com.example.myapplication.core.scheduleSync
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.repository.ReturnedRepo
import com.example.myapplication.domin.repository.VaultRepository
import dagger.hilt.android.HiltAndroidApp
import io.ktor.http.ContentType
import io.ktor.util.converters.DataConversion
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: CustomWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Hilt يقوم بحقن الـ workerFactory هنا تلقائياً بمجرد تشغيل onCreate

        // جرب تنادي على الـ schedule بعد ما نتأكد إن الـ Injection تم
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory) // Hilt سيقوم بتوفير المصنع هنا
            .setMinimumLoggingLevel(android.util.Log.INFO) // اختياري للـ Debugging
            .build()
}

class CustomWorkerFactory @Inject constructor(private val outboundRepo: OutboundRepo,
                                              private val returnedRepo: ReturnedRepo,
                                              private val vaultRepository: VaultRepository,
                                              private val inboundRepository: IInboundRepository): WorkerFactory(){
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker= SyncWorker(appContext,workerParameters,outboundRepo,
        vaultRepository ,returnedRepo,
        inboundRepository

    )

}