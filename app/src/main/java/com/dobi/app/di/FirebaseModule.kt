package com.dobi.app.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.gitlive.firebase.Firebase as GitLiveFirebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import android.content.Context
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // Google Firebase SDK — used by Android-only repository implementations.
    @Provides @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        Firebase.analytics

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides @Singleton
    fun provideFirestore(): com.google.firebase.firestore.FirebaseFirestore =
        com.google.firebase.firestore.FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideStorage(): FirebaseStorage = Firebase.storage

    // GitLive Firebase KMP SDK — used by shared repository implementations.
    @Provides @Singleton
    fun provideGitLiveFirestore(): dev.gitlive.firebase.firestore.FirebaseFirestore =
        GitLiveFirebase.firestore

    @Provides @Singleton
    fun provideGitLiveFunctions(): dev.gitlive.firebase.functions.FirebaseFunctions =
        GitLiveFirebase.functions
}
