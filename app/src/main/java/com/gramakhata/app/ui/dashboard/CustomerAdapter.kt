package com.gramakhata.app.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.gramakhata.app.R
import com.gramakhata.app.data.db.entity.CustomerWithBalance
import com.gramakhata.app.databinding.ItemCustomerBinding
import com.gramakhata.app.util.toRupeeString
import java.io.File

/**
 * RecyclerView adapter for the customer list on the Due Dashboard.
 * Displays customer name, phone, avatar, net balance with colour coding,
 * and due ageing indicator (green/amber/red based on days overdue).
 */
class CustomerAdapter(
    private val onItemClick: (CustomerWithBalance) -> Unit
) : ListAdapter<CustomerWithBalance, CustomerAdapter.CustomerViewHolder>(CustomerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CustomerViewHolder(
        private val binding: ItemCustomerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomerWithBalance) {
            binding.tvCustomerName.text = item.name
            binding.tvPhone.text = item.phone.ifBlank { "No phone" }

            // Balance display with color coding
            val balance = item.netBalance
            binding.tvDueAmount.text = balance.toRupeeString()

            val color = if (balance > 0) {
                ContextCompat.getColor(binding.root.context, R.color.credit_red)
            } else if (balance < 0) {
                ContextCompat.getColor(binding.root.context, R.color.payment_green)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.text_secondary)
            }
            binding.tvDueAmount.setTextColor(color)

            // Due Ageing indicator — color-code based on days since oldest transaction
            val ageingView = binding.root.findViewById<View>(R.id.viewAgeingIndicator)
            if (ageingView != null && balance > 0) {
                ageingView.visibility = View.VISIBLE
                val daysSinceCreated = ((System.currentTimeMillis() - (item.oldestTransactionTime ?: System.currentTimeMillis())) / (1000 * 60 * 60 * 24)).toInt()
                val ageingColor = when {
                    daysSinceCreated > 30 -> ContextCompat.getColor(binding.root.context, R.color.due_overdue)
                    daysSinceCreated > 7 -> ContextCompat.getColor(binding.root.context, R.color.due_warning)
                    else -> ContextCompat.getColor(binding.root.context, R.color.due_normal)
                }
                ageingView.setBackgroundColor(ageingColor)
            } else {
                ageingView?.visibility = View.GONE
            }

            // Load avatar photo
            if (!item.photoPath.isNullOrBlank() && File(item.photoPath).exists()) {
                binding.ivAvatar.load(File(item.photoPath)) {
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.ic_person)
                    error(R.drawable.ic_person)
                }
                binding.ivAvatar.setPadding(0, 0, 0, 0)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_person)
                val pad = (8 * binding.root.resources.displayMetrics.density).toInt()
                binding.ivAvatar.setPadding(pad, pad, pad, pad)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class CustomerDiffCallback : DiffUtil.ItemCallback<CustomerWithBalance>() {
        override fun areItemsTheSame(old: CustomerWithBalance, new: CustomerWithBalance) =
            old.id == new.id

        override fun areContentsTheSame(old: CustomerWithBalance, new: CustomerWithBalance) =
            old == new
    }
}

