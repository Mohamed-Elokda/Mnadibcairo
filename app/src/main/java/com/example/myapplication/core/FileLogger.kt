import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val FOLDER_NAME = "Caro_Logs" // اسم المجلد في مدير الملفات
    private const val FILE_NAME = "sync_errors.txt"

    fun logError(message: String, throwable: Throwable? = null) {
        try {
            // 1. الوصول لمجلد المستندات العام (Documents)
            val publicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val caroFolder = File(publicDirectory, FOLDER_NAME)

            // 2. إنشاء المجلد إذا لم يكن موجوداً
            if (!caroFolder.exists()) {
                caroFolder.mkdirs()
            }

            val logFile = File(caroFolder, FILE_NAME)

            // 3. كتابة الخطأ
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val writer = FileWriter(logFile, true)
            val printWriter = PrintWriter(writer)

            printWriter.println("==========================================")
            printWriter.println("Timestamp: $timestamp")
            printWriter.println("Error Message: $message")

            throwable?.let {
                printWriter.println("Exception: ${it.localizedMessage}")
                it.printStackTrace(printWriter)
            }

            printWriter.println("==========================================")
            printWriter.println() // سطر فارغ للترتيب

            printWriter.flush()
            printWriter.close()

            Log.d("FileLogger", "Error saved to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("FileLogger", "Failed to write to file: ${e.message}")
        }
    }
}