package com.gramakhata.app.data.repository

import androidx.lifecycle.LiveData
import com.gramakhata.app.data.db.dao.CustomerDao
import com.gramakhata.app.data.db.dao.TransactionDao
import com.gramakhata.app.data.db.entity.Customer
import com.gramakhata.app.data.db.entity.CustomerWithBalance
import com.gramakhata.app.data.db.entity.Transaction

/**
 * Repository layer that mediates between ViewModels and Room DAOs.
 * Single source of truth for all data operations.
 */
class AppRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao
) {
    // ---- Customer Operations ----

    val allCustomersWithBalance: LiveData<List<CustomerWithBalance>> =
        customerDao.getCustomersWithBalance()

    fun searchCustomersWithBalance(query: String): LiveData<List<CustomerWithBalance>> =
        customerDao.searchCustomersWithBalance(query)

    fun getCustomersWithBalanceInRange(startDate: Long, endDate: Long): LiveData<List<CustomerWithBalance>> =
        customerDao.getCustomersWithBalanceInRange(startDate, endDate)

    fun searchCustomersWithBalanceInRange(query: String, startDate: Long, endDate: Long): LiveData<List<CustomerWithBalance>> =
        customerDao.searchCustomersWithBalanceInRange(query, startDate, endDate)

    fun getCustomerByIdLive(id: Long): LiveData<Customer?> =
        customerDao.getCustomerByIdLive(id)

    suspend fun getCustomerById(id: Long): Customer? =
        customerDao.getCustomerById(id)

    suspend fun insertCustomer(customer: Customer): Long =
        customerDao.insert(customer)

    suspend fun updateCustomer(customer: Customer) =
        customerDao.update(customer)

    suspend fun deleteCustomer(customer: Customer) =
        customerDao.delete(customer)

    // ---- Transaction Operations ----

    fun getTransactionsForCustomer(customerId: Long): LiveData<List<Transaction>> =
        transactionDao.getTransactionsForCustomer(customerId)

    fun getNetBalanceForCustomer(customerId: Long): LiveData<Double> =
        transactionDao.getNetBalanceForCustomer(customerId)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.delete(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.update(transaction)

    // ---- Dashboard / Report Operations ----

    val totalOutstanding: LiveData<Double> =
        transactionDao.getTotalOutstanding()

    fun getTransactionsForDay(startOfDay: Long): LiveData<List<Transaction>> =
        transactionDao.getTransactionsForDay(startOfDay)

    suspend fun getTransactionsForDaySuspend(startOfDay: Long): List<Transaction> =
        transactionDao.getTransactionsForDaySuspend(startOfDay)

    fun getTotalCreditForDay(startOfDay: Long): LiveData<Double> =
        transactionDao.getTotalCreditForDay(startOfDay)

    fun getTotalPaymentForDay(startOfDay: Long): LiveData<Double> =
        transactionDao.getTotalPaymentForDay(startOfDay)

    // ---- Export ----

    suspend fun getAllCustomers(): List<Customer> =
        customerDao.getAllCustomersSuspend()

    suspend fun getAllTransactions(): List<Transaction> =
        transactionDao.getAllTransactions()
}
