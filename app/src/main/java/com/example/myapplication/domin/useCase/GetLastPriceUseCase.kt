package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.repository.ReturnedRepo
import javax.inject.Inject

class GetLastPriceUseCase @Inject constructor(private val repository: ReturnedRepo) {
    suspend operator fun invoke(customerId: Int, itemId: Int) = repository.getLastPrice(customerId, itemId)
}