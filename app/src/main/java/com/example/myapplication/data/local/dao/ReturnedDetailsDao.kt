package com.example.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.ReturnedDetailsEntity
import com.example.myapplication.data.local.entity.ReturnedDetailsWithItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnedDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addReturned(returnedDetailsEntity: ReturnedDetailsEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnedDetailsList(details: List<ReturnedDetailsEntity>)
    @Query("select * from returnedDetails where returnedId=:returnedId")
     fun getAllReturnedDetails(returnedId: String): Flow<List<ReturnedDetailsEntity>>

    @Query("""
        SELECT 
            rd.*, 
            i.itemName as itemName
        FROM returnedDetails rd
        JOIN items i ON rd.itemId = i.id
        WHERE rd.returnedId = :returnedId
    """)
    fun getDetailsByReturnedId(returnedId: String): Flow<List<ReturnedDetailsWithItem>>

    @Query("SELECT * FROM returnedDetails WHERE returnedId = :returnId")
    suspend fun getDetailsByReturnId(returnId: String): List<ReturnedDetailsEntity>


    @Query("SELECT * FROM returnedDetails WHERE returnedId = :returnedId")
    suspend fun getReturnedDetailsStatic(returnedId: String): List<ReturnedDetailsEntity>

    @Query("DELETE FROM returnedDetails WHERE returnedId = :returnedId")
    suspend fun deleteReturnedDetails(returnedId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnedDetails(details: List<ReturnedDetailsEntity>)
    }