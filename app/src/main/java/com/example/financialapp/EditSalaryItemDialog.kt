package com.example.financialapp

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.financialapp.databinding.DialogEditSalaryItemBinding

/**
 * 编辑工资项对话框
 */
class EditSalaryItemDialog : DialogFragment() {
    
    private var _binding: DialogEditSalaryItemBinding? = null
    private val binding get() = _binding!!
    
    private var salaryItem: SalaryItem? = null
    private var onSalaryItemUpdated: ((SalaryItem) -> Unit)? = null
    
    companion object {
        fun newInstance(
            salaryItem: SalaryItem,
            onSalaryItemUpdated: (SalaryItem) -> Unit
        ): EditSalaryItemDialog {
            return EditSalaryItemDialog().apply {
                this.salaryItem = salaryItem
                this.onSalaryItemUpdated = onSalaryItemUpdated
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditSalaryItemBinding.inflate(LayoutInflater.from(context))
        
        setupUI()
        
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
    
    private fun setupUI() {
        val item = salaryItem ?: return
        
        // 设置当前值
        binding.etSalaryItemName.setText(item.name)
        binding.etSalaryItemAmount.setText(item.amount.toString())
        binding.cbIncludeTax.isChecked = item.includeTax
        binding.cbIncludeSocialSecurity.isChecked = item.includeSocialSecurity
        
        // 如果是默认项目，禁用基数设置修改
        if (item.isDefault) {
            binding.cbIncludeTax.isEnabled = false
            binding.cbIncludeSocialSecurity.isEnabled = false
        }
        
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveSalaryItem()
        }
    }
    
    private fun saveSalaryItem() {
        val item = salaryItem ?: return
        val amountText = binding.etSalaryItemAmount.text.toString().trim()
        
        // 验证金额
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount < 0) {
            showError("请输入有效的金额（不能为负数）")
            return
        }
        
        // 创建更新后的工资项
        val updatedItem = item.copy(
            amount = amount,
            includeTax = binding.cbIncludeTax.isChecked,
            includeSocialSecurity = binding.cbIncludeSocialSecurity.isChecked
        )
        
        // 回调更新工资项
        onSalaryItemUpdated?.invoke(updatedItem)
        
        dismiss()
    }
    
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
