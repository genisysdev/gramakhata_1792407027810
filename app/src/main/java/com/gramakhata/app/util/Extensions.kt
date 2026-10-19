package com.gramakhata.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Format a Double amount to Indian Rupee display format.
 * Example: 12450.0 -> "₹ 12,450"
 */
fun Double.toRupeeString(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(this)
}

/**
 * Format a timestamp to a readable date-time string.
 */
fun Long.toDateTimeString(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Format a timestamp to a date-only string.
 */
fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Get the start of today (midnight) in milliseconds.
 */
fun getStartOfToday(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
