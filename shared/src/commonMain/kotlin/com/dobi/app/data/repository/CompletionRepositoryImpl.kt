package com.dobi.app.data.repository

import com.dobi.app.domain.model.Completion
import com.dobi.app.domain.repository.CompletionRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.datetime.Clock

class CompletionRepositoryImpl(
    private val firestore: FirebaseFirestore
) : CompletionRepository {

    override suspend fun saveCompletion(completion: Completion) {
        firestore.collection("completions").add(
            mapOf(
                "childId" to completion.childId,
                "conceptId" to completion.conceptId,
                "effort" to completion.effort.name,
                "mode" to completion.mode.name,
                "timestamp" to Clock.System.now().toEpochMilliseconds()
            )
        )
    }
}
