package com.dobi.app.di

import com.dobi.app.data.repository.CompletionRepositoryImpl
import com.dobi.app.domain.repository.CompletionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CompletionModule {

    @Provides @Singleton
    fun provideCompletionRepository(
        firestore: dev.gitlive.firebase.firestore.FirebaseFirestore
    ): CompletionRepository = CompletionRepositoryImpl(firestore)
}
