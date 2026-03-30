package com.example.myapplication.ui.inbound

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.myapplication.R
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.InboundDetailWithItemName

import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.domin.model.InboundDetails
import com.example.myapplication.domin.useCase.AddInboundUseCase
import com.example.myapplication.domin.useCase.GetInboundDetailsUseCase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch



class InboundDetailActivity : AppCompatActivity() {

    private var remoteImageUrl: String? = null
    private var currentInvoiceNum: String = "" // لتخزين رَقَم الفاتورة
    private lateinit var btnPrint: Button
    private lateinit var btnPDF: Button
    private lateinit var btnExcel: Button
    // إعداد الـ ViewModel مع الـ UseCase الجديد
    private val viewModel: InboundViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = InboundRepositoryImpl(
            database.inboundDao(),
            database.inboundDetailesDao(),
            database.stockDao(),
            database.suppliedDao(),
            database.itemsDao()
        )
        // إضافة الـ UseCase الجديد للـ Factory
        InboundViewModelFactory(
            AddInboundUseCase(repository),
            GetInboundDetailsUseCase(repository),
            repository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbound_detail)
        btnPrint = findViewById(R.id.btnPrint)
        btnPDF = findViewById(R.id.btnExportPDF)
        btnExcel = findViewById(R.id.btnExportExcel)
        val invoiceNum = intent.getStringExtra("INVOICE_NUM") ?: "N/A"
        currentInvoiceNum = invoiceNum // حفظ القيمة هنا للاستخدام في الدوال الأخرى
// في onCreate أضف المستمعات:
        btnPrint.setOnClickListener { printInvoice() }
        btnPDF.setOnClickListener { exportToPDF() }
        btnExcel.setOnClickListener { exportToExcel() }
        // تعريف عناصر الواجهة
        val tvSupplier = findViewById<TextView>(R.id.tvDetailSupplier)
        val tvInvoiceNum = findViewById<TextView>(R.id.tvDetailInvoiceNum)
        val tvDate = findViewById<TextView>(R.id.tvDetailDate)

        val tableItems = findViewById<TableLayout>(R.id.tableDetailItems)
        val btnViewPhoto = findViewById<Button>(R.id.btnViewPhoto)

        // 1. استقبال البيانات من الـ Intent
        val inboundId = intent.getLongExtra("INBOUND_ID", -1L)
        val supplier = intent.getStringExtra("SUPPLIER") ?: "غير معروف"
        val date = intent.getStringExtra("DATE")
        remoteImageUrl = intent.getStringExtra("IMAGE_URL")

        val total = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0)
        val paid = intent.getDoubleExtra("PAID_AMOUNT", 0.0)

        // 2. تعبئة البيانات المالية والأساسية
        tvSupplier.text = "المورد: $supplier"
        tvInvoiceNum.text = "رقم الفاتورة: $invoiceNum"
        tvDate.text = "التاريخ: ${date?.substringBefore("T") ?: "N/A"}"

        // تنسيق العملة (ج.م)


        // 3. مراقبة تفاصيل الفاتورة من قاعدة البيانات
        if (inboundId != -1L) {
            viewModel.getInboundDetails(inboundId).observe(this) { detailsList ->
                displayDetailsInTable(detailsList, tableItems)
            }
        } else {
            Toast.makeText(this, "خطأ في تحميل رقم الفاتورة", Toast.LENGTH_SHORT).show()
        }

        btnViewPhoto.setOnClickListener {
            if (!remoteImageUrl.isNullOrEmpty()) {
                showImageDialog(remoteImageUrl!!)
            } else {
                Toast.makeText(this, "لا توجد صورة لهذه الفاتورة", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun printInvoice() {
        // 1. جلب البيانات من الواجهة
        val supplierName = intent.getStringExtra("SUPPLIER") ?: "غير معروف"
        val invoiceDate = intent.getStringExtra("DATE") ?: "N/A"
        val table = findViewById<TableLayout>(R.id.tableDetailItems)

        // 2. إنشاء عرض مؤقت (LinearLayout) في الذاكرة ليحتوي على شكل الفاتورة كاملة
        val printContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(40, 40, 40, 40)
            layoutParams = android.view.ViewGroup.LayoutParams(
                table.width + 80,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 3. إضافة النصوص العلوية (Header)
        val tvTitle = createPrintTextView("فاتورة مشتريات (وارد)", 28f, true)
        val tvInfo = createPrintTextView(
            "رقم الفاتورة: $currentInvoiceNum\nالمورد: $supplierName\nالتاريخ: $invoiceDate",
            18f,
            false
        ).apply {
            setPadding(0, 20, 0, 40)
            gravity = android.view.Gravity.RIGHT
        }

        printContainer.addView(tvTitle)
        printContainer.addView(tvInfo)

        // 4. أخذ نسخة من الجدول الأصلي وإضافتها للحاوية
        // ملاحظة: لا يمكن إضافة نفس الـ View لمرتين، لذا سنرسم الجدول يدوياً أو نستخدم نسخة Bitmap منه
        val tableBitmap = createBitmapFromView(table)
        val ivTable = android.widget.ImageView(this).apply {
            setImageBitmap(tableBitmap)
            adjustViewBounds = true
        }
        printContainer.addView(ivTable)

        // 5. تحويل الحاوية الكاملة إلى Bitmap للطباعة
        printContainer.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(printContainer.layoutParams.width, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        printContainer.layout(0, 0, printContainer.measuredWidth, printContainer.measuredHeight)

        val finalBitmap = createBitmapFromView(printContainer)

        // 6. إرسال الصورة النهائية للطابعة
        val printHelper = androidx.print.PrintHelper(this)
        printHelper.scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
        printHelper.printBitmap("${getString(R.string.app_name)} - $currentInvoiceNum", finalBitmap)
    }

    // دالة مساعدة لإنشاء نصوص الطباعة بتنسيق موحد
    private fun createPrintTextView(text: String, size: Float, isBold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = size
            this.setTextColor(android.graphics.Color.BLACK)
            this.gravity = Gravity.CENTER
            if (isBold) this.setTypeface(null, android.graphics.Typeface.BOLD)
        }
    }
    private fun exportToExcel() {
        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("بيانات الوارد")

        // 1. إنشاء تنسيق لرأس الجدول (Header Style)
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = org.apache.poi.ss.usermodel.IndexedColors.CORNFLOWER_BLUE.index
            fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
            alignment = org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER
            borderBottom = org.apache.poi.ss.usermodel.BorderStyle.THIN
            val font = workbook.createFont().apply {
                color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
            }
            setFont(font)
        }

        // 2. إنشاء صف العناوين
        val headerRow = sheet.createRow(0)
        val columns = arrayOf("رقم الفاتورة", "اسم المورد", "التاريخ", "الصنف", "الكمية")

        for (i in columns.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(columns[i])
            cell.cellStyle = headerStyle
        }

        // 3. جلب البيانات وتعبئتها
        val table = findViewById<TableLayout>(R.id.tableDetailItems)
        val supplierName = intent.getStringExtra("SUPPLIER") ?: "غير معروف"
        val invoiceDate = intent.getStringExtra("DATE") ?: "N/A"

        var excelRowIndex = 1

        for (i in 1 until table.childCount) {
            val tableRow = table.getChildAt(i) as TableRow

            if (tableRow.childCount >= 2) {
                val excelRow = sheet.createRow(excelRowIndex++)

                val itemName = (tableRow.getChildAt(0) as TextView).text.toString()
                val itemQty = (tableRow.getChildAt(1) as TextView).text.toString()

                // تم التعديل هنا: استخدام setCellValue كـ String لتجنب خطأ التحويل
                excelRow.createCell(0).setCellValue(currentInvoiceNum)
                excelRow.createCell(1).setCellValue(supplierName)
                excelRow.createCell(2).setCellValue(invoiceDate)
                excelRow.createCell(3).setCellValue(itemName)
                excelRow.createCell(4).setCellValue(itemQty.toDoubleOrNull() ?: 0.0)
            }
        }

        // 4. ضبط عرض الأعمدة يدوياً (الحل البديل لـ autoSizeColumn لتجنب الانهيار)
        // العرض = (عدد الحروف التقريبي * 256)
        sheet.setColumnWidth(0, 15 * 256) // رقم الفاتورة
        sheet.setColumnWidth(1, 20 * 256) // المورد
        sheet.setColumnWidth(2, 20 * 256) // التاريخ
        sheet.setColumnWidth(3, 30 * 256) // الصنف
        sheet.setColumnWidth(4, 10 * 256) // الكمية

        // 5. حفظ الملف وفتحه
        val fileName = "تقرير_وارد_$currentInvoiceNum.xlsx"
        val file = java.io.File(getExternalFilesDir(null), fileName)

        try {
            val out = java.io.FileOutputStream(file)
            workbook.write(out)
            out.close()
            workbook.close()

            Toast.makeText(this, "تم استخراج الإكسيل بنجاح", Toast.LENGTH_SHORT).show()

            // فتح الملف تلقائياً
            openExcelFile(file)

        } catch (e: Exception) {
            android.util.Log.e("ExcelError", "Error: ${e.message}")
            Toast.makeText(this, "خطأ في التصدير: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun openExcelFile(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            // تحديد الـ MimeType لملفات Excel الحديثة
            intent.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY)

            startActivity(android.content.Intent.createChooser(intent, "فتح الفاتورة بواسطة:"))
        } catch (e: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق لفتح ملفات Excel", Toast.LENGTH_LONG).show()
        }
    }
    private fun exportToPDF() {
        val table = findViewById<TableLayout>(R.id.tableDetailItems)
        val pdfDocument = android.graphics.pdf.PdfDocument()

        // إعدادات الصفحة (أكبر قليلاً من الجدول لتسيع البيانات العلوية)
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
            table.width + 100,
            table.height + 400, // زيادة الطول لرأس الفاتورة
            1
        ).create()

        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        // 1. رسم عنوان الفاتورة
        paint.textSize = 30f
        paint.isFakeBoldText = true
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("فاتورة مشتريات (وارد)", (pageInfo.pageWidth / 2).toFloat(), 60f, paint)

        // 2. رسم البيانات الأساسية (اسم المورد، الرقم، التاريخ)
        paint.textSize = 20f
        paint.isFakeBoldText = false
        paint.textAlign = android.graphics.Paint.Align.RIGHT

        val startX = (pageInfo.pageWidth - 50).toFloat()
        canvas.drawText("رقم الفاتورة: $currentInvoiceNum", startX, 120f, paint)
        canvas.drawText("اسم المورد: ${intent.getStringExtra("SUPPLIER")}", startX, 160f, paint)
        canvas.drawText("التاريخ: ${intent.getStringExtra("DATE")}", startX, 200f, paint)

        // 3. رسم خط فاصل
        paint.strokeWidth = 2f
        canvas.drawLine(50f, 230f, (pageInfo.pageWidth - 50).toFloat(), 230f, paint)

        // 4. رسم جدول الأصناف
        // نقوم بنقل "نقطة الرسم" إلى ما بعد البيانات العلوية
        canvas.save()
        canvas.translate(50f, 260f)
        table.draw(canvas)
        canvas.restore()

        pdfDocument.finishPage(page)

        // حفظ الملف
        val fileName = "فاتورة_$currentInvoiceNum.pdf"
        val file = java.io.File(getExternalFilesDir(null), fileName)

        try {
            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()
            Toast.makeText(this, "تم حفظ الفاتورة بنجاح", Toast.LENGTH_SHORT).show()

            // فتح الملف تلقائياً (استخدم الدالة التي شرحناها سابقاً)
            openPdfFile(file)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun openPdfFile(file: java.io.File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // لضمان فتح الملف في تطبيق خارجي بشكل نظيف
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY)

            startActivity(android.content.Intent.createChooser(intent, "فتح الفاتورة بواسطة:"))
        } catch (e: Exception) {
            Toast.makeText(this, "لا يوجد تطبيق لعرض PDF", Toast.LENGTH_LONG).show()
        }
    }    private fun createBitmapFromView(view: android.view.View): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
    private fun displayDetailsInTable(details: List<InboundDetailWithItemName>, table: TableLayout) {
        // تنظيف الجدول مع الحفاظ على صف العنوان (الهيدر)
        if (table.childCount > 1) {
            table.removeViews(1, table.childCount - 1)
        }

        if (details.isEmpty()) {
            val emptyRow = TableRow(this)
            emptyRow.addView(createCell("لا توجد أصناف مسجلة"))
            table.addView(emptyRow)
            return
        }

        for (item in details) {
            val row = TableRow(this).apply {
                setPadding(0, 12, 0, 12)
            }

            // إضافة الأعمدة: (الاسم، الكمية، السعر، الإجمالي)
            row.addView(createCell(item.itemName)) // عرض الاسم بدلاً من الـ ID
            row.addView(createCell(item.quantity.toString()))


            table.addView(row)
        }
    }

    private fun createCell(text: String, isBold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            this.gravity = Gravity.CENTER
            this.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            this.setPadding(8, 8, 8, 8)
            if (isBold) {
                setTypeface(null, Typeface.BOLD)
                setTextColor(resources.getColor(android.R.color.black, null))
            }
        }
    }

    private fun showImageDialog(imageUrl: String) {
        val builder = AlertDialog.Builder(this)
        val imageView = ImageView(this).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            load(imageUrl) {
                placeholder(android.R.drawable.progress_indeterminate_horizontal)
                error(android.R.drawable.stat_notify_error)
            }
        }
        builder.setView(imageView)
        builder.setPositiveButton("إغلاق") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }
}