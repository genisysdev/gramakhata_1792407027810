package com.gramakhata.app.data.db.entity

/**
 * Data class to hold a customer along with their computed net balance.
 * Used in the Due Dashboard to sort customers by outstanding amount.
 */
data class CustomerWithBalance(
    val id: Long,
    val name: String,
    val phone: String,
    val photoPath: String?,
    val netBalance: Double, // positive = customer owes, negative = shop owes
    val oldestTransactionTime: Long? = null // timestamp of oldest credit transaction (for due ageing)
)

