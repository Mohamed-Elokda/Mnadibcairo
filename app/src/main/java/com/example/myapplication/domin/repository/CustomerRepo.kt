package com.example.myapplication.domin.repository

import com.example.myapplication.domin.model.CustomerModel
import kotlinx.coroutines.flow.Flow


interface CustomerRepo {
    suspend fun insertCustomer(customerModel: CustomerModel):Int
    suspend fun updateCustomer(customerModel: CustomerModel)
    suspend fun deleteCustomer(customerModel: CustomerModel)
    suspend fun getCustomerById(id: Int): CustomerModel?
     fun getAllCustomers(userId: String): Flow<List<CustomerModel>>
    suspend fun syncCustomersFromServer(userId: String)

}