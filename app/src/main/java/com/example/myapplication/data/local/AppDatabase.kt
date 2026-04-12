package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.example.myapplication.data.local.entity.Customer
import com.example.myapplication.data.local.entity.InboundEntity
import com.example.myapplication.data.local.entity.InboundDetailesEntity
import com.example.myapplication.data.local.entity.ItemsEntity

import com.example.myapplication.data.local.entity.OutboundDetailesEntity
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.local.entity.PaymentEntity
import com.example.myapplication.data.local.entity.ReturnedDetailsEntity
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.data.local.entity.StockEntity
import com.example.myapplication.data.local.entity.Supplied
import com.example.myapplication.data.local.entity.TransferDetailsEntity
import com.example.myapplication.data.local.entity.TransferEntity

@Database(entities = [ StockEntity::class, Customer::class, InboundEntity::class, InboundDetailesEntity::class, OutboundEntity::class, OutboundDetailesEntity::class
                     , ItemsEntity::class, Supplied::class, PaymentEntity::class, ReturnedEntity::class, ReturnedDetailsEntity::class, TransferDetailsEntity::class, TransferEntity::class], version = 20,exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun customerDao(): CustomerDao
    abstract fun inboundDao(): InboundDao
    abstract fun inboundDetailesDao(): InboundDetailesDao
    abstract fun outboundDao(): OutboundDao
    abstract fun outboundDetailesDao(): OutboundDetailesDao
    abstract fun itemsDao(): ItemsDao
    abstract fun suppliedDao(): SuppliedDao
    abstract fun PaymentDao(): PaymentDao
    abstract fun returnedDao(): ReturnedDao
    abstract fun transferDao(): TransferDao
    abstract fun returnedDetailsDao(): ReturnedDetailsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sales_database"
                ).fallbackToDestructiveMigration().build() // السطر ده هيمنع الـ Crash وهيمسح الداتا القديمة.build()
                INSTANCE = instance
                instance
            }
        }
    }
}