package com.example.myapplication.domin.useCase

import com.example.myapplication.data.local.entity.ItemHistoryProjection
import com.example.myapplication.domin.model.ItemHistoryProjectionModel
import com.example.myapplication.domin.repository.ReturnedRepo

import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetItemHistoryUseCase @Inject constructor(private val repository: ReturnedRepo) {
    operator fun invoke(customerId: Int, itemId: Int): Flow<List<ItemHistoryProjectionModel>> {
        return repository.getItemPurchaseHistory(customerId, itemId).map { it->
            it.map { item->
                ItemHistoryProjectionModel (
                    date=item.date,
                    price=item.price,
                    amount=item.amount

                )
            }

        }
    }
}