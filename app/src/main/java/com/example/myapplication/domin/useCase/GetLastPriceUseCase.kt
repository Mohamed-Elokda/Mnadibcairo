package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.repository.ReturnedRepo

class GetLastPriceUseCase(private val repository: ReturnedRepo) {
    suspend operator fun invoke(customerId: Int, itemId: Int) = repository.getLastPrice(customerId, itemId)
}