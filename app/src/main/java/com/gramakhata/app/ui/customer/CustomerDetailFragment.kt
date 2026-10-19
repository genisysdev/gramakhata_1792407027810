package com.gramakhata.app.ui.customer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.gramakhata.app.R
import com.gramakhata.app.databinding.FragmentCustomerDetailBinding
import com.gramakhata.app.ui.transaction.TransactionAdapter
import com.gramakhata.app.util.toRupeeString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.net.URLEncoder

/**
 * Customer Detail screen showing balance, action buttons,
 * reminder options, and transaction history.
 */
class CustomerDetailFragment : Fragment() {

    private var _binding: FragmentCustomerDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomerViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    private var customerId: Long = -1L
    private var customerPhone: String = ""
    private var customerName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        customerId = arguments?.getLong("customerId", -1L) ?: -1L
        if (customerId <= 0) {
            findNavController().popBackStack()
            return
        }

        setupTransactionList()
        setupButtons()
        observeData()
    }

    private fun setupTransactionList() {
        adapter = TransactionAdapter(
            onEditClick = { transaction ->
                showEditTransactionDialog(transaction)
            },
            onDeleteClick = { transaction ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.txn_delete_title)
                    .setMessage(getString(R.string.txn_delete_confirm))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        viewModel.deleteTransaction(transaction)
                        Toast.makeText(context, getString(R.string.txn_deleted), Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        )
        binding.rvTransactions.adapter = adapter
    }

    /**
     * Show a dialog with pre-filled amount & note for editing an existing transaction.
     */
    private fun showEditTransactionDialog(transaction: com.gramakhata.app.data.db.entity.Transaction) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_transaction, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditAmount)
        val etNote = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEditNote)

        // Pre-fill existing values
        etAmount.setText(transaction.amount.toString())
        etNote.setText(transaction.note)

        val typeLabel = if (transaction.isCredit) getString(R.string.txn_credit_label) else getString(R.string.txn_payment_label)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${getString(R.string.txn_edit)} — $typeLabel")
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newAmount = etAmount.text?.toString()?.trim()?.toDoubleOrNull()
                val newNote = etNote.text?.toString()?.trim() ?: ""

                if (newAmount != null && newAmount > 0) {
                    val updated = transaction.copy(amount = newAmount, note = newNote)
                    viewModel.updateTransaction(updated)
                    Toast.makeText(context, getString(R.string.txn_updated), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupButtons() {
        // Credit button
        binding.btnCredit.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("customerId", customerId)
                putBoolean("isCredit", true)
            }
            findNavController().navigate(R.id.action_customerDetail_to_addTransaction, bundle)
        }

        // Payment button
        binding.btnPayment.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("customerId", customerId)
                putBoolean("isCredit", false)
            }
            findNavController().navigate(R.id.action_customerDetail_to_addTransaction, bundle)
        }

        // WhatsApp reminder
        binding.btnWhatsApp.setOnClickListener { sendWhatsAppReminder() }

        // SMS reminder
        binding.btnSMS.setOnClickListener { sendSMSReminder() }
    }

    private fun observeData() {
        // Customer info
        viewModel.getCustomerById(customerId).observe(viewLifecycleOwner) { customer ->
            customer?.let {
                customerName = it.name
                customerPhone = it.phone
                binding.tvName.text = it.name
                binding.tvPhone.text = it.phone.ifBlank { "No phone number" }

                if (!it.photoPath.isNullOrBlank() && File(it.photoPath).exists()) {
                    binding.ivAvatar.load(File(it.photoPath)) {
                        transformations(CircleCropTransformation())
                    }
                    binding.ivAvatar.setPadding(0, 0, 0, 0)
                }
            }
        }

        // Net balance
        viewModel.getNetBalance(customerId).observe(viewLifecycleOwner) { balance ->
            val bal = balance ?: 0.0
            binding.tvNetBalance.text = bal.toRupeeString()

            if (bal > 0) {
                binding.tvBalanceLabel.text = getString(R.string.customer_will_get)
                binding.tvNetBalance.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.secondary_light)
                )
            } else if (bal < 0) {
                binding.tvBalanceLabel.text = getString(R.string.customer_will_give)
            } else {
                binding.tvBalanceLabel.text = getString(R.string.customer_net_balance)
            }
        }

        // Transaction history
        viewModel.getTransactions(customerId).observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
            binding.tvNoTransactions.visibility =
                if (transactions.isNullOrEmpty()) View.VISIBLE else View.GONE
            binding.rvTransactions.visibility =
                if (transactions.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun getShopName(): String {
        val prefs = requireContext().getSharedPreferences("shop_prefs", 0)
        return prefs.getString("shop_name", getString(R.string.shop_default_name)) ?: "My Shop"
    }

    private fun getOwnerName(): String {
        val prefs = requireContext().getSharedPreferences("shop_prefs", 0)
        return prefs.getString("owner_name", getString(R.string.shop_default_owner)) ?: "Shopkeeper"
    }

    private fun getReminderMessage(balance: Double): String {
        return getString(
            R.string.remind_message,
            getShopName(),
            String.format("%.0f", balance),
            getOwnerName()
        )
    }

    private fun sendWhatsAppReminder() {
        if (customerPhone.isBlank()) {
            Toast.makeText(context, getString(R.string.remind_no_phone), Toast.LENGTH_SHORT).show()
            return
        }

        val balance = binding.tvNetBalance.text.toString()
        val message = getReminderMessage(
            balance.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: 0.0
        )

        try {
            val phone = customerPhone.replace("[^\\d+]".toRegex(), "")
            val phoneFormatted = if (phone.startsWith("+")) phone else "+91$phone"
            val url = "https://wa.me/$phoneFormatted?text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, getString(R.string.remind_whatsapp_not_installed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSMSReminder() {
        if (customerPhone.isBlank()) {
            Toast.makeText(context, getString(R.string.remind_no_phone), Toast.LENGTH_SHORT).show()
            return
        }

        val balance = binding.tvNetBalance.text.toString()
        val message = getReminderMessage(
            balance.replace("[^\\d.]".toRegex(), "").toDoubleOrNull() ?: 0.0
        )

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${customerPhone}")
            putExtra("sms_body", message)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
