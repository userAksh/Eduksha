package com.dobi.app.domain.model

data class McqQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String = ""
)
