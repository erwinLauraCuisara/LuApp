package com.example.luapp.ui.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.luapp.data.model.ConsumptionItem
import com.example.luapp.data.model.ExpenseItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun generateCashRegisterPdf(
    context: Context,
    cashRegisterId: Long,
    openedAt: Long,
    closedAt: Long,
    consumptions: List<ConsumptionItem>,
    expenses: List<ExpenseItem>
): File {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fileFmt = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault())

    val W = 595
    val H = 842
    val ML = 40f
    val CW = W - ML * 2

    val doc = PdfDocument()
    var pageNum = 0
    lateinit var page: PdfDocument.Page
    var canvas: android.graphics.Canvas? = null
    var y = 0f

    // drawText uses the BASELINE at y. Ascent (top of text) is ~80% of textSize above baseline.
    // After a divider line at y, must advance at least ~ascent before drawing next text.
    val boldLg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.BLACK
    }
    val boldMd = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.BLACK
    }
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK }
    val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f; color = Color.GRAY }
    val errPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.rgb(176, 0, 32) }
    val thin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE }
    val thick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; strokeWidth = 1f; style = Paint.Style.STROKE }

    fun trunc(text: String, maxW: Float, p: Paint): String {
        if (p.measureText(text) <= maxW) return text
        var s = text
        while (s.isNotEmpty() && p.measureText("$s...") > maxW) s = s.dropLast(1)
        return "$s..."
    }

    fun newPage() {
        if (pageNum > 0) doc.finishPage(page)
        pageNum++
        page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNum).create())
        canvas = page.canvas
        // Start with enough space so first text top doesn't get cut (ascent of boldLg ≈ 15pt)
        y = 55f
    }

    fun need(h: Float) { if (y + h > H - 48f) newPage() }
    fun nl(h: Float) { y += h }
    fun right(text: String, p: Paint) { canvas?.drawText(text, ML + CW - p.measureText(text), y, p) }

    // Draws a horizontal line, then advances enough so the NEXT text's ascent clears the line.
    // Rule: after line at y, next baseline must be >= y + ascent_max(~13f) + gap(5f) = 18f.
    fun hline(p: Paint = thin) {
        canvas?.drawLine(ML, y, ML + CW, y, p)
        nl(18f)
    }

    // Section heading: text then a thin underline, with proper gap before/after.
    fun section(title: String) {
        need(44f)
        canvas?.drawText(title, ML, y, boldMd)
        // Gap below text: descent(~3f) + visual gap(5f) = 8f minimum so line sits clearly below title
        nl(10f)
        hline()
    }

    // A text row with left and right content, returns after drawing.
    fun row(l: String, r: String, lp: Paint = body, rp: Paint = lp) {
        need(22f)
        canvas?.drawText(trunc(l, CW * 0.65f, lp), ML, y, lp)
        right(r, rp)
        nl(20f)
    }

    // Thin separator between list items; advances enough for next item's ascent.
    fun itemDivider() {
        canvas?.drawLine(ML, y, ML + CW, y, thin)
        nl(16f)
    }

    // ── Page 1 ──
    newPage()

    // Centered header
    val t1 = "CIERRE DE CAJA"
    canvas?.drawText(t1, (W - boldLg.measureText(t1)) / 2f, y, boldLg)
    nl(28f)
    val t2 = "Caja #$cashRegisterId"
    canvas?.drawText(t2, (W - boldMd.measureText(t2)) / 2f, y, boldMd)
    nl(22f)
    hline(thick)

    canvas?.drawText("Apertura: ${dateFmt.format(Date(openedAt))}", ML, y, body)
    nl(18f)
    canvas?.drawText("Cierre:   ${dateFmt.format(Date(closedAt))}", ML, y, body)
    nl(22f)
    hline(thick)

    // ── Resumen ──
    val totC = consumptions.sumOf { it.amount + it.amountQr }
    val totCash = consumptions.sumOf { it.amount }
    val totQr = consumptions.sumOf { it.amountQr }
    val totE = expenses.sumOf { it.amount }
    val totD = consumptions.sumOf { it.pendingAmount ?: 0.0 }

    section("RESUMEN")
    row("Consumos", "Bs %.2f".format(totC))
    if (totQr > 0) {
        row("  Efectivo", "Bs %.2f".format(totCash), small, small)
        row("  QR", "Bs %.2f".format(totQr), small, small)
    }
    row("Gastos", "Bs %.2f".format(totE))
    if (totD > 0) row("Deudas pendientes", "Bs %.2f".format(totD), errPaint)
    // Thin divider above NETO: sits below last row (already 20f below its baseline)
    canvas?.drawLine(ML, y, ML + CW, y, thin)
    nl(18f)
    row("NETO", "Bs %.2f".format(totC - totE), boldMd)
    nl(10f)

    // ── Pago a chicas ──
    val buddyShareMap = mutableMapOf<Long, Double>()
    val buddyNameMap = mutableMapOf<Long, String>()
    consumptions.forEach { c ->
        val n = c.buddyIds.size
        if (n == 0) return@forEach
        val share = c.appointmentFee / n
        c.buddyIds.forEachIndexed { i, id ->
            buddyShareMap[id] = (buddyShareMap[id] ?: 0.0) + share
            buddyNameMap[id] = c.buddyNames.getOrElse(i) { "?" }
        }
    }
    val buddies = buddyShareMap.entries
        .map { (id, total) -> buddyNameMap[id]!! to total }
        .sortedBy { it.first }

    if (buddies.isNotEmpty()) {
        hline(thick)
        section("PAGO A CHICAS")
        buddies.forEach { (name, total) -> row(name, "Bs %.2f".format(total)) }
        nl(8f)
    }

    // ── Consumos ──
    hline(thick)
    section("CONSUMOS (${consumptions.size})")

    if (consumptions.isEmpty()) {
        need(20f)
        canvas?.drawText("Sin consumos", ML, y, small)
        nl(20f)
    } else {
        consumptions.forEach { item ->
            val hasMeta = item.customerName != null || item.buddyNames.isNotEmpty()
            val hasFin  = item.appointmentFee > 0 || (item.pendingAmount ?: 0.0) > 0
            val lineCount = 3 + (if (hasMeta) 1 else 0) + (if (hasFin) 1 else 0)
            need(lineCount * 16f + 20f)

            // Concept + amount
            canvas?.drawText(trunc(item.concept, CW * 0.65f, body), ML, y, body)
            right("Bs %.2f".format(item.amount + item.amountQr), body)
            nl(16f)
            if (item.amountQr > 0) {
                val breakdown = buildString {
                    if (item.amount > 0) append("Efectivo: Bs %.2f".format(item.amount))
                    if (item.amount > 0 && item.amountQr > 0) append("  /  ")
                    append("QR: Bs %.2f".format(item.amountQr))
                }
                canvas?.drawText(trunc(breakdown, CW, small), ML, y, small)
                nl(14f)
            }

            // Customer / buddy
            val meta = listOfNotNull(
                item.customerName?.let { "Cliente: $it" },
                if (item.buddyNames.isNotEmpty()) "Chica${if (item.buddyNames.size > 1) "s" else ""}: ${item.buddyNames.joinToString(", ")}" else null
            )
            if (meta.isNotEmpty()) {
                canvas?.drawText(trunc(meta.joinToString("  /  "), CW, small), ML, y, small)
                nl(14f)
            }

            // Fee / pending
            val fin = buildList {
                if (item.appointmentFee > 0) add("Comision: Bs %.2f".format(item.appointmentFee))
                if ((item.pendingAmount ?: 0.0) > 0) add("[!] Deuda: Bs %.2f".format(item.pendingAmount))
            }
            if (fin.isNotEmpty()) {
                val fp = if ((item.pendingAmount ?: 0.0) > 0) errPaint else small
                canvas?.drawText(trunc(fin.joinToString("  /  "), CW, fp), ML, y, fp)
                nl(14f)
            }

            // Date
            canvas?.drawText(dateFmt.format(Date(item.createdAt)), ML, y, small)
            nl(14f)

            itemDivider()
        }
    }

    // ── Gastos ──
    hline(thick)
    section("GASTOS (${expenses.size})")

    if (expenses.isEmpty()) {
        need(20f)
        canvas?.drawText("Sin gastos", ML, y, small)
        nl(20f)
    } else {
        expenses.forEach { item ->
            val hasDetails = !item.details.isNullOrBlank()
            need((2 + (if (hasDetails) 1 else 0)) * 16f + 20f)

            canvas?.drawText(trunc(item.concept, CW * 0.65f, body), ML, y, body)
            right("Bs %.2f".format(item.amount), body)
            nl(16f)

            canvas?.drawText(dateFmt.format(Date(item.createdAt)), ML, y, small)
            nl(14f)

            if (hasDetails) {
                canvas?.drawText(trunc(item.details!!, CW, small), ML, y, small)
                nl(14f)
            }

            itemDivider()
        }
    }

    // Footer
    need(30f)
    nl(12f)
    hline()
    canvas?.drawText("Generado el ${dateFmt.format(Date())}", ML, y, small)

    doc.finishPage(page)

    val dir = File(context.cacheDir, "pdfs").also { it.mkdirs() }
    val file = File(dir, "Caja_${cashRegisterId}_${fileFmt.format(Date(closedAt))}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
}
