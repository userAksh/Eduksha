package com.dobi.app.domain.model

data class Child(
    val id: String = "",
    val name: String = "",
    val grade: Int = 1,
    val board: String = "CBSE",
    val parentId: String = "",
    val gender: String = "boy",
    val dateOfBirth: String = ""
)
