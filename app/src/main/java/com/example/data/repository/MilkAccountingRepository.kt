package com.example.data.repository

import com.example.data.db.MilkRecordDao
import com.example.data.model.MilkRecordEntity
import kotlinx.coroutines.flow.Flow

class MilkAccountingRepository(private val dao: MilkRecordDao) {
    fun getRecordsForMonth(year: Int, month: Int): Flow<List<MilkRecordEntity>> =
        dao.getRecordsForMonth(year, month)

    suspend fun getRecordsForMonthOnce(year: Int, month: Int): List<MilkRecordEntity> =
        dao.getRecordsForMonthOnce(year, month)

    suspend fun getRecordByDate(date: String): MilkRecordEntity? =
        dao.getRecordByDate(date)

    suspend fun saveRecord(record: MilkRecordEntity) =
        dao.upsertRecord(record)

    suspend fun deleteRecord(record: MilkRecordEntity) =
        dao.deleteRecord(record)

    suspend fun deleteRecordByDate(date: String) =
        dao.deleteRecordByDate(date)
}
