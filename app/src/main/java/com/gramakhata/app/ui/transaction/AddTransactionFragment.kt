package com.gramakhata.app.ui.transaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.gramakhata.app.R
import com.gramakhata.app.data.db.entity.Customer
import com.gramakhata.app.databinding.FragmentAddTransactionBinding
import com.gramakhata.app.ui.customer.CustomerViewModel
import java.util.Locale

/**
 * Fragment for adding a credit or payment transaction.
 * Supports voice input for amount AND item description (GenAI feature).
 */
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var customerViewModel: CustomerViewModel

    private var customerId: Long = -1L
    private var isCredit: Boolean = true

    // Voice input for AMOUNT — extracts numbers only
    private val voiceAmountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()?.let { spoken ->
                val amount = spoken.replace("[^\\d.]".toRegex(), "")
                if (amount.isNotBlank()) {
                    binding.etAmount.setText(amount)
                }
            }
        }
    }

    // Voice input for NOTE/DESCRIPTION — uses full spoken text
    private val voiceNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()?.let { spoken ->
                val existing = binding.etNote.text?.toString()?.trim() ?: ""
                if (existing.isBlank()) {
                    binding.etNote.setText(spoken)
                } else {
                    binding.etNote.setText("$existing, $spoken")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        customerViewModel = ViewModelProvider(this)[CustomerViewModel::class.java]

        customerId = arguments?.getLong("customerId", -1L) ?: -1L
        isCredit = arguments?.getBoolean("isCredit", true) ?: true

        setupUI()
        setupButtons()
        observeData()
    }

    private fun setupUI() {
        if (isCredit) {
            binding.tvTransactionType.text = getString(R.string.txn_credit_title)
            binding.tvTransactionType.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.credit_red)
            )
            binding.btnSave.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.credit_red)
            )
        } else {
            binding.tvTransactionType.text = getString(R.string.txn_payment_title)
            binding.tvTransactionType.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.payment_green)
            )
            binding.btnSave.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.payment_green)
            )
        }

        customerViewModel.getCustomerById(customerId).observe(viewLifecycleOwner) { customer ->
            customer?.let {
                binding.tvCustomerName.text = "For: ${it.name}"
            }
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text?.toString()?.trim() ?: ""
            val note = binding.etNote.text?.toString()?.trim() ?: ""

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                binding.etAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }

            transactionViewModel.saveTransaction(customerId, amount, isCredit, note)
        }

        // Voice input for AMOUNT
        binding.btnVoiceInput.setOnClickListener {
            startVoiceInput(voiceAmountLauncher, getString(R.string.voice_speak_amount))
        }

        // Voice input for ITEM DESCRIPTION
        binding.btnVoiceNote.setOnClickListener {
            startVoiceInput(voiceNoteLauncher, getString(R.string.voice_speak_item))
        }
    }

    private fun observeData() {
        transactionViewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                val msg = if (isCredit) "Credit recorded!" else "Payment recorded!"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun startVoiceInput(
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
        prompt: String
    ) {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
            launcher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.voice_not_supported), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

