package com.example.myapplication.domin.useCase

import com.example.myapplication.domin.repository.StatementRepository
import javax.inject.Inject

class ReconcileAllCustomersDebt @Inject constructor(private val statementRepo: StatementRepository) {

    suspend operator fun invoke(){
        statementRepo.reconcileAllCustomersDebt()
    }
}