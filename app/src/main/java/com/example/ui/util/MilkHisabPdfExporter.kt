package com.example.ui.util

import android.content.Context
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.MilkHisabCalculator
import com.example.data.model.MilkRecordEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MilkHisabPdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN // 523f

    /**
     * Exports the Milk Dairy Hisab PDF.
     * @param periodFilter 0 = Full Month, 1 = Period 1 (1–10), 2 = Period 2 (11–20), 3 = Period 3 (21–End)
     */
    fun exportMonthPdf(
        context: Context,
        year: Int,
        month: Int,
        recordsMap: Map<Int, MilkRecordEntity>,
        periodFilter: Int = 0
    ): SavedExport? {
        val summary = MilkHisabCalculator.computeSummary(year, month, recordsMap)
        val monthName = summary.monthName

        val periodLabel = when (periodFilter) {
            1 -> "Period_1_Day_1_to_10"
            2 -> "Period_2_Day_11_to_20"
            3 -> "Period_3_Day_21_to_${summary.totalDays}"
            else -> "Full_Month"
        }
        val fileName = "Milk_Bill_${monthName}_${year}_$periodLabel.pdf"

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val tempFile = File(dir, "temp_${System.currentTimeMillis()}_$fileName")

        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var canvas = page.canvas
        var y = MARGIN

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.rgb(60, 64, 70)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.rgb(255, 45, 85) // CrimsonPrimary
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.rgb(220, 224, 230)
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Columns: Date (45), Morning Qty (75), Morning Amt (75), Evening Qty (75), Evening Amt (75), Total Qty (85), Total Amt (93)
        val colW = floatArrayOf(45f, 75f, 75f, 75f, 75f, 85f, 93f)

        val periodSubtitle = when (periodFilter) {
            1 -> "$monthName $year • Cycle 1 (Day 1 - 10)"
            2 -> "$monthName $year • Cycle 2 (Day 11 - 20)"
            3 -> "$monthName $year • Cycle 3 (Day 21 - ${summary.totalDays})"
            else -> "$monthName $year • Complete Monthly Statement"
        }

        fun drawHeader(onFirstPage: Boolean) {
            if (onFirstPage) {
                // Brand label
                canvas.drawText("AU NOTES • ACCOUNTING", MARGIN, y + 14f, boldPaint.apply {
                    color = AColor.rgb(255, 45, 85)
                    textSize = 10f
                })
                y += 20f

                // Main Title
                canvas.drawText("Milk Dairy Bill & Statement", MARGIN, y + 18f, headerTitlePaint)
                y += 24f

                // Subtitle: Month & Year + Filter
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AColor.rgb(30, 41, 59)
                    textSize = 12f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                canvas.drawText(periodSubtitle, MARGIN, y + 12f, subPaint)

                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
                val rightText = "Generated: $dateStr"
                val rightW = textPaint.measureText(rightText)
                canvas.drawText(rightText, MARGIN + CONTENT_W - rightW, y + 12f, textPaint)
                y += 22f

                canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, linePaint)
                y += 10f
            } else {
                canvas.drawText("Milk Dairy Bill — $monthName $year (Page $pageNo)", MARGIN, y + 10f, boldPaint)
                y += 16f
            }

            // Draw Two-Tier Table Header as requested by user (MORNING & EVENING grouped on top)
            val headerH = 32f
            fillPaint.color = AColor.rgb(241, 245, 249) // Slate-100 background
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + headerH), 4f, 4f, fillPaint)
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + headerH), 4f, 4f, linePaint)

            // Horizontal separator between top group row and sub-column row
            canvas.drawLine(MARGIN + colW[0], y + 16f, MARGIN + CONTENT_W, y + 16f, linePaint)

            // Vertical group separators
            val mStart = MARGIN + colW[0]
            val mEnd = mStart + colW[1] + colW[2]
            val eEnd = mEnd + colW[3] + colW[4]
            canvas.drawLine(mStart, y, mStart, y + headerH, linePaint)
            canvas.drawLine(mEnd, y, mEnd, y + headerH, linePaint)
            canvas.drawLine(eEnd, y, eEnd, y + headerH, linePaint)

            // Inner sub-column vertical lines
            canvas.drawLine(mStart + colW[1], y + 16f, mStart + colW[1], y + headerH, linePaint)
            canvas.drawLine(mEnd + colW[3], y + 16f, mEnd + colW[3], y + headerH, linePaint)
            canvas.drawLine(eEnd + colW[5], y + 16f, eEnd + colW[5], y + headerH, linePaint)

            // Tier 1: Top Group Labels
            boldPaint.color = AColor.rgb(15, 23, 42)
            boldPaint.textSize = 9f

            // Date (centered in cell spanning both rows)
            val dTitle = "DATE"
            val dTW = boldPaint.measureText(dTitle)
            canvas.drawText(dTitle, MARGIN + (colW[0] - dTW) / 2f, y + 20f, boldPaint)

            // MORNING (centered over Qty & Amount)
            boldPaint.color = AColor.rgb(255, 45, 85) // Accent color
            val mTitle = "MORNING"
            val mTW = boldPaint.measureText(mTitle)
            canvas.drawText(mTitle, mStart + (colW[1] + colW[2] - mTW) / 2f, y + 11.5f, boldPaint)

            // EVENING (centered over Qty & Amount)
            boldPaint.color = AColor.rgb(14, 165, 233) // Sky blue accent
            val eTitle = "EVENING"
            val eTW = boldPaint.measureText(eTitle)
            canvas.drawText(eTitle, mEnd + (colW[3] + colW[4] - eTW) / 2f, y + 11.5f, boldPaint)

            // DAILY TOTAL (centered over Total Qty & Total Amt)
            boldPaint.color = AColor.rgb(15, 23, 42)
            val totTitle = "DAILY TOTAL"
            val totTW = boldPaint.measureText(totTitle)
            canvas.drawText(totTitle, eEnd + (colW[5] + colW[6] - totTW) / 2f, y + 11.5f, boldPaint)

            // Tier 2: Sub-column Labels (all English)
            boldPaint.color = AColor.rgb(71, 85, 105)
            boldPaint.textSize = 8f

            val subHeaders = arrayOf(
                "", // Date already drawn
                "QUANTITY", "AMOUNT",
                "QUANTITY", "AMOUNT",
                "TOTAL QTY", "TOTAL ₹"
            )

            var cx = MARGIN
            subHeaders.forEachIndexed { i, title ->
                val w = colW[i]
                if (title.isNotEmpty()) {
                    val tw = boldPaint.measureText(title)
                    canvas.drawText(title, cx + w - tw - 6f, y + 27.5f, boldPaint)
                }
                cx += w
            }

            y += headerH + 2f
        }

        drawHeader(true)

        val rowH = 17f

        // Determine range of days based on periodFilter
        val startDay = when (periodFilter) {
            2 -> 11
            3 -> 21
            else -> 1
        }
        val endDay = when (periodFilter) {
            1 -> 10.coerceAtMost(summary.totalDays)
            2 -> 20.coerceAtMost(summary.totalDays)
            else -> summary.totalDays
        }

        val activeDays = (startDay..endDay).toList()

        var subTotalMorningQty = 0.0
        var subTotalMorningAmt = 0.0
        var subTotalEveningQty = 0.0
        var subTotalEveningAmt = 0.0
        var subTotalQty = 0.0
        var subTotalAmt = 0.0

        for (day in activeDays) {
            if (y + rowH > PAGE_H - MARGIN - 40f) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                canvas = page.canvas
                y = MARGIN
                drawHeader(false)
            }

            val r = recordsMap[day]
            val isEven = day % 2 == 0
            if (isEven) {
                fillPaint.color = AColor.rgb(248, 250, 252)
                canvas.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + rowH), fillPaint)
            }
            canvas.drawLine(MARGIN, y + rowH, MARGIN + CONTENT_W, y + rowH, linePaint)

            var cx = MARGIN

            // 1. Date
            textPaint.color = AColor.rgb(15, 23, 42)
            textPaint.textSize = 8.5f
            val dStr = "%02d".format(day)
            val dW = textPaint.measureText(dStr)
            canvas.drawText(dStr, cx + (colW[0] - dW) / 2, y + 12f, textPaint)
            cx += colW[0]

            // 2. Morning Quantity (show "0 KG" if blank per user request)
            val mQtyVal = r?.morningQty
            val mQty = if (mQtyVal != null && mQtyVal > 0) MilkHisabCalculator.formatQty(mQtyVal) + " KG" else "0 KG"
            if (mQtyVal != null) subTotalMorningQty += mQtyVal
            textPaint.color = if (mQtyVal != null && mQtyVal > 0) AColor.rgb(15, 23, 42) else AColor.rgb(148, 163, 184)
            val mQtyW = textPaint.measureText(mQty)
            canvas.drawText(mQty, cx + colW[1] - mQtyW - 6f, y + 12f, textPaint)
            cx += colW[1]

            // 3. Morning Amount (show "₹0" if blank)
            val mAmtVal = r?.morningAmount
            val mAmt = if (mAmtVal != null && mAmtVal > 0) "₹" + MilkHisabCalculator.formatAmount(mAmtVal) else "₹0"
            if (mAmtVal != null) subTotalMorningAmt += mAmtVal
            textPaint.color = if (mAmtVal != null && mAmtVal > 0) AColor.rgb(15, 23, 42) else AColor.rgb(148, 163, 184)
            val mAmtW = textPaint.measureText(mAmt)
            canvas.drawText(mAmt, cx + colW[2] - mAmtW - 6f, y + 12f, textPaint)
            cx += colW[2]

            // 4. Evening Quantity (show "0 KG" if blank)
            val eQtyVal = r?.eveningQty
            val eQty = if (eQtyVal != null && eQtyVal > 0) MilkHisabCalculator.formatQty(eQtyVal) + " KG" else "0 KG"
            if (eQtyVal != null) subTotalEveningQty += eQtyVal
            textPaint.color = if (eQtyVal != null && eQtyVal > 0) AColor.rgb(15, 23, 42) else AColor.rgb(148, 163, 184)
            val eQtyW = textPaint.measureText(eQty)
            canvas.drawText(eQty, cx + colW[3] - eQtyW - 6f, y + 12f, textPaint)
            cx += colW[3]

            // 5. Evening Amount (show "₹0" if blank)
            val eAmtVal = r?.eveningAmount
            val eAmt = if (eAmtVal != null && eAmtVal > 0) "₹" + MilkHisabCalculator.formatAmount(eAmtVal) else "₹0"
            if (eAmtVal != null) subTotalEveningAmt += eAmtVal
            textPaint.color = if (eAmtVal != null && eAmtVal > 0) AColor.rgb(15, 23, 42) else AColor.rgb(148, 163, 184)
            val eAmtW = textPaint.measureText(eAmt)
            canvas.drawText(eAmt, cx + colW[4] - eAmtW - 6f, y + 12f, textPaint)
            cx += colW[4]

            // 6. Total Qty
            val dayTotalQty = (r?.morningQty ?: 0.0) + (r?.eveningQty ?: 0.0)
            subTotalQty += dayTotalQty
            val tQty = if (dayTotalQty > 0) MilkHisabCalculator.formatQty(dayTotalQty) + " KG" else "0 KG"
            boldPaint.color = if (dayTotalQty > 0) AColor.BLACK else AColor.rgb(148, 163, 184)
            boldPaint.textSize = 8.5f
            val tQtyW = boldPaint.measureText(tQty)
            canvas.drawText(tQty, cx + colW[5] - tQtyW - 6f, y + 12f, boldPaint)
            cx += colW[5]

            // 7. Total Amt
            val dayTotalAmt = (r?.morningAmount ?: 0.0) + (r?.eveningAmount ?: 0.0)
            subTotalAmt += dayTotalAmt
            val tAmt = if (dayTotalAmt > 0) "₹" + MilkHisabCalculator.formatAmount(dayTotalAmt) else "₹0"
            boldPaint.color = if (dayTotalAmt > 0) AColor.rgb(255, 45, 85) else AColor.rgb(148, 163, 184)
            val tAmtW = boldPaint.measureText(tAmt)
            canvas.drawText(tAmt, cx + colW[6] - tAmtW - 6f, y + 12f, boldPaint)

            y += rowH
        }

        // Subtotal row for selected period or month
        fillPaint.color = AColor.rgb(238, 242, 246)
        canvas.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + 20f), fillPaint)
        canvas.drawLine(MARGIN, y + 20f, MARGIN + CONTENT_W, y + 20f, linePaint)

        boldPaint.color = AColor.rgb(15, 23, 42)
        boldPaint.textSize = 9f
        canvas.drawText("SUB-TOTAL", MARGIN + 8f, y + 14f, boldPaint)

        var sx = MARGIN + colW[0]
        val subVals = arrayOf(
            MilkHisabCalculator.formatQty(subTotalMorningQty) + " KG",
            "₹" + MilkHisabCalculator.formatAmount(subTotalMorningAmt),
            MilkHisabCalculator.formatQty(subTotalEveningQty) + " KG",
            "₹" + MilkHisabCalculator.formatAmount(subTotalEveningAmt),
            MilkHisabCalculator.formatQty(subTotalQty) + " KG",
            "₹" + MilkHisabCalculator.formatAmount(subTotalAmt)
        )
        subVals.forEachIndexed { idx, v ->
            val w = colW[idx + 1]
            boldPaint.color = if (idx == 5) AColor.rgb(255, 45, 85) else AColor.rgb(15, 23, 42)
            val vw = boldPaint.measureText(v)
            canvas.drawText(v, sx + w - vw - 6f, y + 14f, boldPaint)
            sx += w
        }

        y += 26f

        // Analytics & Highlights (Highest milk day, Highest earnings day, Dates without milk)
        if (y + 180f > PAGE_H - MARGIN) {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN
        }

        // Calculate analytics
        var maxQtyDay: Int? = null
        var maxQtyVal = 0.0
        var maxEarnDay: Int? = null
        var maxEarnVal = 0.0
        val noMilkDates = mutableListOf<Int>()

        for (d in activeDays) {
            val r = recordsMap[d]
            val totQ = (r?.morningQty ?: 0.0) + (r?.eveningQty ?: 0.0)
            val totA = (r?.morningAmount ?: 0.0) + (r?.eveningAmount ?: 0.0)
            if (totQ > maxQtyVal) {
                maxQtyVal = totQ
                maxQtyDay = d
            }
            if (totA > maxEarnVal) {
                maxEarnVal = totA
                maxEarnDay = d
            }
            if (totQ <= 0.0) {
                noMilkDates.add(d)
            }
        }

        // Highlights Card
        fillPaint.color = AColor.rgb(248, 250, 252)
        val highlightsBox = RectF(MARGIN, y, MARGIN + CONTENT_W, y + 74f)
        canvas.drawRoundRect(highlightsBox, 8f, 8f, fillPaint)
        canvas.drawRoundRect(highlightsBox, 8f, 8f, linePaint)

        boldPaint.color = AColor.rgb(15, 23, 42)
        boldPaint.textSize = 10f
        canvas.drawText("Monthly Insights & Delivery Analytics", MARGIN + 12f, y + 18f, boldPaint)

        textPaint.color = AColor.rgb(51, 65, 85)
        textPaint.textSize = 8.5f

        val peakText = if (maxQtyDay != null) {
            "Peak Milk Delivery: Day %02d (%s KG)".format(maxQtyDay, MilkHisabCalculator.formatQty(maxQtyVal))
        } else {
            "Peak Milk Delivery: None recorded"
        }

        val earnText = if (maxEarnDay != null) {
            "Highest Earnings: Day %02d (₹%s)".format(maxEarnDay, MilkHisabCalculator.formatAmount(maxEarnVal))
        } else {
            "Highest Earnings: None recorded"
        }

        canvas.drawText("• $peakText", MARGIN + 12f, y + 36f, textPaint)
        canvas.drawText("• $earnText", MARGIN + (CONTENT_W / 2f), y + 36f, textPaint)

        val noDeliveryText = if (noMilkDates.isEmpty()) {
            "No Missed Days: Milk delivered every single day this period!"
        } else {
            "Dates Without Delivery (${noMilkDates.size} day(s)): " + noMilkDates.joinToString(", ") { "%02d".format(it) }
        }
        textPaint.color = if (noMilkDates.isEmpty()) AColor.rgb(16, 185, 129) else AColor.rgb(225, 29, 72)
        canvas.drawText("• $noDeliveryText", MARGIN + 12f, y + 54f, textPaint)

        y += 84f

        // Grand Total Box
        fillPaint.color = AColor.rgb(255, 245, 247)
        val grandBox = RectF(MARGIN, y, MARGIN + CONTENT_W, y + 56f)
        canvas.drawRoundRect(grandBox, 8f, 8f, fillPaint)
        val grandBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.rgb(255, 140, 160)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(grandBox, 8f, 8f, grandBorderPaint)

        boldPaint.color = AColor.rgb(255, 45, 85)
        boldPaint.textSize = 11f
        canvas.drawText("STATEMENT SUMMARY — $periodSubtitle", MARGIN + 12f, y + 20f, boldPaint)

        boldPaint.color = AColor.BLACK
        boldPaint.textSize = 11.5f
        canvas.drawText("Total Milk Delivered: ${MilkHisabCalculator.formatQty(subTotalQty)} KG", MARGIN + 12f, y + 42f, boldPaint)

        boldPaint.color = AColor.rgb(255, 45, 85)
        boldPaint.textSize = 12f
        val grandAmtStr = "Total Payable Amount: ₹${MilkHisabCalculator.formatAmount(subTotalAmt)}"
        val grandAmtW = boldPaint.measureText(grandAmtStr)
        canvas.drawText(grandAmtStr, MARGIN + CONTENT_W - grandAmtW - 12f, y + 42f, boldPaint)

        doc.finishPage(page)

        return try {
            FileOutputStream(tempFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()
            NoteExporter.saveToDevice(context, tempFile, fileName, "application/pdf")
        } catch (e: Exception) {
            doc.close()
            null
        }
    }
}
