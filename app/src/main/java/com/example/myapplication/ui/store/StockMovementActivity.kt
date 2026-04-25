package com.example.myapplication.ui.store

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
@AndroidEntryPoint
class StockMovementActivity : AppCompatActivity() {

    // تعريف الـ ViewModel (يفترض وجود DI أو Factory)
    // سأستخدم التوصيف البسيط هنا، تأكد من إعداد الـ ViewModelProvider.Factory إذا لم تكن تستخدم Hilt
    private val viewModel: StockMovementViewModel by viewModels ()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stock_movement)

        // إعداد الحواف (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. استقبال البيانات من الـ Intent
        val itemId = intent.getIntExtra("ITEM_ID", -1)
        val itemName = intent.getStringExtra("ITEM_NAME") ?: "صنف غير معروف"

        // 2. ربط عناصر الواجهة الأساسية
        val tvItemName = findViewById<TextView>(R.id.tvItemName)
        val tvCurrentStock = findViewById<TextView>(R.id.tvCurrentStock)
        val rvMovement = findViewById<RecyclerView>(R.id.rvMovement)

        tvItemName.text = itemName

        // 3. إعداد الـ Adapter
        val adapter = MovementAdapter()
        rvMovement.adapter = adapter

        // 4. طلب البيانات من الـ ViewModel
        if (itemId != -1) {
            viewModel.loadMovement(itemId)
        }

        // 5. مراقبة البيانات (Observation) باستخدام Flow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.movementState.collect { movements ->
                    adapter.submitList(movements)

                    // تحديث إجمالي الرصيد في الكارت العلوي من آخر حركة
                    if (movements.isNotEmpty()) {
                        val lastStock = movements.last().runningStock
                        tvCurrentStock.text = "الرصيد الحالي بالمخزن: $lastStock"
                    } else {
                        tvCurrentStock.text = "لا توجد حركات لهذا الصنف"
                    }
                }
            }
        }
    }
}