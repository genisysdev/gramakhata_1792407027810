package com.gramakhata.app.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gramakhata.app.data.db.entity.Transaction

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Delete
    suspend fun delete(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    /**
     * Get all transactions for a specific customer, newest first.
     */
    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): LiveData<List<Transaction>>

    /**
     * Get net balance for a specific customer.
     * Positive = customer owes shop.
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN isCredit = 1 THEN amount ELSE -amount END), 0)
        FROM transactions WHERE customerId = :customerId
    """)
    fun getNetBalanceForCustomer(customerId: Long): LiveData<Double>

    /**
     * Get all transactions for today (from start of day to now).
     */
    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay ORDER BY timestamp ASC")
    fun getTransactionsForDay(startOfDay: Long): LiveData<List<Transaction>>

    /**
     * Get today's transactions (suspend version for report generation).
     */
    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay ORDER BY timestamp ASC")
    suspend fun getTransactionsForDaySuspend(startOfDay: Long): List<Transaction>

    /**
     * Get total credit given today.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isCredit = 1 AND timestamp >= :startOfDay
    """)
    fun getTotalCreditForDay(startOfDay: Long): LiveData<Double>

    /**
     * Get total payment received today.
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE isCredit = 0 AND timestamp >= :startOfDay
    """)
    fun getTotalPaymentForDay(startOfDay: Long): LiveData<Double>

    /**
     * Get total outstanding across all customers.
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN isCredit = 1 THEN amount ELSE -amount END), 0)
        FROM transactions
    """)
    fun getTotalOutstanding(): LiveData<Double>

    /**
     * Get all transactions (for export).
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<Transaction>
}
