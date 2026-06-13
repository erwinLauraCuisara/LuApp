package com.example.luapp.data.model

data class ConsumptionItem(
    val id: Long,
    val concept: String,
    val customerName: String?,
    val buddyId: Long?,
    val buddyName: String?,
    val amount: Double,
    val appointmentFee: Double,
    val pendingAmount: Double?,
    val details: String?,
    val createdAt: Long
)
