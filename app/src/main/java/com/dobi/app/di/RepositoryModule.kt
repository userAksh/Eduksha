package com.dobi.app.di

import com.dobi.app.data.repository.DailyNewsRepositoryImpl
import com.dobi.app.data.repository.DeleteAccountRepositoryImpl
import com.dobi.app.data.repository.FcmTokenRepositoryImpl
import com.dobi.app.data.repository.FriendSnapsRepositoryImpl
import com.dobi.app.data.repository.InviteRepositoryImpl
import com.dobi.app.data.repository.McqGenerationRepository
import com.dobi.app.data.repository.McqGenerationRepositoryImpl
import com.dobi.app.data.repository.ParentTaskRepositoryImpl
import com.dobi.app.data.repository.StreakShareRepositoryImpl
import com.dobi.app.data.repository.WriteAnswerRepository
import com.dobi.app.data.repository.WriteAnswerRepositoryImpl
import com.dobi.app.data.repository.UserRepositoryImpl
import com.dobi.app.domain.repository.DailyNewsRepository
import com.dobi.app.domain.repository.DeleteAccountRepository
import com.dobi.app.domain.repository.FcmTokenRepository
import com.dobi.app.domain.repository.FriendSnapsRepository
import com.dobi.app.domain.repository.InviteRepository
import com.dobi.app.domain.repository.ParentTaskRepository
import com.dobi.app.domain.repository.StreakShareRepository
import com.dobi.app.domain.repository.UserRepository
import com.dobi.app.domain.usecase.SelectRoleUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindMcqGenerationRepository(impl: McqGenerationRepositoryImpl): McqGenerationRepository

    @Binds @Singleton
    abstract fun bindWriteAnswerRepository(impl: WriteAnswerRepositoryImpl): WriteAnswerRepository

    @Binds @Singleton
    abstract fun bindStreakShareRepository(impl: StreakShareRepositoryImpl): StreakShareRepository

    @Binds @Singleton
    abstract fun bindParentTaskRepository(impl: ParentTaskRepositoryImpl): ParentTaskRepository

    @Binds @Singleton
    abstract fun bindFcmTokenRepository(impl: FcmTokenRepositoryImpl): FcmTokenRepository

    @Binds @Singleton
    abstract fun bindInviteRepository(impl: InviteRepositoryImpl): InviteRepository

    companion object {
        @Provides @Singleton
        fun provideFriendSnapsRepository(): FriendSnapsRepository = FriendSnapsRepositoryImpl()

        @Provides @Singleton
        fun provideDailyNewsRepository(
            functions: com.google.firebase.functions.FirebaseFunctions
        ): DailyNewsRepository = DailyNewsRepositoryImpl(functions)

        @Provides @Singleton
        fun provideDeleteAccountRepository(
            functions: dev.gitlive.firebase.functions.FirebaseFunctions
        ): DeleteAccountRepository = DeleteAccountRepositoryImpl(functions)

        @Provides @Singleton
        fun provideSelectRoleUseCase(
            repository: UserRepository
        ): SelectRoleUseCase = SelectRoleUseCase(repository)
    }
}
