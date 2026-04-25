package com.example.myapplication.data.di

import com.example.myapplication.data.repository.CustomerRepoImpl
import com.example.myapplication.data.repository.InboundRepositoryImpl
import com.example.myapplication.data.repository.OutboundRepoImpl
import com.example.myapplication.data.repository.PaymentRepositoryImpl
import com.example.myapplication.data.repository.ReturnedRepoImpl
import com.example.myapplication.data.repository.StatementRepoImpl
import com.example.myapplication.data.repository.StockRepoImpl
import com.example.myapplication.data.repository.TransferRepositoryImpl
import com.example.myapplication.data.repository.VaultRepositoryImpl
import com.example.myapplication.domin.repository.CustomerRepo
import com.example.myapplication.domin.repository.IInboundRepository
import com.example.myapplication.domin.repository.IStockRepository
import com.example.myapplication.domin.repository.ITransferRepository
import com.example.myapplication.domin.repository.OutboundRepo
import com.example.myapplication.domin.repository.PaymentRepository
import com.example.myapplication.domin.repository.ReturnedRepo
import com.example.myapplication.domin.repository.StatementRepository
import com.example.myapplication.domin.repository.StockRepository
import com.example.myapplication.domin.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
public abstract class RepoModule {

    @Binds
    @Singleton
    abstract fun bindCustomerRepo(
        customerRepoImpl: CustomerRepoImpl
    ): CustomerRepo

    @Binds
    @Singleton
    abstract fun bindInboundRepository(
        inboundRepoImpl: InboundRepositoryImpl
    ): IInboundRepository

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        stockRepoImpl: StockRepoImpl
    ): StockRepository

    @Binds
    @Singleton
    abstract fun bindTransferRepository(
        transferRepository: TransferRepositoryImpl
    ): ITransferRepository

    @Binds
    @Singleton
    abstract fun bindOutboundRepository(
        outboundRepoImpl: OutboundRepoImpl
    ): OutboundRepo

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepository: PaymentRepositoryImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindReturnedRepo(
        returnedRepo: ReturnedRepoImpl
    ): ReturnedRepo

    @Binds
    @Singleton
    abstract fun bindStatementRepository(
        statementRepository: StatementRepoImpl
    ): StatementRepository

    @Binds
    @Singleton
    abstract fun bindVaultRepository(
        vaultRepository: VaultRepositoryImpl
    ): VaultRepository


}