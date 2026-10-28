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

    fun exportMonthPdf(
        context: Context,
        year: Int,
        month: Int,
        recordsMap: Map<Int, MilkRecordEntity>
    ): SavedExport? {
        val summary = MilkHisabCalculator.computeSummary(year, month, recordsMap)
        val monthName = summary.monthName
        val fileName = "Milk_Hisab_${monthName}_$year.pdf"

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val tempFile = File(dir, "temp_${System.currentTimeMillis()}_$fileName")

        val doc = PdfDocument()
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var canvas = page.canvas
        var y = MARGIN

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.DKGRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.BLACK
            textSize = 9.5f
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

        fun drawHeader(onFirstPage: Boolean) {
            if (onFirstPage) {
                // Brand label
                canvas.drawText("AU NOTES • ACCOUNTING", MARGIN, y + 14f, boldPaint.apply {
                    color = AColor.rgb(255, 45, 85)
                    textSize = 10f
                })
                y += 20f

                // Main Title
                canvas.drawText("Milk Dairy Hisab", MARGIN, y + 18f, headerTitlePaint)
                y += 24f

                // Subtitle: Month & Year
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AColor.DKGRAY
                    textSize = 12f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                canvas.drawText("$monthName $year", MARGIN, y + 12f, subPaint)

                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())
                val rightText = "Generated: $dateStr"
                val rightW = textPaint.measureText(rightText)
                canvas.drawText(rightText, MARGIN + CONTENT_W - rightW, y + 12f, textPaint)
                y += 24f

                // Divider line
                canvas.drawLine(MARGIN, y, MARGIN + CONTENT_W, y, linePaint)
                y += 12f
            } else {
                // Secondary page header
                canvas.drawText("Milk Dairy Hisab — $monthName $year (Page $pageNo)", MARGIN, y + 10f, boldPaint)
                y += 18f
            }

            // Draw Table Header
            val colW = floatArrayOf(40f, 75f, 75f, 75f, 75f, 90f, 93f)
            val titles = arrayOf("Date", "Subah Qty", "Subah ₹", "Shaam Qty", "Shaam ₹", "Total Qty", "Total ₹")

            fillPaint.color = AColor.rgb(244, 245, 247)
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + 22f), 4f, 4f, fillPaint)
            canvas.drawRoundRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + 22f), 4f, 4f, linePaint)

            var cx = MARGIN
            titles.forEachIndexed { i, t ->
                val w = colW[i]
                boldPaint.color = AColor.rgb(40, 44, 52)
                boldPaint.textSize = 8.5f
                val tw = boldPaint.measureText(t)
                val textX = if (i == 0) cx + (w - tw) / 2 else cx + w - tw - 6f
                canvas.drawText(t, textX, y + 15f, boldPaint)
                cx += w
            }
            y += 24f
        }

        // Draw First Header
        drawHeader(true)

        val colW = floatArrayOf(40f, 75f, 75f, 75f, 75f, 90f, 93f)
        val rowH = 17f

        // Loop through all days 1..totalDays
        for (day in 1..summary.totalDays) {
            // Check if page needs to break
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
                fillPaint.color = AColor.rgb(250, 251, 253)
                canvas.drawRect(RectF(MARGIN, y, MARGIN + CONTENT_W, y + rowH), fillPaint)
            }
            canvas.drawLine(MARGIN, y + rowH, MARGIN + CONTENT_W, y + rowH, linePaint)

            var cx = MARGIN

            // 1. Date
            textPaint.color = AColor.BLACK
            textPaint.textSize = 8.5f
            val dStr = "%02d".format(day)
            val dW = textPaint.measureText(dStr)
            canvas.drawText(dStr, cx + (colW[0] - dW) / 2, y + 12f, textPaint)
            cx += colW[0]

            // 2. Subah Qty
            val sQty = if (r?.morningQty != null) MilkHisabCalculator.formatQty(r.morningQty) + " KG" else "---"
            val sQtyW = textPaint.measureText(sQty)
            canvas.drawText(sQty, cx + colW[1] - sQtyW - 6f, y + 12f, textPaint)
            cx += colW[1]

            // 3. Subah Amt
            val sAmt = if (r?.morningAmount != null) "₹" + MilkHisabCalculator.formatAmount(r.morningAmount) else "---"
            val sAmtW = textPaint.measureText(sAmt)
            canvas.drawText(sAmt, cx + colW[2] - sAmtW - 6f, y + 12f, textPaint)
            cx += colW[2]

            // 4. Shaam Qty
            val eQty = if (r?.eveningQty != null) MilkHisabCalculator.formatQty(r.eveningQty) + " KG" else "---"
            val eQtyW = textPaint.measureText(eQty)
            canvas.drawText(eQty, cx + colW[3] - eQtyW - 6f, y + 12f, textPaint)
            cx += colW[3]

            // 5. Shaam Amt
            val eAmt = if (r?.eveningAmount != null) "₹" + MilkHisabCalculator.formatAmount(r.eveningAmount) else "---"
            val eAmtW = textPaint.measureText(eAmt)
            canvas.drawText(eAmt, cx + colW[4] - eAmtW - 6f, y + 12f, textPaint)
            cx += colW[4]

            // 6. Total Qty
            val tQty = if (r != null && r.hasEntry) MilkHisabCalculator.formatQty(r.dailyTotalQty) + " KG" else "0 KG"
            boldPaint.color = AColor.BLACK
            boldPaint.textSize = 8.5f
            val tQtyW = boldPaint.measureText(tQty)
            canvas.drawText(tQty, cx + colW[5] - tQtyW - 6f, y + 12f, boldPaint)
            cx += colW[5]

            // 7. Total Amt
            val tAmt = if (r != null && r.hasEntry) "₹" + MilkHisabCalculator.formatAmount(r.dailyTotalAmount) else "₹0"
            boldPaint.color = AColor.rgb(255, 45, 85)
            val tAmtW = boldPaint.measureText(tAmt)
            canvas.drawText(tAmt, cx + colW[6] - tAmtW - 6f, y + 12f, boldPaint)

            y += rowH
        }

        y += 14f

        // Period Summaries Section
        if (y + 160f > PAGE_H - MARGIN) {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN
            canvas.drawText("Milk Dairy Hisab — $monthName $year Period & Monthly Summaries", MARGIN, y + 14f, boldPaint.apply {
                color = AColor.BLACK
                textSize = 12f
            })
            y += 24f
        }

        // Draw Period Cards
        val pCardW = (CONTENT_W - 16f) / 3f
        val periods = listOf(summary.period1, summary.period2, summary.period3)

        periods.forEachIndexed { i, p ->
            val px = MARGIN + i * (pCardW + 8f)
            fillPaint.color = AColor.rgb(248, 249, 251)
            canvas.drawRoundRect(RectF(px, y, px + pCardW, y + 84f), 6f, 6f, fillPaint)
            canvas.drawRoundRect(RectF(px, y, px + pCardW, y + 84f), 6f, 6f, linePaint)

            boldPaint.color = AColor.rgb(255, 45, 85)
            boldPaint.textSize = 9.5f
            canvas.drawText("Period ${p.periodIndex} (${p.startDay}–${p.endDay})", px + 8f, y + 16f, boldPaint)

            textPaint.color = AColor.rgb(60, 64, 70)
            textPaint.textSize = 8f
            canvas.drawText("Subah: ${MilkHisabCalculator.formatQty(p.morningQty)} KG | ₹${MilkHisabCalculator.formatAmount(p.morningAmount)}", px + 8f, y + 32f, textPaint)
            canvas.drawText("Shaam: ${MilkHisabCalculator.formatQty(p.eveningQty)} KG | ₹${MilkHisabCalculator.formatAmount(p.eveningAmount)}", px + 8f, y + 46f, textPaint)

            canvas.drawLine(px + 8f, y + 54f, px + pCardW - 8f, y + 54f, linePaint)

            boldPaint.color = AColor.BLACK
            boldPaint.textSize = 8.5f
            canvas.drawText("Total: ${MilkHisabCalculator.formatQty(p.totalQty)} KG", px + 8f, y + 68f, boldPaint)
            boldPaint.color = AColor.rgb(255, 45, 85)
            val pAmtStr = "₹${MilkHisabCalculator.formatAmount(p.totalAmount)}"
            val pAmtW = boldPaint.measureText(pAmtStr)
            canvas.drawText(pAmtStr, px + pCardW - pAmtW - 8f, y + 68f, boldPaint)
        }

        y += 96f

        // Grand Monthly Totals Box
        fillPaint.color = AColor.rgb(255, 245, 247) // Light crimson tint
        val grandBox = RectF(MARGIN, y, MARGIN + CONTENT_W, y + 70f)
        canvas.drawRoundRect(grandBox, 8f, 8f, fillPaint)
        val grandBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.rgb(255, 140, 160)
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(grandBox, 8f, 8f, grandBorderPaint)

        boldPaint.color = AColor.rgb(255, 45, 85)
        boldPaint.textSize = 12f
        canvas.drawText("Monthly Grand Total — $monthName $year", MARGIN + 14f, y + 22f, boldPaint)

        textPaint.color = AColor.DKGRAY
        textPaint.textSize = 9.5f
        canvas.drawText(
            "Monthly Subah: ${MilkHisabCalculator.formatQty(summary.monthlyMorningQty)} KG  (₹${MilkHisabCalculator.formatAmount(summary.monthlyMorningAmount)})   •   Monthly Shaam: ${MilkHisabCalculator.formatQty(summary.monthlyEveningQty)} KG  (₹${MilkHisabCalculator.formatAmount(summary.monthlyEveningAmount)})",
            MARGIN + 14f,
            y + 40f,
            textPaint
        )

        boldPaint.color = AColor.BLACK
        boldPaint.textSize = 12f
        canvas.drawText("Total Milk: ${MilkHisabCalculator.formatQty(summary.grandTotalQty)} KG", MARGIN + 14f, y + 58f, boldPaint)

        boldPaint.color = AColor.rgb(255, 45, 85)
        boldPaint.textSize = 13f
        val grandAmtStr = "Total Amount: ₹${MilkHisabCalculator.formatAmount(summary.grandTotalAmount)}"
        val grandAmtW = boldPaint.measureText(grandAmtStr)
        canvas.drawText(grandAmtStr, MARGIN + CONTENT_W - grandAmtW - 14f, y + 58f, boldPaint)

        // Finish last page
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
