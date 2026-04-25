package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.model.ItemMovement
import com.example.myapplication.domin.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetItemMovementUseCase @Inject constructor(private val repository: StockRepository) {
    operator fun invoke(itemId: Int): Flow<List<ItemMovement>> {
        return repository.getItemMovement(itemId)
    }
}