package com.gramakhata.app.ui.customer

import android.app.Application
import androidx.lifecycle.*
import com.gramakhata.app.GramaKhataApp
import com.gramakhata.app.data.db.entity.Customer
import com.gramakhata.app.data.db.entity.Transaction
import com.gramakhata.app.data.repository.AppRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for Customer add/edit and detail screens.
 */
class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository = (application as GramaKhataApp).repository

    private val _saveResult = MutableLiveData<Long>()
    val saveResult: LiveData<Long> = _saveResult

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    fun getCustomerById(id: Long): LiveData<Customer?> =
        repository.getCustomerByIdLive(id)

    fun getNetBalance(customerId: Long): LiveData<Double> =
        repository.getNetBalanceForCustomer(customerId)

    fun getTransactions(customerId: Long) =
        repository.getTransactionsForCustomer(customerId)

    fun saveCustomer(name: String, phone: String, photoPath: String?, existingId: Long = -1L) {
        viewModelScope.launch {
            if (existingId > 0) {
                val existing = repository.getCustomerById(existingId)
                if (existing != null) {
                    repository.updateCustomer(
                        existing.copy(name = name, phone = phone, photoPath = photoPath ?: existing.photoPath)
                    )
                    _saveResult.value = existingId
                }
            } else {
                val id = repository.insertCustomer(
                    Customer(name = name, phone = phone, photoPath = photoPath)
                )
                _saveResult.value = id
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _deleteResult.value = true
        }
    }

    /**
     * Delete a single transaction (for undo/fix wrong entries).
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    /**
     * Update an existing transaction (edit amount/note).
     */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }
}

