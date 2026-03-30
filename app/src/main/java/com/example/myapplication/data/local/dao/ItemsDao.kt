package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.ItemsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemsEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemsList(items: List<ItemsEntity>)
    @Query("SELECT * FROM Items")
     fun getAllItems(): Flow<List<ItemsEntity>>
    @Query("SELECT * FROM Items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Int): ItemsEntity?
    @Query("SELECT COUNT(*) FROM Items") // تأكد من اسم الجدول لديك
    suspend fun getItemsCount(): Int
}