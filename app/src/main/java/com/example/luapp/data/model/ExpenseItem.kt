package com.example.luapp.data.model

data class ExpenseItem(
    val id: Long,
    val concept: String,
    val amount: Double,
    val details: String?,
    val createdAt: Long
)
