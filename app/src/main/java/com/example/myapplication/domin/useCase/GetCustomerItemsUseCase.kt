package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.repository.ReturnedRepo
import javax.inject.Inject

class GetCustomerItemsUseCase @Inject constructor(private val repository: ReturnedRepo) {
    operator fun invoke() = repository.getItemsByCustomer()
}