package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milk_records")
data class MilkRecordEntity(
    @PrimaryKey
    val date: String, // format "yyyy-MM-dd" e.g. "2026-10-01"
    val year: Int,
    val month: Int, // 1..12
    val dayOfMonth: Int, // 1..31
    val morningQty: Double? = null,
    val morningAmount: Double? = null,
    val eveningQty: Double? = null,
    val eveningAmount: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val dailyTotalQty: Double
        get() = (morningQty ?: 0.0) + (eveningQty ?: 0.0)

    val dailyTotalAmount: Double
        get() = (morningAmount ?: 0.0) + (eveningAmount ?: 0.0)

    val hasEntry: Boolean
        get() = morningQty != null || morningAmount != null || eveningQty != null || eveningAmount != null
}
