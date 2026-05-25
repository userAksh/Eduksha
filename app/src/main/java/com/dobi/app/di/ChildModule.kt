package com.dobi.app.di

import com.dobi.app.data.repository.ChildRepositoryImpl
import com.dobi.app.domain.repository.ChildRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChildModule {

    @Provides @Singleton
    fun provideChildRepository(
        firestore: dev.gitlive.firebase.firestore.FirebaseFirestore
    ): ChildRepository = ChildRepositoryImpl(firestore)
}
