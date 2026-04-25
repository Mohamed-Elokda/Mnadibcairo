package com.example.myapplication.domin.useCase

import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.repository.ReturnedRepo
import javax.inject.Inject

class DeleteReturnedUsecase @Inject constructor(private val returnedRepo: ReturnedRepo) {
    suspend operator fun invoke(returned: ReturnedWithNameModel){
        returnedRepo.deleteReturned(returned)
    }
}