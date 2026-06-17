package com.sunny.healthapp.data.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.sunny.healthapp.HealthApp
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

object DoctorReportGenerator {

    private const val PAGE_WIDTH = 595  // A4 in 72-dpi points (~8.27 in)
    private const val PAGE_HEIGHT = 842 // A4 (~11.69 in)
    private const val MARGIN = 36f

    suspend fun generate(context: Context): android.net.Uri {
        val app = context.applicationContext as HealthApp
        val today = LocalDate.now()
        val from = today.minusDays(30)

        // Collect 30 days of data
        val days = (0..30).map { offset ->
            val d = today.minusDays(offset.toLong())
            DayData(
                date = d,
                summary = runCatching { app.repository.dailySummary(d) }.getOrNull(),
                sleep = runCatching { app.repository.sleepOnDate(d) }.getOrNull(),
            )
        }.reversed() // ascending by date

        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(info)
        val canvas = page.canvas

        var y = MARGIN

        // ---- Header ----
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Vitals — Health Summary", MARGIN, y + 20, headerPaint)
        y += 36

        val subPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isAntiAlias = true
        }
        val rangeFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
        canvas.drawText(
            "30-day report · ${from.format(rangeFmt)} → ${today.format(rangeFmt)}",
            MARGIN, y, subPaint,
        )
        y += 16
        canvas.drawText(
            "Generated on ${Instant.now().atZone(ZoneId.systemDefault()).format(rangeFmt)} · Source: Fitbit via Health Connect",
            MARGIN, y, subPaint,
        )
        y += 22

        // ---- Summary stats ----
        val statPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }
        val rhrValues = days.mapNotNull { it.summary?.restingHeartRate }
        val stepValues = days.mapNotNull { it.summary?.steps?.takeIf { s -> s > 0 } }
        val sleepValues = days.mapNotNull { it.sleep?.total?.toMinutes()?.takeIf { m -> m > 0 } }
        val hrvValues = days.mapNotNull { it.sleep?.avgHrv }

        sectionHeader(canvas, "Averages over the period", y, headerPaint = makeSection())
        y += 24
        val cellW = (PAGE_WIDTH - MARGIN * 2) / 4f
        drawStat(canvas, MARGIN + cellW * 0, y, "Resting HR", rhrValues.takeIf { it.isNotEmpty() }?.average()?.toInt()?.let { "$it bpm" } ?: "—", labelPaint, statPaint)
        drawStat(canvas, MARGIN + cellW * 1, y, "HRV", hrvValues.takeIf { it.isNotEmpty() }?.average()?.toInt()?.let { "$it ms" } ?: "—", labelPaint, statPaint)
        drawStat(canvas, MARGIN + cellW * 2, y, "Sleep / night", sleepValues.takeIf { it.isNotEmpty() }?.average()?.toLong()?.let { fmtDur(it) } ?: "—", labelPaint, statPaint)
        drawStat(canvas, MARGIN + cellW * 3, y, "Steps / day", stepValues.takeIf { it.isNotEmpty() }?.average()?.toLong()?.let { "%,d".format(it) } ?: "—", labelPaint, statPaint)
        y += 56

        // ---- Resting HR chart ----
        if (rhrValues.size >= 2) {
            sectionHeader(canvas, "Resting heart rate (bpm)", y, headerPaint = makeSection())
            y += 14
            y = drawLineChart(
                canvas = canvas,
                top = y,
                heightPx = 110f,
                values = days.map { it.summary?.restingHeartRate?.toFloat() ?: Float.NaN },
                color = Color.rgb(255, 90, 110),
            ) + 12
        }

        // ---- Sleep duration chart ----
        if (sleepValues.size >= 2) {
            sectionHeader(canvas, "Sleep duration (hours)", y, headerPaint = makeSection())
            y += 14
            y = drawLineChart(
                canvas = canvas,
                top = y,
                heightPx = 110f,
                values = days.map { (it.sleep?.total?.toMinutes()?.toFloat() ?: Float.NaN) / 60f },
                color = Color.rgb(120, 156, 255),
            ) + 12
        }

        // ---- HRV chart ----
        if (hrvValues.size >= 2) {
            sectionHeader(canvas, "HRV (ms)", y, headerPaint = makeSection())
            y += 14
            y = drawLineChart(
                canvas = canvas,
                top = y,
                heightPx = 110f,
                values = days.map { it.sleep?.avgHrv?.toFloat() ?: Float.NaN },
                color = Color.rgb(185, 156, 255),
            ) + 16
        }

        // ---- Footer notes ----
        val footPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            isAntiAlias = true
        }
        val notes = listOf(
            "Data captured by a wrist-worn Fitbit device and bridged via Android Health Connect.",
            "HRV values are nightly RMSSD averages; sleep duration excludes awake periods.",
            "This report is informational and not a medical diagnosis. Please discuss with a clinician.",
        )
        notes.forEach { line ->
            canvas.drawText(line, MARGIN, y, footPaint)
            y += 12
        }

        doc.finishPage(page)

        // Write to cache and return a content URI via FileProvider
        val outDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(outDir, "vitals-doctor-report-${today}.pdf")
        outFile.outputStream().use { doc.writeTo(it) }
        doc.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile,
        )
    }

    private data class DayData(
        val date: LocalDate,
        val summary: com.sunny.healthapp.domain.model.DailySummary?,
        val sleep: com.sunny.healthapp.domain.model.SleepSummary?,
    )

    private fun makeSection() = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    private fun sectionHeader(
        canvas: android.graphics.Canvas,
        text: String,
        y: Float,
        headerPaint: Paint,
    ) {
        canvas.drawText(text, MARGIN, y, headerPaint)
    }

    private fun drawStat(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valuePaint: Paint,
    ) {
        canvas.drawText(label.uppercase(), x, y, labelPaint)
        val bigValue = Paint(valuePaint).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(value, x, y + 22, bigValue)
    }

    private fun drawLineChart(
        canvas: android.graphics.Canvas,
        top: Float,
        heightPx: Float,
        values: List<Float>,
        color: Int,
    ): Float {
        val chartLeft = MARGIN
        val chartRight = PAGE_WIDTH - MARGIN
        val chartTop = top
        val chartBottom = top + heightPx
        val chartWidth = chartRight - chartLeft

        val axisPaint = Paint().apply {
            this.color = Color.LTGRAY
            strokeWidth = 0.6f
            isAntiAlias = true
        }
        // axis frame
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)

        // Find min/max over non-NaN
        val nonNan = values.filter { !it.isNaN() }
        if (nonNan.isEmpty()) return chartBottom
        var minV = nonNan.min()
        var maxV = nonNan.max()
        if (minV == maxV) {
            minV -= 1f; maxV += 1f
        }
        val padded = (maxV - minV) * 0.15f
        minV = max(0f, minV - padded)
        maxV += padded

        val linePaint = Paint().apply {
            this.color = color
            strokeWidth = 2f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        val fillPaint = Paint().apply {
            this.color = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val dotPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val mapped = values.mapIndexed { i, v ->
            val xt = if (values.size <= 1) 0f else i.toFloat() / (values.size - 1)
            val x = chartLeft + xt * chartWidth
            val y = if (v.isNaN()) Float.NaN
            else chartBottom - ((v - minV) / (maxV - minV)) * heightPx
            x to y
        }

        val path = Path()
        var pathStarted = false
        mapped.forEach { (x, y) ->
            if (y.isNaN()) return@forEach
            if (!pathStarted) {
                path.moveTo(x, y)
                pathStarted = true
            } else {
                path.lineTo(x, y)
            }
        }
        val fillPath = Path(path)
        val lastX = mapped.lastOrNull { !it.second.isNaN() }?.first ?: chartLeft
        val firstX = mapped.firstOrNull { !it.second.isNaN() }?.first ?: chartLeft
        fillPath.lineTo(lastX, chartBottom)
        fillPath.lineTo(firstX, chartBottom)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)

        // Endpoint dot
        val end = mapped.lastOrNull { !it.second.isNaN() }
        if (end != null) {
            canvas.drawCircle(end.first, end.second, 3f, dotPaint)
        }

        // Min/max labels
        val labelPaint = Paint().apply {
            this.color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("%.0f".format(maxV), chartLeft + 2, chartTop + 9, labelPaint)
        canvas.drawText("%.0f".format(minV), chartLeft + 2, chartBottom - 2, labelPaint)
        return chartBottom
    }

    private fun fmtDur(min: Long): String {
        val h = min / 60
        val m = min % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
