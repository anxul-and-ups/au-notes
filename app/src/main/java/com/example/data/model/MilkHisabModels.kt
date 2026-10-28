package com.example.data.model

import java.util.Calendar
import java.util.Locale

data class PeriodSummary(
    val periodIndex: Int, // 1, 2, 3
    val title: String,
    val startDay: Int,
    val endDay: Int,
    val morningQty: Double,
    val morningAmount: Double,
    val eveningQty: Double,
    val eveningAmount: Double
) {
    val totalQty: Double get() = morningQty + eveningQty
    val totalAmount: Double get() = morningAmount + eveningAmount
}

data class MonthlyHisabSummary(
    val year: Int,
    val month: Int,
    val monthName: String,
    val totalDays: Int,
    val period1: PeriodSummary,
    val period2: PeriodSummary,
    val period3: PeriodSummary,
    val monthlyMorningQty: Double,
    val monthlyMorningAmount: Double,
    val monthlyEveningQty: Double,
    val monthlyEveningAmount: Double
) {
    val grandTotalQty: Double get() = monthlyMorningQty + monthlyEveningQty
    val grandTotalAmount: Double get() = monthlyMorningAmount + monthlyEveningAmount
}

object MilkHisabCalculator {
    fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getMonthName(month: Int): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return if (month in 1..12) monthNames[month - 1] else "Month $month"
    }

    fun formatDateKey(year: Int, month: Int, day: Int): String {
        return "%04d-%02d-%02d".format(Locale.ENGLISH, year, month, day)
    }

    fun computeSummary(year: Int, month: Int, recordsMap: Map<Int, MilkRecordEntity>): MonthlyHisabSummary {
        val lastDay = getDaysInMonth(year, month)
        val monthName = getMonthName(month)

        fun calcPeriod(index: Int, start: Int, end: Int): PeriodSummary {
            var mQty = 0.0
            var mAmt = 0.0
            var eQty = 0.0
            var eAmt = 0.0
            for (day in start..end) {
                val r = recordsMap[day]
                if (r != null) {
                    mQty += r.morningQty ?: 0.0
                    mAmt += r.morningAmount ?: 0.0
                    eQty += r.eveningQty ?: 0.0
                    eAmt += r.eveningAmount ?: 0.0
                }
            }
            return PeriodSummary(
                periodIndex = index,
                title = "Period $index ($start–$end $monthName)",
                startDay = start,
                endDay = end,
                morningQty = mQty,
                morningAmount = mAmt,
                eveningQty = eQty,
                eveningAmount = eAmt
            )
        }

        val p1 = calcPeriod(1, 1, 10.coerceAtMost(lastDay))
        val p2 = calcPeriod(2, 11, 20.coerceAtMost(lastDay))
        val p3 = calcPeriod(3, 21, lastDay)

        val mMorningQty = p1.morningQty + p2.morningQty + p3.morningQty
        val mMorningAmount = p1.morningAmount + p2.morningAmount + p3.morningAmount
        val mEveningQty = p1.eveningQty + p2.eveningQty + p3.eveningQty
        val mEveningAmount = p1.eveningAmount + p2.eveningAmount + p3.eveningAmount

        return MonthlyHisabSummary(
            year = year,
            month = month,
            monthName = monthName,
            totalDays = lastDay,
            period1 = p1,
            period2 = p2,
            period3 = p3,
            monthlyMorningQty = mMorningQty,
            monthlyMorningAmount = mMorningAmount,
            monthlyEveningQty = mEveningQty,
            monthlyEveningAmount = mEveningAmount
        )
    }

    fun formatQty(value: Double?): String {
        if (value == null) return "---"
        return if (value == 0.0) "0"
        else if (value % 1.0 == 0.0) "%.0f".format(Locale.ENGLISH, value)
        else "%.2f".format(Locale.ENGLISH, value).trimEnd('0').trimEnd('.')
    }

    fun formatAmount(value: Double?): String {
        if (value == null) return "---"
        return if (value == 0.0) "0"
        else if (value % 1.0 == 0.0) "%.0f".format(Locale.ENGLISH, value)
        else "%.2f".format(Locale.ENGLISH, value).trimEnd('0').trimEnd('.')
    }
}
