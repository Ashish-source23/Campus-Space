package com.example.campusspace.entity

data class User(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)