package com.example.luapp.data.model

data class ConsumptionItem(
    val id: Long,
    val concept: String,
    val customerName: String?,
    val buddyIds: List<Long> = emptyList(),
    val buddyNames: List<String> = emptyList(),
    val amount: Double,        // monto efectivo
    val amountQr: Double = 0.0,
    val appointmentFee: Double,
    val pendingAmount: Double?,
    val details: String?,
    val createdAt: Long
)
