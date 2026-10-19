package com.gramakhata.app.ui.report

import android.app.Application
import androidx.lifecycle.*
import com.gramakhata.app.GramaKhataApp
import com.gramakhata.app.data.db.entity.Transaction
import com.gramakhata.app.data.repository.AppRepository
import com.gramakhata.app.util.getStartOfToday

/**
 * ViewModel for the Daily Collection Report.
 */
class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository = (application as GramaKhataApp).repository
    private val startOfToday = getStartOfToday()

    val todayTransactions: LiveData<List<Transaction>> =
        repository.getTransactionsForDay(startOfToday)

    val totalCreditToday: LiveData<Double> =
        repository.getTotalCreditForDay(startOfToday)

    val totalPaymentToday: LiveData<Double> =
        repository.getTotalPaymentForDay(startOfToday)
}
