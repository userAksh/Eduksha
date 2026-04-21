package com.dobi.app.di

import com.dobi.app.data.repository.ConceptRepositoryImpl
import com.dobi.app.domain.repository.ConceptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConceptModule {

    @Binds
    @Singleton
    abstract fun bindConceptRepository(impl: ConceptRepositoryImpl): ConceptRepository
}
