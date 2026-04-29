package com.dobi.app.domain.model

data class ParentTask(
    val id: String = "",
    val title: String,
    val description: String,
    val reward: String,
    val subject: String? = null,
    val dueLabel: String? = null,
    val status: ParentTaskStatus = ParentTaskStatus.PENDING
)

enum class ParentTaskStatus { PENDING, IN_PROGRESS, SUBMITTED, COMPLETED }
