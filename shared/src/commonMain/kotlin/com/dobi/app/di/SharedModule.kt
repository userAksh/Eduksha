package com.dobi.app.di

import com.dobi.app.data.repository.ChildRepositoryImpl
import com.dobi.app.data.repository.CompletionRepositoryImpl
import com.dobi.app.data.repository.ConceptRepositoryImpl
import com.dobi.app.data.repository.DailyNewsRepositoryImpl
import com.dobi.app.data.repository.DeleteAccountRepositoryImpl
import com.dobi.app.data.repository.FriendSnapsRepositoryImpl
import com.dobi.app.data.repository.PairingRepositoryImpl
import com.dobi.app.domain.repository.ChildRepository
import com.dobi.app.domain.repository.CompletionRepository
import com.dobi.app.domain.repository.ConceptRepository
import com.dobi.app.domain.repository.DailyNewsRepository
import com.dobi.app.domain.repository.DeleteAccountRepository
import com.dobi.app.domain.repository.FriendSnapsRepository
import com.dobi.app.domain.repository.PairingRepository
import com.dobi.app.domain.usecase.GetConceptByIdUseCase
import com.dobi.app.domain.usecase.GetConceptsUseCase
import com.dobi.app.domain.usecase.SaveCompletionUseCase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import org.koin.dsl.module

val firebaseModule = module {
    single { Firebase.firestore }
    single { Firebase.functions }
}

val repositoryModule = module {
    single<ChildRepository> { ChildRepositoryImpl(get()) }
    single<ConceptRepository> { ConceptRepositoryImpl(get()) }
    single<PairingRepository> { PairingRepositoryImpl(get()) }
    single<CompletionRepository> { CompletionRepositoryImpl(get()) }
    single<DailyNewsRepository> { DailyNewsRepositoryImpl(get()) }
    single<DeleteAccountRepository> { DeleteAccountRepositoryImpl(get()) }
    single<FriendSnapsRepository> { FriendSnapsRepositoryImpl() }
}

val useCaseModule = module {
    factory { GetConceptsUseCase(get()) }
    factory { GetConceptByIdUseCase(get()) }
    factory { SaveCompletionUseCase(get()) }
}

/** All shared Koin modules. Platform apps pass these to [startKoin]. */
val sharedModules = listOf(firebaseModule, repositoryModule, useCaseModule)
