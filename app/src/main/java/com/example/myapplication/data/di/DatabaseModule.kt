package com.example.myapplication.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.dao.CustomerDao
import com.example.myapplication.data.local.dao.InboundDao
import com.example.myapplication.data.local.dao.InboundDetailesDao
import com.example.myapplication.data.local.dao.ItemsDao
import com.example.myapplication.data.local.dao.OutboundDao
import com.example.myapplication.data.local.dao.OutboundDetailesDao
import com.example.myapplication.data.local.dao.PaymentDao
import com.example.myapplication.data.local.dao.ReturnedDao
import com.example.myapplication.data.local.dao.ReturnedDetailsDao
import com.example.myapplication.data.local.dao.StockDao
import com.example.myapplication.data.local.dao.SuppliedDao
import com.example.myapplication.data.local.dao.TransferDao
import com.example.myapplication.data.local.dao.VaultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {

        return Room.databaseBuilder(
            context, AppDatabase::class.java, "sales_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    @Singleton
    fun provideStockDao(db: AppDatabase): StockDao {
        return db.stockDao()
    }

    @Provides
    @Singleton
    fun provideCustomerDao(db: AppDatabase): CustomerDao {
        return db.customerDao()
    }

    @Provides
    @Singleton
    fun provideInboundDao(db: AppDatabase): InboundDao {
        return db.inboundDao()
    }

    @Provides
    @Singleton
    fun provideInboundDetailsDao(db: AppDatabase): InboundDetailesDao {
        return db.inboundDetailesDao()
    }

    @Provides
    @Singleton
    fun provideOutboundDao(db: AppDatabase): OutboundDao {
        return db.outboundDao()
    }

    @Provides
    @Singleton
    fun provideOutboundDetailsDao(db: AppDatabase): OutboundDetailesDao {
        return db.outboundDetailesDao()
    }

    @Provides
    @Singleton
    fun provideItemsDao(db: AppDatabase): ItemsDao {
        return db.itemsDao()
    }

    @Provides
    @Singleton
    fun provideSuppliedDao(db: AppDatabase): SuppliedDao {
        return db.suppliedDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(db: AppDatabase): PaymentDao {
        return db.PaymentDao()
    }

    @Provides
    @Singleton
    fun provideReturnedDetailsDao(db: AppDatabase): ReturnedDetailsDao {
        return db.returnedDetailsDao()
    }

    @Provides
    @Singleton
    fun provideTransferDao(db: AppDatabase): TransferDao {
        return db.transferDao()
    }


    @Provides
    @Singleton
    fun provideReturnedDao(db: AppDatabase): ReturnedDao {
        return db.returnedDao()
    }
   @Provides
    @Singleton
    fun provideVaultDao(db: AppDatabase): VaultDao {
        return db.vaultDoa()
    }


    @OptIn(SupabaseInternal::class)
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://wktoywqvndtxozxbrmha.supabase.co",
            supabaseKey = "sb_publishable_EA11pxS0e2KdoyxhXbWU6A_3QUN5Uww"
        ) {
            install(Postgrest)
            install(Storage) // ستحتاجه لرفع الصور لاحقاً
// الإعداد الصحيح للوقت في النسخ الحديثة
            httpConfig {
                // وقت تنفيذ الطلب بالكامل
                install(io.ktor.client.plugins.HttpTimeout) {
                    requestTimeoutMillis = 30000L  // 30 ثانية
                    connectTimeoutMillis = 30000L  // 30 ثانية
                    socketTimeoutMillis = 30000L   // 30 ثانية
                }
            }
        }
    }
}