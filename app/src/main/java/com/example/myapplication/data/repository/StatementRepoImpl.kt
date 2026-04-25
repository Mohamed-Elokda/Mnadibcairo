package com.example.myapplication.data.repository

import com.example.myapplication.data.local.dao.*
import com.example.myapplication.domin.model.StatementTransaction
import com.example.myapplication.domin.repository.StatementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class StatementRepoImpl @Inject constructor(
    private val outboundDao: OutboundDao,
    private val paymentDao: PaymentDao,
    private val returnedDao: ReturnedDao,
    private val customerDao: CustomerDao,

    private val itemsDao: ItemsDao,
) : StatementRepository {
    override fun getCustomerStatement(customerId: Int): Flow<List<StatementTransaction>> {

        return combine(

            customerDao.getCustomerById(customerId), // 1. جلب بيانات العميل (لازم تضيف الـ Flow ده)

            outboundDao.getOutboundsByCustomer(customerId),

            paymentDao.getPaymentsByCustomer(customerId),

            returnedDao.getReturnsByCustomer(customerId),

            itemsDao.getAllItems()

        ) { customer, outbounds, payments, returns, items ->



            val itemsMap = items.associateBy { it.id }

            val detailedList = mutableListOf<StatementTransaction>()



// ==========================================

// 1. إضافة الرصيد الافتتاحي كأول عملية

// ==========================================

            if (customer != null) {

                detailedList.add(StatementTransaction(

                    date = "0000-00-00", // تاريخ وهمي قديم جداً ليظهر في البداية

                    description = "رصيد افتتاحي",

                    itemName = "رصيد سابق",

                    quantity = 0,

                    amountIn = customer.firstCustomerDebt, // المبلغ الافتتاحي مدين (+)

                    amountOut = 0.0

                ))

            }



// 2. معالجة المبيعات (نفس الكود بتاعك)

            outbounds.forEach { outboundWithDetails ->

                val header = outboundWithDetails.outbound

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

                if (header.moneyResive > 0) {

                    detailedList.add(StatementTransaction(

                        date = header.outboundDate,

                        description = "دفع مقدم - فاتورة #${header.invorseNumber}",

                        itemName = "نقدي (لحظة البيع)",

                        quantity = 0,

                        amountIn = 0.0,

                        amountOut = header.moneyResive.toDouble()

                    ))

                }

            }



// 3. معالجة المرتجعات (نفس الكود بتاعك)

            returns.forEach { returnWithDetails ->

                val header = returnWithDetails.returned

                returnWithDetails.details.forEach { detail ->

                    detailedList.add(StatementTransaction(

                        date = header.returnedDate,

                        description = "مرتجع أصناف",

                        itemName = itemsMap[detail.itemId]?.itemName ?: "صنف غير معروف",

                        quantity = detail.amount,

                        amountIn = 0.0,

                        amountOut = detail.amount * detail.price

                    ))

                }

            }



// 4. معالجة التحصيلات المالية (نفس الكود بتاعك)

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



// 5. الترتيب وحساب الرصيد التراكمي

// الرصيد الافتتاحي هيطلع الأول بسبب تاريخه (0000-00-00)

            val sortedList = detailedList.sortedWith(compareBy({ it.date }, { it.description != "رصيد افتتاحي" }))



            var currentBalance = 0.0

            sortedList.forEach { transaction ->

                currentBalance += (transaction.amountIn - transaction.amountOut)

                transaction.runningBalance = currentBalance

            }



            sortedList

        }

    }}
