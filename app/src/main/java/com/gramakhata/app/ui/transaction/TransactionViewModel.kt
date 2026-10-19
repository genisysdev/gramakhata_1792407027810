package com.gramakhata.app.ui.transaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gramakhata.app.GramaKhataApp
import com.gramakhata.app.data.db.entity.Transaction
import com.gramakhata.app.data.repository.AppRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for adding transactions (credit/payment).
 */
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository = (application as GramaKhataApp).repository

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    fun saveTransaction(customerId: Long, amount: Double, isCredit: Boolean, note: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    customerId = customerId,
                    amount = amount,
                    isCredit = isCredit,
                    note = note
                )
            )
            _saveResult.value = true
        }
    }
}
