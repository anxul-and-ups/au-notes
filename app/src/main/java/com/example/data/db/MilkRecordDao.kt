package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MilkRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkRecordDao {
    @Query("SELECT * FROM milk_records WHERE year = :year AND month = :month ORDER BY dayOfMonth ASC")
    fun getRecordsForMonth(year: Int, month: Int): Flow<List<MilkRecordEntity>>

    @Query("SELECT * FROM milk_records WHERE year = :year AND month = :month ORDER BY dayOfMonth ASC")
    suspend fun getRecordsForMonthOnce(year: Int, month: Int): List<MilkRecordEntity>

    @Query("SELECT * FROM milk_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): MilkRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: MilkRecordEntity)

    @Delete
    suspend fun deleteRecord(record: MilkRecordEntity)

    @Query("DELETE FROM milk_records WHERE date = :date")
    suspend fun deleteRecordByDate(date: String)
}
