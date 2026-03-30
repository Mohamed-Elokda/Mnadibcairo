package com.example.myapplication.domin.repository

import com.example.myapplication.domin.model.Customer
import kotlinx.coroutines.flow.Flow


interface CustomerRepo {
    suspend fun insertCustomer(customer: Customer):Int
    suspend fun updateCustomer(customer: Customer)
    suspend fun deleteCustomer(customer: Customer)
    suspend fun getCustomerById(id: Int): Customer?
     fun getAllCustomers(userId: String): Flow<List<Customer>>
    suspend fun syncCustomersFromServer(userId: String)

}