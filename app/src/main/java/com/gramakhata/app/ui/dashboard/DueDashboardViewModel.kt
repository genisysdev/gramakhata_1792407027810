package com.gramakhata.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.*
import com.gramakhata.app.GramaKhataApp
import com.gramakhata.app.data.db.entity.CustomerWithBalance
import com.gramakhata.app.data.repository.AppRepository

/**
 * ViewModel for the Due Dashboard.
 * Provides live customer list with balances, total outstanding,
 * and supports search + date range filtering.
 */
class DueDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository = (application as GramaKhataApp).repository

    private val searchQuery = MutableLiveData("")
    private val dateRange = MutableLiveData<Pair<Long, Long>?>(null)

    /**
     * Combined filter trigger — fires whenever search or date range changes.
     */
    private val filterTrigger = MediatorLiveData<Pair<String, Pair<Long, Long>?>>().apply {
        addSource(searchQuery) { query ->
            value = Pair(query ?: "", dateRange.value)
        }
        addSource(dateRange) { range ->
            value = Pair(searchQuery.value ?: "", range)
        }
    }

    /**
     * Customers list that reacts to search query and date range changes.
     */
    val customersWithBalance: LiveData<List<CustomerWithBalance>> = filterTrigger.switchMap { (query, range) ->
        val source = if (query.isBlank()) {
            if (range != null) {
                repository.getCustomersWithBalanceInRange(range.first, range.second)
            } else {
                repository.allCustomersWithBalance
            }
        } else {
            if (range != null) {
                repository.searchCustomersWithBalanceInRange(query, range.first, range.second)
            } else {
                repository.searchCustomersWithBalance(query)
            }
        }
        source
    }

    /**
     * Total outstanding amount across all customers.
     */
    val totalOutstanding: LiveData<Double> = repository.totalOutstanding

    /**
     * Update the search query to filter customers.
     */
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    /**
     * Set a date range filter.
     */
    fun setDateRange(startMs: Long, endMs: Long) {
        dateRange.value = Pair(startMs, endMs)
    }

    /**
     * Clear the date range filter.
     */
    fun clearDateRange() {
        dateRange.value = null
    }
}

