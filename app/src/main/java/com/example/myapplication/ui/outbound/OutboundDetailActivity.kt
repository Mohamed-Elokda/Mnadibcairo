package com.example.myapplication.ui.outbound

import android.R.attr.gravity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutboundDetailActivity : AppCompatActivity() {

    private var photoUrl: String? = null
    private lateinit var mainDetailLayout: androidx.constraintlayout.widget.ConstraintLayout
    private var currentDetailsList: List<com.example.myapplication.data.local.entity.OutboundDetailWithItemName> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outbound_detail)
        mainDetailLayout = findViewById(R.id.mainDetailLayout)
        // تعريف العناصر
        val tvCustomer = findViewById<TextView>(R.id.tvDetailCustomer)
        val tvInvoiceNum = findViewById<TextView>(R.id.tvDetailInvoiceNum)
        val tvDate = findViewById<TextView>(R.id.tvDetailDate)

        val tvTotalAmount = findViewById<TextView>(R.id.tvTotalAmount)
        val tvPaidAmount = findViewById<TextView>(R.id.tvPaidAmount)
        val tvRemainingAmount = findViewById<TextView>(R.id.tvRemainingAmount)
        findViewById<Button>(R.id.btnExportExcel).setOnClickListener { exportToExcel() }
        findViewById<Button>(R.id.btnExportPdf).setOnClickListener { printInvoice() }
        val tableItems = findViewById<TableLayout>(R.id.tableDetailItems)
        val btnViewPhoto = findViewById<Button>(R.id.btnViewPhoto)

        // داخل onCreate في OutboundDetailActivity
        val outboundId = intent.getStringExtra("OUTBOUND_ID")
        val invoiceNum = intent.getStringExtra("INVOICE_NUM")
        val customerName = intent.getStringExtra("CUSTOMER_NAME") // تطابق مع المفتاح الجديد
        val remoteImageUrl = intent.getStringExtra("IMAGE_URL")
        val date = intent.getStringExtra("DATE")
        val paidAmount = intent.getStringExtra("PAID_AMOUNT") ?: "0"

// تعيين البيانات للواجهة
        tvCustomer.text = "العميل: $customerName"
        tvInvoiceNum.text = "رقم الفاتورة: $invoiceNum"
        tvDate.text = "التاريخ: ${date?.substringBefore(" ")}"
        tvPaidAmount.text = "$paidAmount ج.م"

        btnViewPhoto.setOnClickListener {
            if (!remoteImageUrl.isNullOrEmpty()) {
                showImageDialog(remoteImageUrl!!)
            } else {
                Toast.makeText(this, "لا توجد صورة لهذه الفاتورة", Toast.LENGTH_SHORT).show()
            }
        }

        if (outboundId != null) {
            loadFullData(outboundId)
        }
    }
    private fun exportToExcel() {
        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("فاتورة صادرة")

        // إنشاء رأس الفاتورة
        val headerInfo = sheet.createRow(0)
        headerInfo.createCell(0).setCellValue("رقم الفاتورة: ${intent.getStringExtra("INVOICE_NUM")}")
        headerInfo.createCell(1).setCellValue("العميل: ${intent.getStringExtra("CUSTOMER_NAME")}")

        // عناوين الجدول
        val headerRow = sheet.createRow(2)
        val columns = arrayOf("الصنف", "الكمية", "السعر", "الإجمالي")
        columns.forEachIndexed { i, col -> headerRow.createCell(i).setCellValue(col) }

        // تعبئة البيانات
        currentDetailsList.forEachIndexed { index, detail ->
            val row = sheet.createRow(index + 3)
            row.createCell(0).setCellValue(detail.itemName)
            row.createCell(1).setCellValue(detail.quantity.toDouble())
            row.createCell(2).setCellValue(detail.price)
            row.createCell(3).setCellValue(detail.quantity * detail.price)
        }

        val file = java.io.File(getExternalFilesDir(null), "Invoice_${intent.getStringExtra("INVOICE_NUM")}.xlsx")
        val out = java.io.FileOutputStream(file)
        workbook.write(out)
        out.close()
        workbook.close()

        Toast.makeText(this, "تم حفظ ملف Excel", Toast.LENGTH_SHORT).show()
        // يمكنك استدعاء دالة فتح الملف هنا
    }
    private fun printInvoice() {
        val printManager = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "Invoice_${intent.getStringExtra("INVOICE_NUM") ?: "000"}"

        val printAdapter = object : android.print.PrintDocumentAdapter() {
            override fun onLayout(oldAttributes: android.print.PrintAttributes?, newAttributes: android.print.PrintAttributes, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
                val info = android.print.PrintDocumentInfo.Builder(jobName)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(pages: Array<out android.print.PageRange>?, destination: android.os.ParcelFileDescriptor, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback) {
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = android.graphics.Paint()

                // إعدادات النصوص
                val titlePaint = android.graphics.Paint().apply {
                    textSize = 22f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val textPaint = android.graphics.Paint().apply {
                    textSize = 12f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

                // 1. رأس الفاتورة (Header)
                canvas.drawText("فاتورة مبيعات", 297f, 60f, titlePaint)

                val startY = 100f
                val rightMargin = 550f
                canvas.drawText("المورد/العميل: ${intent.getStringExtra("CUSTOMER_NAME") ?: "عام"}", rightMargin, startY, textPaint)
                canvas.drawText("رقم الفاتورة: ${intent.getStringExtra("INVOICE_NUM") ?: "---"}", rightMargin, startY + 20f, textPaint)
                canvas.drawText("التاريخ: ${intent.getStringExtra("DATE")?.substringBefore(" ") ?: ""}", rightMargin, startY + 40f, textPaint)

                // 2. رسم الجدول (Table Structure)
                var currentY = 180f
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 1f

                // رسم خطوط الرأس
                canvas.drawLine(40f, currentY, 550f, currentY, paint) // الخط العلوي
                currentY += 20f

                // عناوين الأعمدة
                textPaint.isFakeBoldText = true
                canvas.drawText("الصنف", 540f, currentY - 5f, textPaint)
                canvas.drawText("الكمية", 380f, currentY - 5f, textPaint)
                canvas.drawText("السعر", 250f, currentY - 5f, textPaint)
                canvas.drawText("الإجمالي", 100f, currentY - 5f, textPaint)
                textPaint.isFakeBoldText = false

                canvas.drawLine(40f, currentY, 550f, currentY, paint) // خط تحت العناوين
                currentY += 25f

                // 3. إضافة الأصناف من القائمة
                currentDetailsList.forEach { detail ->
                    canvas.drawText(detail.itemName, 540f, currentY, textPaint)
                    canvas.drawText(detail.quantity.toString(), 380f, currentY, textPaint)
                    canvas.drawText("${detail.price}", 250f, currentY, textPaint)
                    canvas.drawText("${detail.quantity * detail.price}", 100f, currentY, textPaint)

                    currentY += 15f
                    canvas.drawLine(40f, currentY, 550f, currentY, paint) // خط فاصل بين الأصناف
                    currentY += 20f
                }

                // 4. التذييل (Footer) - الإجماليات
                currentY += 20f
                val totalAmount = currentDetailsList.sumOf { it.quantity * it.price }
                val paidAmount = intent.getStringExtra("PAID_AMOUNT")?.toDoubleOrNull() ?: 0.0

                textPaint.isFakeBoldText = true
                canvas.drawText("إجمالي القيمة: $totalAmount ج.م", rightMargin, currentY, textPaint)
                canvas.drawText("المبلغ المدفوع: $paidAmount ج.م", rightMargin, currentY + 25f, textPaint)

                paint.color = android.graphics.Color.RED
                canvas.drawText("المتبقي: ${totalAmount - paidAmount} ج.م", rightMargin, currentY + 50f, textPaint)

                pdfDocument.finishPage(page)

                try {
                    pdfDocument.writeTo(java.io.FileOutputStream(destination.fileDescriptor))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdfDocument.close()
                }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            }
        }

        printManager.print(jobName, printAdapter, null)
    }

    private fun loadFullData(outboundId: String) {
        val database = AppDatabase.getDatabase(this)
        val table = findViewById<TableLayout>(R.id.tableDetailItems)

        lifecycleScope.launch {
            // جمع البيانات من الـ Flow
            database.outboundDetailesDao()
                .getDetailsByOutboundId(outboundId.toLong())
                .collect { detailsList ->

                    // الانتقال للخيط الرئيسي لتحديث الواجهة
                    withContext(Dispatchers.Main) {
                        currentDetailsList = detailsList
                        // مسح الصفوف القديمة (عدا العنوان)
                        if (table.childCount > 1) {
                            table.removeViews(1, table.childCount - 1)
                        }

                        var totalInvoice = 0.0

                        detailsList.forEach { detail ->
                            val row = TableRow(this@OutboundDetailActivity).apply {
                                setPadding(8, 24, 8, 24)
                                gravity = Gravity.CENTER_VERTICAL
                            }

                            // الآن نستخدم itemName الذي جاء من الـ JOIN
                            row.addView(createDetailTextView(detail.itemName))
                            row.addView(createDetailTextView(detail.quantity.toString()))
                            row.addView(createDetailTextView("${detail.price} ج.م"))

                            val subTotal = detail.quantity * detail.price
                            totalInvoice += subTotal
                            row.addView(createDetailTextView("$subTotal ج.م"))

                            table.addView(row)
                        }

                        // تحديث الإجماليات
                        findViewById<TextView>(R.id.tvTotalAmount).text = "الإجمالي: $totalInvoice ج.م"
                        val paid = intent.getStringExtra("PAID_AMOUNT")?.toDoubleOrNull() ?: 0.0
                        findViewById<TextView>(R.id.tvRemainingAmount).text = "المتبقي: ${totalInvoice - paid} ج.م"
                    }
                }
        }
    }

    // دالة مساعدة لتنسيق نصوص الجدول
    private fun createDetailTextView(txt: String): TextView {
        return TextView(this).apply {
            text = txt
            gravity = Gravity.CENTER
            textSize = 14f
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun showImageDialog(url: String) {
        val imageView = ImageView(this)
        imageView.adjustViewBounds = true
        imageView.load(url)

        AlertDialog.Builder(this)
            .setTitle("صورة الفاتورة")
            .setView(imageView)
            .setPositiveButton("إغلاق", null)
            .show()
    }
}