package com.example.financialapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.financialapp.databinding.ItemSettingsHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SettingsHistoryAdapter(
    private val records: List<SalarySettingsRecord>,
    private val onItemClick: (SalarySettingsRecord) -> Unit
) : RecyclerView.Adapter<SettingsHistoryAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    class ViewHolder(val binding: ItemSettingsHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSettingsHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        
        with(holder.binding) {
            // 生效时间
            tvEffectiveDate.text = if (record.isHistoricalModification) {
                "${record.modifiedYear}年${record.modifiedMonth}月"
            } else {
                dateFormat.format(Date(record.effectiveDate))
            }
            
            // 历史修改标签
            if (record.isHistoricalModification) {
                tvModificationTag.visibility = View.VISIBLE
                tvModificationTag.text = "历史修改"
            } else {
                tvModificationTag.visibility = View.GONE
            }
            
            // 工资
            tvSalary.text = "月薪：${currencyFormat.format(record.grossSalary)}"
            
            // 社保比例
            tvSocialRates.text = "社保：个人${(record.personalSocialInsuranceRate * 100).toInt()}% " +
                    "公司${(record.companySocialInsuranceRate * 100).toInt()}%"
            
            // 公积金比例
            tvHousingRates.text = "公积金：个人${(record.personalHousingFundRate * 100).toInt()}% " +
                    "公司${(record.companyHousingFundRate * 100).toInt()}%"
            
            // 创建时间
            tvCreatedAt.text = "创建时间：${dateFormat.format(Date(record.createdAt))}"
            
            // 点击事件
            root.setOnClickListener {
                onItemClick(record)
            }
        }
    }

    override fun getItemCount() = records.size
}
