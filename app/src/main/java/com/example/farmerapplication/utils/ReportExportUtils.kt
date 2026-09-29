package com.example.farmerapplication.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max

object ReportExportUtils {

    fun exportCsv(context: Context, fileName: String, title: String, headers: List<String>, rows: List<List<String>>): Boolean {
        return try {
            val sb = StringBuilder()
            // Title section
            sb.append(escapeCsv(title)).append("\n")
            sb.append("\n")
            // Header row
            sb.append(headers.joinToString(",") { escapeCsv(it) }).append("\n")
            // Data rows
            rows.forEach { row -> sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n") }
            val outputStream = getOutputStream(context = context, fileName = fileName, mimeType = "text/csv")
            if (outputStream == null) return false
            outputStream.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun exportPdf(context: Context, fileName: String, title: String, headers: List<String>, rows: List<List<String>>, columnWidths: List<Int>, @DrawableRes logoResId: Int): Boolean {
        var pdfDocument: PdfDocument? = null
        return try {
            // A4 landscape dimensions
            val pageWidth = 842
            val pageHeight = 595
            val leftMargin = 40f
            val rightMargin = 40f
            val usableWidth = pageWidth - leftMargin - rightMargin
            // Validate column width configuration
            if (headers.size != columnWidths.size) {
                throw IllegalArgumentException("Number of headers must match number of column widths.")
            }
            val widthSum = columnWidths.sum()
            if (widthSum <= 0) {
                throw IllegalArgumentException("Column widths must be greater than zero.")
            }
            pdfDocument = PdfDocument()
            // Paint configuration
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK; textAlign = Paint.Align.CENTER }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; isFakeBoldText = true; color = Color.WHITE; textAlign = Paint.Align.CENTER }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = Color.BLACK; textAlign = Paint.Align.CENTER }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 1f }
            val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D4C41") }
            val alternateRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5F0EA") }
            val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            // Load logo
            val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResId)
            var logoWidth = 0f
            var logoHeight = 0f
            if (logoBitmap != null) {
                val scale = minOf(150f / logoBitmap.width, 65f / logoBitmap.height, 1f)
                logoWidth = logoBitmap.width * scale
                logoHeight = logoBitmap.height * scale
            }
            // Page state
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            fun drawTopSection(): Float {
                var currentY = 25f
                // Draw logo
                if (logoBitmap != null) {
                    val logoLeft = (pageWidth - logoWidth) / 2f
                    val logoRect = RectF(logoLeft, currentY, logoLeft + logoWidth, currentY + logoHeight)
                    canvas.drawBitmap(logoBitmap, null, logoRect, whitePaint)
                    currentY += logoHeight + 20f
                }
                // Draw centered title
                canvas.drawText(title, pageWidth / 2f, currentY, titlePaint)
                currentY += 24f
                // Draw divider
                val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; strokeWidth = 1.5f }
                canvas.drawLine(leftMargin, currentY, pageWidth - rightMargin, currentY, dividerPaint)
                return currentY + 14f
            }

            fun drawWrappedText(c: Canvas, text: String, centerX: Float, topY: Float, cellWidth: Float, paint: Paint, lineHeight: Float, maxLines: Int = 5): Int {
                val availableWidth = max(20f, cellWidth - 12f)
                val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (words.isEmpty()) return 1
                val lines = mutableListOf<String>()
                var currentLine = ""
                for (word in words) {
                    val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(candidate) <= availableWidth) {
                        currentLine = candidate
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        if (paint.measureText(word) > availableWidth) {
                            var partial = ""
                            for (character in word) {
                                val test = partial + character
                                if (paint.measureText(test) <= availableWidth) {
                                    partial = test
                                } else {
                                    if (partial.isNotEmpty()) lines.add(partial)
                                    partial = character.toString()
                                }
                            }
                            currentLine = partial
                        } else {
                            currentLine = word
                        }
                    }
                    if (lines.size >= maxLines) break
                }
                if (currentLine.isNotEmpty() && lines.size < maxLines) lines.add(currentLine)
                if (lines.isEmpty()) lines.add("")
                val actualLineCount = lines.size.coerceAtMost(maxLines)
                val visibleLines = lines.take(actualLineCount)
                val totalTextHeight = visibleLines.size * lineHeight
                var baseline = topY + (lineHeight / 2f) + ((totalTextHeight - lineHeight) / 2f) - ((paint.ascent() + paint.descent()) / 2f)
                visibleLines.forEach { line -> c.drawText(line, centerX, baseline, paint); baseline += lineHeight }
                return visibleLines.size
            }

            fun drawTableHeader(c: Canvas, startY: Float): Float {
                val headerPadding = 8f
                val lineHeight = 13f
                var maxLines = 1
                var x = leftMargin
                headers.forEachIndexed { index, header ->
                    val columnWidth = usableWidth * columnWidths[index].toFloat() / widthSum.toFloat()
                    val lines = calculateLineCount(text = header, paint = headerPaint, cellWidth = columnWidth - 12f, maxLines = 4)
                    maxLines = max(maxLines, lines)
                    x += columnWidth
                }
                val headerHeight = max(30f, maxLines * lineHeight + headerPadding * 2)
                c.drawRect(leftMargin, startY, pageWidth - rightMargin, startY + headerHeight, headerBgPaint)
                x = leftMargin
                headers.forEachIndexed { index, header ->
                    val columnWidth = usableWidth * columnWidths[index].toFloat() / widthSum.toFloat()
                    val centerX = x + columnWidth / 2f
                    drawWrappedText(c = c, text = header, centerX = centerX, topY = startY, cellWidth = columnWidth, paint = headerPaint, lineHeight = lineHeight, maxLines = 4)
                    if (index < headers.lastIndex) {
                        c.drawLine(x + columnWidth, startY, x + columnWidth, startY + headerHeight, linePaint)
                    }
                    x += columnWidth
                }
                return startY + headerHeight
            }

            // Draw top section
            var y = drawTopSection()
            // Draw table header
            y = drawTableHeader(canvas, y)
            val lineHeight = 13f

            rows.forEachIndexed { rowIndex, row ->
                var maxLines = 1
                row.forEachIndexed { index, cell ->
                    if (index >= columnWidths.size) return@forEachIndexed
                    val columnWidth = usableWidth * columnWidths[index].toFloat() / widthSum.toFloat()
                    val lines = calculateLineCount(text = cell, paint = cellPaint, cellWidth = columnWidth - 12f, maxLines = 5)
                    maxLines = max(maxLines, lines)
                }
                val rowHeight = max(30f, maxLines * lineHeight + 14f)
                // Start a new page if the row does not fit
                if (y + rowHeight > pageHeight - 35f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 30f
                    y = drawTableHeader(canvas, y)
                }
                // Alternate row background
                if (rowIndex % 2 == 1) {
                    canvas.drawRect(leftMargin, y, pageWidth - rightMargin, y + rowHeight, alternateRowPaint)
                }
                // Draw cells
                var x = leftMargin
                row.forEachIndexed { index, cell ->
                    if (index >= columnWidths.size) return@forEachIndexed
                    val columnWidth = usableWidth * columnWidths[index].toFloat() / widthSum.toFloat()
                    val centerX = x + columnWidth / 2f
                    drawWrappedText(c = canvas, text = cell, centerX = centerX, topY = y, cellWidth = columnWidth, paint = cellPaint, lineHeight = lineHeight, maxLines = 5)
                    canvas.drawLine(x, y, x, y + rowHeight, linePaint)
                    x += columnWidth
                }
                // Right border
                canvas.drawLine(pageWidth - rightMargin, y, pageWidth - rightMargin, y + rowHeight, linePaint)
                // Bottom border
                canvas.drawLine(leftMargin, y + rowHeight, pageWidth - rightMargin, y + rowHeight, linePaint)
                y += rowHeight
            }

            // Finish current page
            pdfDocument.finishPage(page)
            val outputStream = getOutputStream(context = context, fileName = fileName, mimeType = "application/pdf")
            if (outputStream == null) return false
            outputStream.use { pdfDocument.writeTo(it) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { logoBitmapSafeRecycle() } catch (_: Exception) {}
            try { pdfDocument?.close() } catch (_: Exception) {}
        }
    }

    private fun calculateLineCount(text: String, paint: Paint, cellWidth: Float, maxLines: Int): Int {
        if (text.isBlank()) return 1
        val availableWidth = max(20f, cellWidth)
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return 1
        var lineCount = 1
        var currentLine = ""
        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= availableWidth) {
                currentLine = candidate
            } else {
                lineCount++
                if (lineCount >= maxLines) return maxLines
                currentLine = word
            }
        }
        return lineCount.coerceAtMost(maxLines)
    }

    private fun logoBitmapSafeRecycle() {
        // Bitmap loaded by BitmapFactory is managed safely by Android.
    }

    private fun getOutputStream(context: Context, fileName: String, mimeType: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            FileOutputStream(File(downloadsDir, fileName))
        }
    }
}