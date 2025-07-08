package com.example.financialapp

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.financialapp.databinding.DialogDeleteSalaryItemBinding

/**
 * 删除工资项确认对话框
 */
class DeleteSalaryItemDialog : DialogFragment() {
    
    private var _binding: DialogDeleteSalaryItemBinding? = null
    private val binding get() = _binding!!
    
    private var itemName: String = ""
    private var onDeleteConfirmed: ((Boolean) -> Unit)? = null
    
    companion object {
        fun newInstance(
            itemName: String,
            onDeleteConfirmed: (skipFutureConfirmation: Boolean) -> Unit
        ): DeleteSalaryItemDialog {
            return DeleteSalaryItemDialog().apply {
                this.itemName = itemName
                this.onDeleteConfirmed = onDeleteConfirmed
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDeleteSalaryItemBinding.inflate(LayoutInflater.from(context))
        
        setupUI()
        
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }
    
    private fun setupUI() {
        // 设置删除消息
        binding.tvDeleteMessage.text = "确定要删除工资项「${itemName}」吗？"
        
        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        // 删除按钮
        binding.btnDelete.setOnClickListener {
            val skipFutureConfirmation = binding.cbDontAskAgain.isChecked
            onDeleteConfirmed?.invoke(skipFutureConfirmation)
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
