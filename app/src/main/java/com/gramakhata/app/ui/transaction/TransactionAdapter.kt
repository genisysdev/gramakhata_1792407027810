package com.gramakhata.app.ui.transaction

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gramakhata.app.R
import com.gramakhata.app.data.db.entity.Transaction
import com.gramakhata.app.databinding.ItemTransactionBinding
import com.gramakhata.app.util.toDateTimeString
import com.gramakhata.app.util.toRupeeString

/**
 * RecyclerView adapter for transaction history items.
 * Color-codes credits (red) and payments (green).
 * Long-press shows Edit/Delete options.
 */
class TransactionAdapter(
    private val onEditClick: ((Transaction) -> Unit)? = null,
    private val onDeleteClick: ((Transaction) -> Unit)? = null
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(txn: Transaction) {
            val context = binding.root.context

            // Type badge
            if (txn.isCredit) {
                binding.tvType.text = context.getString(R.string.txn_credit_label)
                binding.tvType.setTextColor(ContextCompat.getColor(context, R.color.credit_red))
                binding.tvType.setBackgroundResource(R.drawable.bg_credit_badge)

                binding.tvAmount.text = "+ ${txn.amount.toRupeeString()}"
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.credit_red))
            } else {
                binding.tvType.text = context.getString(R.string.txn_payment_label)
                binding.tvType.setTextColor(ContextCompat.getColor(context, R.color.payment_green))
                binding.tvType.setBackgroundResource(R.drawable.bg_payment_badge)

                binding.tvAmount.text = "- ${txn.amount.toRupeeString()}"
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.payment_green))
            }

            // Date
            binding.tvDate.text = txn.timestamp.toDateTimeString()

            // Note
            binding.tvNote.text = txn.note.ifBlank { "—" }

            // Long press → show Edit / Delete options
            binding.root.setOnLongClickListener {
                val options = arrayOf(
                    context.getString(R.string.txn_edit),
                    context.getString(R.string.txn_delete_title)
                )
                android.app.AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.txn_options_title))
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> onEditClick?.invoke(txn)
                            1 -> onDeleteClick?.invoke(txn)
                        }
                    }
                    .show()
                true
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(old: Transaction, new: Transaction) = old.id == new.id
        override fun areContentsTheSame(old: Transaction, new: Transaction) = old == new
    }
}

