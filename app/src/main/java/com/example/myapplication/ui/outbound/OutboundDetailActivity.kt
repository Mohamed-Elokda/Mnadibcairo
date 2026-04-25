package com.example.myapplication.ui.outbound

import android.R.attr.gravity
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import dagger.hilt.android.AndroidEntryPoint

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue
@AndroidEntryPoint
class OutboundDetailActivity : AppCompatActivity() {

    private lateinit var mainDetailLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var tvFinalRemainingAmount: TextView
    private  var previousDebt: Double=0.0
    private  var paidAmount: String=""
    private val viewModel: OutboundViewModel by viewModels ()

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
        findViewById<Button>(R.id.btnExportExcel).setOnClickListener { exportToExcel() }
        findViewById<Button>(R.id.btnExportPdf).setOnClickListener { printInvoice() }
        val tableItems = findViewById<TableLayout>(R.id.tableDetailItems)

        // داخل onCreate في OutboundDetailActivity
        val outboundId = intent.getStringExtra("OUTBOUND_ID")
        val invoiceNum = intent.getStringExtra("INVOICE_NUM")
        val customerName = intent.getStringExtra("CUSTOMER_NAME") // تطابق مع المفتاح الجديد
        val date = intent.getStringExtra("DATE")
         previousDebt = intent.getDoubleExtra("previousDebt", 0.0)

// تعريف الـ Views الجديدة (تأكد من وجود هذه الـ IDs في ملف XML الذي عدلناه سابقاً)
        val tvPreviousDebt = findViewById<TextView>(R.id.tvPreviousDebt)
         tvFinalRemainingAmount = findViewById<TextView>(R.id.tvFinalRemainingAmount)

// تعيين القيم للواجهة
        tvPreviousDebt.text = String.format("%.2f ج.م", previousDebt)
       paidAmount = intent.getStringExtra("PAID_AMOUNT") ?: "0"

// تعيين البيانات للواجهة
        tvCustomer.text = "العميل: $customerName"
        tvInvoiceNum.text = "رقم الفاتورة: $invoiceNum"
        tvDate.text = "التاريخ: ${date?.substringBefore(" ")}"
        tvPaidAmount.text = "$paidAmount ج.م"


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
                val totalInvoiceAmount = currentDetailsList.sumOf { it.quantity * it.price }
                val paidAmount = intent.getStringExtra("PAID_AMOUNT")?.toDoubleOrNull() ?: 0.0
                val prevDebt = intent.getDoubleExtra("previousDebt", 0.0)
                val finalRemaining = intent.getDoubleExtra("totalRemainder", 0.0)

                textPaint.isFakeBoldText = true
// رسم البيانات
                canvas.drawText("إجمالي الفاتورة الحالية: $totalInvoiceAmount ج.م", rightMargin, currentY, textPaint)
                canvas.drawText("رصيد سابق على العميل: $prevDebt ج.م", rightMargin, currentY + 25f, textPaint)
                canvas.drawText("المبلغ المدفوع الآن: $paidAmount ج.م", rightMargin, currentY + 50f, textPaint)

                paint.color = android.graphics.Color.RED
                paint.strokeWidth = 2f
                canvas.drawLine(40f, currentY + 65f, 550f, currentY + 65f, paint) // خط تمييز للمتبقي النهائي

                canvas.drawText("إجمالي المتبقي النهائي: ${prevDebt+totalInvoiceAmount-prevDebt }} ج.م", rightMargin, currentY + 90f, textPaint)
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

    @SuppressLint("SuspiciousIndentation")
    private fun loadFullData(outboundId: String) {

        val table = findViewById<TableLayout>(R.id.tableDetailItems)


            // جمع البيانات من الـ Flow
            viewModel
               .loadInvoiceDetails(outboundId)
            viewModel.invoiceDetails.observe(this@OutboundDetailActivity) { detailsList ->
                currentDetailsList = detailsList
                detailsList.forEach { it->


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
                        tvFinalRemainingAmount.text=(totalInvoice+previousDebt-paidAmount.toDouble()).toString()




                    // الانتقال للخيط الرئيسي لتحديث الواجهة
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