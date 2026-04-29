package com.dobi.app.di

import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.data.repository.McqGenerationRepositoryImpl
import com.dobi.app.data.repository.WriteAnswerRepository
import com.dobi.app.data.repository.WriteAnswerRepositoryImpl
import com.dobi.app.data.repository.UserRepositoryImpl
import com.dobi.app.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindMcqGenerationRepository(
        impl: McqGenerationRepositoryImpl
    ): McqGenerationRepository

    @Binds
    @Singleton
    abstract fun bindWriteAnswerRepository(
        impl: WriteAnswerRepositoryImpl
    ): WriteAnswerRepository
}
