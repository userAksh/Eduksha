package com.dobi.app.di

import com.dobi.app.data.repository.HomeworkRepositoryImpl
import com.dobi.app.domain.repository.HomeworkRepository
import com.dobi.app.domain.usecase.SaveHomeworkUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeworkModule {

    @Binds @Singleton
    abstract fun bindHomeworkRepository(impl: HomeworkRepositoryImpl): HomeworkRepository

    companion object {
        @Provides @Singleton
        fun provideSaveHomeworkUseCase(
            repository: HomeworkRepository
        ): SaveHomeworkUseCase = SaveHomeworkUseCase(repository)
    }
}
