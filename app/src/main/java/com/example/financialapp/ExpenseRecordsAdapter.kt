package com.example.financialapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.financialapp.databinding.ItemExpenseRecordBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ExpenseRecordsAdapter(
    private val records: List<ExpenseRecord>,
    private val onDeleteClick: (Long) -> Unit
) : RecyclerView.Adapter<ExpenseRecordsAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    class ViewHolder(val binding: ItemExpenseRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        
        with(holder.binding) {
            tvCategory.text = record.category
            tvAmount.text = currencyFormat.format(record.amount)
            tvDescription.text = record.description ?: "无备注"
            tvDate.text = dateFormat.format(Date(record.date))
            
            btnDelete.setOnClickListener {
                onDeleteClick(record.id)
            }
        }
    }

    override fun getItemCount() = records.size
}
