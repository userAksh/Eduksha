package com.dobi.app.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Boots Koin with all shared modules plus any platform-specific modules.
 *
 * Android does not call this — it uses Hilt.
 * The iOS app calls this from its @main entry point:
 *
 *   KoinInitKt.doInitKoin(additionalModules: [iosModule])
 */
fun initKoin(additionalModules: List<Module> = emptyList()): KoinApplication =
    startKoin {
        modules(sharedModules + additionalModules)
    }
