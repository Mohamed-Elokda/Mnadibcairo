package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.*
import com.example.myapplication.domin.model.StatementTransaction
import com.example.myapplication.domin.repository.StatementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StatementRepoImpl(
    private val outboundDao: OutboundDao,
    private val paymentDao: PaymentDao,
    private val returnedDao: ReturnedDao,
    private val outboundDetailsDao: OutboundDetailesDao,
    private val returnedDetailsDao: ReturnedDetailsDao,
    private val itemsDao: ItemsDao,
) : StatementRepository {

    override fun getCustomerStatement(customerId: Int): Flow<List<StatementTransaction>> {
        // نستخدم combine لدمج 4 تدفقات بيانات (المبيعات، المدفوعات، المرتجعات، والأصناف)
        return combine(
            outboundDao.getOutboundsByCustomer(customerId),
            paymentDao.getPaymentsByCustomer(customerId),
            returnedDao.getReturnsByCustomer(customerId),
            itemsDao.getAllItems() // جلب قائمة الأصناف لترجمة الـ ID إلى اسم
        ) { outbounds, payments, returns, items ->

            // تحويل قائمة الأصناف إلى Map للبحث السريع بواسطة الـ ID
            val itemsMap = items.associateBy { it.id }
            val detailedList = mutableListOf<StatementTransaction>()

            // 1. معالجة المبيعات (تفصيل كل صنف في الفاتورة)
// داخل StatementRepoImpl - قسم المبيعات
            outbounds.forEach { outboundWithDetails ->
                val header = outboundWithDetails.outbound

                // 1. إضافة أصناف الفاتورة (مدين +)
                outboundWithDetails.details.forEach { detail ->
                    detailedList.add(StatementTransaction(
                        date = header.outboundDate,
                        description = "فاتورة #${header.invorseNumber}",
                        itemName = itemsMap[detail.itemId]?.itemName ?: "صنف ${detail.itemId}",
                        quantity = detail.amount,
                        amountIn = detail.amount * detail.price,
                        amountOut = 0.0
                    ))
                }

                // 2. إضافة المبلغ المدفوع لحظة البيع (دائن -)
                // نستخدم حقل moneyResive من الجدول الخاص بك
                if (header.moneyResive > 0) {
                    detailedList.add(StatementTransaction(
                        date = header.outboundDate,
                        description = "دفع مقدم - فاتورة #${header.invorseNumber}",
                        itemName = "نقدي (لحظة البيع)",
                        quantity = 0,
                        amountIn = 0.0,
                        amountOut = header.moneyResive.toDouble() // تحويل Int إلى Double
                    ))
                }
            }
            // 2. معالجة المرتجعات (تفصيل كل صنف مرجع)
            returns.forEach { returnWithDetails ->
                val header = returnWithDetails.returned
                returnWithDetails.details.forEach { detail ->
                    detailedList.add(StatementTransaction(
                        date = header.returnedDate,
                        description = "مرتجع أصناف",
                        itemName = itemsMap[detail.itemId]?.itemName ?: "صنف غير معروف (${detail.itemId})",
                        quantity = detail.amount,
                        amountIn = 0.0,
                        amountOut = detail.amount * detail.price
                    ))
                }
            }

            // 3. معالجة التحصيلات المالية
            payments.forEach { pay ->
                detailedList.add(StatementTransaction(
                    date = pay.date,
                    description = "تحصيل نقدي",
                    itemName = "وسيلة الدفع: ${pay.paymentType}",
                    quantity = 0,
                    amountIn = 0.0,
                    amountOut = pay.amount
                ))
            }

            // 4. الترتيب الزمني وحساب الرصيد التراكمي
            // ملاحظة: الترتيب حسب التاريخ مهم جداً لحساب الرصيد بشكل صحيح
            val sortedList = detailedList.sortedBy { it.date }

            var currentBalance = 0.0
            sortedList.forEach { transaction ->
                currentBalance += (transaction.amountIn - transaction.amountOut)
                transaction.runningBalance = currentBalance
            }

            sortedList
        }
    }
}