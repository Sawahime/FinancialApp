package com.example.financialapp

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 全部记录适配器，同时显示支出和收入记录
 */
class AllRecordsAdapter(
    private val expenseRecords: List<ExpenseRecord>,
    private val incomeRecords: List<IncomeRecord>,
    private val onDeleteClick: (position: Int, isExpense: Boolean, recordId: Long) -> Unit
) : RecyclerView.Adapter<AllRecordsAdapter.RecordViewHolder>() {

    private val allRecords = mutableListOf<RecordItem>()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    init {
        // 合并所有记录并按时间排序
        val combinedRecords = mutableListOf<RecordItem>()
        
        expenseRecords.forEach { record ->
            combinedRecords.add(RecordItem.ExpenseItem(record))
        }
        
        incomeRecords.forEach { record ->
            combinedRecords.add(RecordItem.IncomeItem(record))
        }
        
        // 按时间倒序排列（最新的在前）
        allRecords.addAll(combinedRecords.sortedByDescending { it.getTimestamp() })
    }

    sealed class RecordItem {
        abstract fun getTimestamp(): Long
        
        data class ExpenseItem(val record: ExpenseRecord) : RecordItem() {
            override fun getTimestamp() = record.date
        }
        
        data class IncomeItem(val record: IncomeRecord) : RecordItem() {
            override fun getTimestamp() = record.date
        }
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = allRecords[position]
        
        when (item) {
            is RecordItem.ExpenseItem -> {
                val record = item.record
                holder.tvAmount.text = "-${currencyFormat.format(record.amount)}"
                holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.expense_color))
                holder.tvCategory.text = record.category
                holder.tvDescription.text = record.description ?: "无备注"
                holder.tvDate.text = dateFormat.format(Date(record.date))

                holder.btnDelete.setOnClickListener {
                    animateAndRemoveItem(holder, position) {
                        onDeleteClick(position, true, record.id)
                    }
                }
            }

            is RecordItem.IncomeItem -> {
                val record = item.record
                holder.tvAmount.text = "+${currencyFormat.format(record.amount)}"
                holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.income_color))
                holder.tvCategory.text = "其他收入"
                holder.tvDescription.text = record.description ?: "无备注"
                holder.tvDate.text = dateFormat.format(Date(record.date))

                holder.btnDelete.setOnClickListener {
                    animateAndRemoveItem(holder, position) {
                        onDeleteClick(position, false, record.id)
                    }
                }
            }
        }
    }

    override fun getItemCount() = allRecords.size

    /**
     * 执行删除动画并移除项目
     */
    private fun animateAndRemoveItem(holder: RecordViewHolder, position: Int, onAnimationEnd: () -> Unit) {
        // 创建淡出动画
        val fadeOut = ObjectAnimator.ofFloat(holder.itemView, "alpha", 1f, 0f)
        fadeOut.duration = 300

        // 创建缩放动画
        val scaleX = ObjectAnimator.ofFloat(holder.itemView, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(holder.itemView, "scaleY", 1f, 0f)
        scaleX.duration = 300
        scaleY.duration = 300

        // 动画结束后移除项目
        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // 从列表中移除项目
                if (position < allRecords.size) {
                    allRecords.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, allRecords.size)
                }
                // 执行删除回调
                onAnimationEnd()
            }
        })

        // 开始动画
        fadeOut.start()
        scaleX.start()
        scaleY.start()
    }

    /**
     * 恢复被误删的项目（如果删除失败）
     */
    fun restoreItem(position: Int, item: RecordItem) {
        if (position <= allRecords.size) {
            allRecords.add(position, item)
            notifyItemInserted(position)
        }
    }
}
