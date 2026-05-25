package com.dobi.app.di

import com.dobi.app.data.repository.PairingRepositoryImpl
import com.dobi.app.domain.repository.PairingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PairingModule {

    @Provides @Singleton
    fun providePairingRepository(
        firestore: dev.gitlive.firebase.firestore.FirebaseFirestore
    ): PairingRepository = PairingRepositoryImpl(firestore)
}
