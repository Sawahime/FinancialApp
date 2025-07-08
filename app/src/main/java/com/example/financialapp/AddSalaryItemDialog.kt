package com.example.financialapp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.financialapp.databinding.DialogAddSalaryItemBinding

/**
 * 添加工资项对话框
 */
class AddSalaryItemDialog : DialogFragment() {
    
    private var _binding: DialogAddSalaryItemBinding? = null
    private val binding get() = _binding!!
    
    private var onSalaryItemAdded: ((String, Double, Boolean, Boolean) -> Unit)? = null
    
    companion object {
        fun newInstance(onSalaryItemAdded: (String, Double, Boolean, Boolean) -> Unit): AddSalaryItemDialog {
            return AddSalaryItemDialog().apply {
                this.onSalaryItemAdded = onSalaryItemAdded
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddSalaryItemBinding.inflate(LayoutInflater.from(context))
        
        setupUI()
        
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
    
    private fun setupUI() {
        // 设置默认值
        binding.etSalaryItemAmount.setText("0")
        binding.cbIncludeTax.isChecked = true
        binding.cbIncludeSocialSecurity.isChecked = false
        
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        // 确认按钮
        binding.btnConfirm.setOnClickListener {
            addSalaryItem()
        }
    }
    
    private fun addSalaryItem() {
        val name = binding.etSalaryItemName.text.toString().trim()
        val amountText = binding.etSalaryItemAmount.text.toString().trim()
        val includeTax = binding.cbIncludeTax.isChecked
        val includeSocialSecurity = binding.cbIncludeSocialSecurity.isChecked
        
        // 验证输入
        if (name.isEmpty()) {
            showError("请输入工资项名称")
            return
        }
        
        if (name.length > 10) {
            showError("工资项名称不能超过10个字符")
            return
        }
        
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount < 0) {
            showError("请输入有效的金额（不能为负数）")
            return
        }
        
        // 回调添加工资项
        onSalaryItemAdded?.invoke(name, amount, includeTax, includeSocialSecurity)
        
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
