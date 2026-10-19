package com.gramakhata.app.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.gramakhata.app.data.db.entity.Customer
import com.gramakhata.app.data.db.entity.CustomerWithBalance

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): LiveData<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerByIdLive(id: Long): LiveData<Customer?>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): LiveData<List<Customer>>

    /**
     * Returns all customers with their net balance (credit - payment).
     * Positive balance = customer owes the shop.
     * Sorted by highest due amount first.
     */
    @Query("""
        SELECT c.id, c.name, c.phone, c.photoPath,
               COALESCE(SUM(CASE WHEN t.isCredit = 1 THEN t.amount ELSE -t.amount END), 0) as netBalance,
               MIN(CASE WHEN t.isCredit = 1 THEN t.timestamp END) as oldestTransactionTime
        FROM customers c
        LEFT JOIN transactions t ON c.id = t.customerId
        GROUP BY c.id
        ORDER BY netBalance DESC
    """)
    fun getCustomersWithBalance(): LiveData<List<CustomerWithBalance>>

    /**
     * Search customers with balance.
     */
    @Query("""
        SELECT c.id, c.name, c.phone, c.photoPath,
               COALESCE(SUM(CASE WHEN t.isCredit = 1 THEN t.amount ELSE -t.amount END), 0) as netBalance,
               MIN(CASE WHEN t.isCredit = 1 THEN t.timestamp END) as oldestTransactionTime
        FROM customers c
        LEFT JOIN transactions t ON c.id = t.customerId
        WHERE c.name LIKE '%' || :query || '%'
        GROUP BY c.id
        ORDER BY netBalance DESC
    """)
    fun searchCustomersWithBalance(query: String): LiveData<List<CustomerWithBalance>>

    /**
     * Returns customers with balance filtered by transaction date range.
     */
    @Query("""
        SELECT c.id, c.name, c.phone, c.photoPath,
               COALESCE(SUM(CASE WHEN t.isCredit = 1 THEN t.amount ELSE -t.amount END), 0) as netBalance,
               MIN(CASE WHEN t.isCredit = 1 THEN t.timestamp END) as oldestTransactionTime
        FROM customers c
        INNER JOIN transactions t ON c.id = t.customerId
        WHERE t.timestamp BETWEEN :startDate AND :endDate
        GROUP BY c.id
        ORDER BY netBalance DESC
    """)
    fun getCustomersWithBalanceInRange(startDate: Long, endDate: Long): LiveData<List<CustomerWithBalance>>

    /**
     * Search customers with balance filtered by date range.
     */
    @Query("""
        SELECT c.id, c.name, c.phone, c.photoPath,
               COALESCE(SUM(CASE WHEN t.isCredit = 1 THEN t.amount ELSE -t.amount END), 0) as netBalance,
               MIN(CASE WHEN t.isCredit = 1 THEN t.timestamp END) as oldestTransactionTime
        FROM customers c
        INNER JOIN transactions t ON c.id = t.customerId
        WHERE c.name LIKE '%' || :query || '%' AND t.timestamp BETWEEN :startDate AND :endDate
        GROUP BY c.id
        ORDER BY netBalance DESC
    """)
    fun searchCustomersWithBalanceInRange(query: String, startDate: Long, endDate: Long): LiveData<List<CustomerWithBalance>>

    /**
     * Get all customers (suspend version for export).
     */
    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersSuspend(): List<Customer>
}
