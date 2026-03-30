package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.Supplied
import kotlinx.coroutines.flow.Flow

@Dao
interface SuppliedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplied: Supplied): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliedList: List<Supplied>)
    @Query("SELECT * FROM Supplied")
    fun getAllSupplied(): Flow<List<Supplied>>


}