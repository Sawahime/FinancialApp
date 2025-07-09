package com.example.financialapp

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.financialapp.databinding.ItemIncomeRecordBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class IncomeRecordsAdapter(
    private val records: MutableList<IncomeRecord>,
    private val onDeleteClick: (Int, Long) -> Unit
) : RecyclerView.Adapter<IncomeRecordsAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    class ViewHolder(val binding: ItemIncomeRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIncomeRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        
        with(holder.binding) {
            tvAmount.text = currencyFormat.format(record.amount)
            tvDescription.text = record.description ?: "无备注"
            tvDate.text = dateFormat.format(Date(record.date))
            
            btnDelete.setOnClickListener {
                animateAndRemoveItem(holder, position) {
                    onDeleteClick(position, record.id)
                }
            }
        }
    }

    override fun getItemCount() = records.size

    /**
     * 执行删除动画并移除项目
     */
    private fun animateAndRemoveItem(holder: ViewHolder, position: Int, onAnimationEnd: () -> Unit) {
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
                if (position < records.size) {
                    records.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, records.size)
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
}
