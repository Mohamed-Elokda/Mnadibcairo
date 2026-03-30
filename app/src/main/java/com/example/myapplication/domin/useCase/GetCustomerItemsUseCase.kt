package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.repository.ReturnedRepo

class GetCustomerItemsUseCase(private val repository: ReturnedRepo) {
    operator fun invoke(customerId: Int) = repository.getItemsByCustomer(customerId)
}