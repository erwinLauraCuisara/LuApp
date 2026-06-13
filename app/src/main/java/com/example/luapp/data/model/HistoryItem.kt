package com.example.luapp.data.model

data class HistoryItem(
    val id: Long,
    val openedAt: Long,
    val closedAt: Long,
    val consumptionTotal: Double,
    val expenseTotal: Double,
    val debtTotal: Double
)
