package com.dobi.app.di

import com.dobi.app.data.repository.ConceptRepositoryImpl
import com.dobi.app.domain.repository.ConceptRepository
import com.dobi.app.domain.usecase.GetConceptByIdUseCase
import com.dobi.app.domain.usecase.GetConceptsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConceptModule {

    @Provides @Singleton
    fun provideConceptRepository(
        firestore: dev.gitlive.firebase.firestore.FirebaseFirestore
    ): ConceptRepository = ConceptRepositoryImpl(firestore)

    @Provides @Singleton
    fun provideGetConceptsUseCase(
        repository: ConceptRepository
    ): GetConceptsUseCase = GetConceptsUseCase(repository)

    @Provides @Singleton
    fun provideGetConceptByIdUseCase(
        repository: ConceptRepository
    ): GetConceptByIdUseCase = GetConceptByIdUseCase(repository)
}
