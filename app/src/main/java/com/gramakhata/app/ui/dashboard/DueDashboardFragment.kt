package com.gramakhata.app.ui.dashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.gramakhata.app.R
import com.gramakhata.app.databinding.FragmentDueDashboardBinding
import com.gramakhata.app.util.toDateString
import com.gramakhata.app.util.toRupeeString
import com.google.android.material.datepicker.MaterialDatePicker

/**
 * Due Dashboard — the home screen of the app.
 * Shows total outstanding, search bar, date filter, and customer list sorted by highest due.
 */
class DueDashboardFragment : Fragment() {

    private var _binding: FragmentDueDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DueDashboardViewModel by viewModels()
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDueDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupDateFilter()
        setupFAB()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter { customer ->
            // Navigate to customer detail
            val bundle = Bundle().apply {
                putLong("customerId", customer.id)
            }
            findNavController().navigate(R.id.action_dashboard_to_customerDetail, bundle)
        }
        binding.rvCustomers.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })
    }

    private fun setupDateFilter() {
        binding.btnDateFilter.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Filter by date range")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val startDate = selection.first ?: return@addOnPositiveButtonClickListener
                val endDate = (selection.second ?: startDate) + (24 * 60 * 60 * 1000 - 1) // End of day

                viewModel.setDateRange(startDate, endDate)

                // Show active filter chip
                binding.chipDateFilter.visibility = View.VISIBLE
                binding.chipDateFilter.text = "${startDate.toDateString()} — ${selection.second?.toDateString() ?: startDate.toDateString()}"
            }

            picker.show(parentFragmentManager, "date_range_picker")
        }

        // Clear date filter when chip is dismissed
        binding.chipDateFilter.setOnCloseIconClickListener {
            viewModel.clearDateRange()
            binding.chipDateFilter.visibility = View.GONE
        }
    }

    private fun setupFAB() {
        binding.fabAddCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_addCustomer)
        }
    }

    private fun observeData() {
        // Observe customer list
        viewModel.customersWithBalance.observe(viewLifecycleOwner) { customers ->
            adapter.submitList(customers)

            // Show/hide empty state
            if (customers.isNullOrEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvCustomers.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvCustomers.visibility = View.VISIBLE
            }

            // Update customer count text
            val dueCount = customers?.count { it.netBalance > 0 } ?: 0
            binding.cardTotalDue.findViewById<android.widget.TextView>(R.id.tvCustomerCount)?.text =
                "$dueCount customers with outstanding dues"
        }

        // Observe total outstanding
        viewModel.totalOutstanding.observe(viewLifecycleOwner) { total ->
            binding.tvTotalDue.text = (total ?: 0.0).toRupeeString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

