package com.example.financialapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financialapp.databinding.DialogSettingsHistoryBinding
import com.example.financialapp.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SimpleSettingsViewModel by viewModels {
        SimpleSettingsViewModel.Factory((requireActivity().application as FinancialApplication).simpleRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            setupUI()
            observeViewModel()
        }
    }

    private fun setupUI() {
        // 设置添加工资项按钮
        binding.ivAddSalaryItem.setOnClickListener {
            showAddSalaryItemDialog()
        }

        binding.etPersonalSocialInsuranceRate.addTextChangedListener(createTextWatcher { text ->
            viewModel.updatePersonalSocialInsuranceRate(text)
        })

        binding.etCompanySocialInsuranceRate.addTextChangedListener(createTextWatcher { text ->
            viewModel.updateCompanySocialInsuranceRate(text)
        })

        binding.etPersonalHousingFundRate.addTextChangedListener(createTextWatcher { text ->
            viewModel.updatePersonalHousingFundRate(text)
        })

        binding.etCompanyHousingFundRate.addTextChangedListener(createTextWatcher { text ->
            viewModel.updateCompanyHousingFundRate(text)
        })

        // 设置按钮监听器
        binding.btnSave.setOnClickListener {
            viewModel.saveSettings()
        }

        binding.btnViewHistory.setOnClickListener {
            showSettingsHistoryDialog()
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetToDefaults()
        }
    }

    /**
     * 显示添加工资项对话框
     */
    private fun showAddSalaryItemDialog() {
        val dialog = AddSalaryItemDialog.newInstance { name, amount, includeTax, includeSocialSecurity ->
            viewModel.addSalaryItem(name, amount, includeTax, includeSocialSecurity)
        }
        dialog.show(parentFragmentManager, "AddSalaryItemDialog")
    }

    /**
     * 显示删除工资项确认对话框
     */
    private fun showDeleteSalaryItemDialog(item: SalaryItem) {
        if (viewModel.shouldSkipDeleteConfirmation()) {
            // 直接删除
            deleteSalaryItem(item.id)
        } else {
            // 显示确认对话框
            val dialog = DeleteSalaryItemDialog.newInstance(item.name) { skipFutureConfirmation ->
                if (skipFutureConfirmation) {
                    viewModel.setSkipDeleteConfirmation(true)
                }
                deleteSalaryItem(item.id)
            }
            dialog.show(parentFragmentManager, "DeleteSalaryItemDialog")
        }
    }

    /**
     * 删除工资项
     */
    private fun deleteSalaryItem(itemId: String) {
        val success = viewModel.removeSalaryItem(itemId)
        if (!success) {
            // 显示错误消息
            android.widget.Toast.makeText(context, "无法删除该工资项", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 显示编辑工资项对话框
     */
    private fun showEditSalaryItemDialog(item: SalaryItem) {
        val dialog = EditSalaryItemDialog.newInstance(item) { updatedItem ->
            viewModel.updateSalaryItemAmount(updatedItem.id, updatedItem.amount)
            // 如果基数设置有变化，需要重新保存整个工资项集合
            // 这里简化处理，只更新金额
        }
        dialog.show(parentFragmentManager, "EditSalaryItemDialog")
    }

    // 记录当前工资项的ID列表，用于检测结构变化
    private var currentSalaryItemIds = emptyList<String>()

    /**
     * 更新工资项UI
     */
    private fun updateSalaryItemsUI(salaryItems: SalaryItemCollection) {
        val newItemIds = salaryItems.sortedItems.map { it.id }

        // 检查是否需要重新创建UI（工资项数量或顺序发生变化）
        if (newItemIds != currentSalaryItemIds) {
            // 工资项结构发生变化，重新创建UI
            binding.llSalaryItems.removeAllViews()

            for (item in salaryItems.sortedItems) {
                addSalaryItemView(item)
            }

            currentSalaryItemIds = newItemIds
        } else {
            // 只是金额变化，更新现有的输入框值（如果需要）
            updateExistingSalaryItemValues(salaryItems)
        }

        // 更新总计显示
        binding.tvTotalSalary.text = "总工资：${salaryItems.totalSalary} 元"
        binding.tvTaxableBase.text = "纳税基数：${salaryItems.taxableBase} 元"
        binding.tvSocialSecurityBase.text = "社保基数：${salaryItems.socialSecurityBase} 元"
    }

    /**
     * 更新现有工资项的值（不重新创建UI）
     */
    private fun updateExistingSalaryItemValues(salaryItems: SalaryItemCollection) {
        // 暂时禁用更新，避免在用户输入时干扰
        // 这个方法主要用于结构变化后的同步，而不是实时更新
        // 实时更新由TextWatcher直接处理
    }

    /**
     * 为单个工资项创建视图
     */
    private fun addSalaryItemView(item: SalaryItem) {
        val itemView = layoutInflater.inflate(R.layout.item_salary_item, binding.llSalaryItems, false)

        // 获取视图组件
        val tilSalaryItem = itemView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSalaryItem)
        val etAmount = itemView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSalaryItemAmount)
        val ivDelete = itemView.findViewById<android.widget.ImageView>(R.id.ivDeleteSalaryItem)

        // 设置工资项名称和金额
        tilSalaryItem.hint = "${item.name}（元）"
        // 格式化金额显示，避免不必要的小数点
        val amountText = if (item.amount > 0) {
            if (item.amount == item.amount.toInt().toDouble()) {
                item.amount.toInt().toString()  // 整数显示为整数
            } else {
                item.amount.toString()  // 小数显示为小数
            }
        } else {
            ""  // 0或负数显示为空
        }
        etAmount.setText(amountText)

        // 设置删除按钮可见性（默认项目不显示删除按钮）
        ivDelete.visibility = if (item.isDefault) android.view.View.GONE else android.view.View.VISIBLE

        // 设置标签以便后续更新
        itemView.tag = item.id

        // 设置金额变化监听器
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s?.toString() ?: ""
                val amount = text.toDoubleOrNull() ?: 0.0
                viewModel.updateSalaryItemAmount(item.id, amount)
            }
        })

        // 设置删除按钮点击监听器
        ivDelete.setOnClickListener {
            showDeleteSalaryItemDialog(item)
        }

        // 设置点击编辑监听器
        itemView.setOnClickListener {
            showEditSalaryItemDialog(item)
        }

        // 添加到容器
        binding.llSalaryItems.addView(itemView)
    }



    private fun showSettingsHistoryDialog() {
        val dialogBinding = DialogSettingsHistoryBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // 设置RecyclerView
        dialogBinding.recyclerViewHistory.layoutManager = LinearLayoutManager(requireContext())

        // 获取设置历史
        val history = viewModel.getSettingsHistory()
        if (history.isEmpty()) {
            dialogBinding.recyclerViewHistory.visibility = View.GONE
            dialogBinding.tvEmptyState.visibility = View.VISIBLE
        } else {
            dialogBinding.recyclerViewHistory.visibility = View.VISIBLE
            dialogBinding.tvEmptyState.visibility = View.GONE

            val adapter = SettingsHistoryAdapter(history) { record ->
                // 点击记录，显示修改对话框
                showModifyHistoricalSettingsDialog(record)
                dialog.dismiss()
            }
            dialogBinding.recyclerViewHistory.adapter = adapter
        }

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showModifyHistoricalSettingsDialog(record: SalarySettingsRecord) {
        // 创建一个简单的输入对话框来修改历史设置
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("修改历史设置")
        builder.setMessage("功能开发中，敬请期待！\n\n当前记录：\n月薪：¥${record.grossSalary.toInt()}\n社保：个人${(record.personalSocialInsuranceRate * 100).toInt()}% 公司${(record.companySocialInsuranceRate * 100).toInt()}%\n公积金：个人${(record.personalHousingFundRate * 100).toInt()}% 公司${(record.companyHousingFundRate * 100).toInt()}%")
        builder.setPositiveButton("确定", null)
        builder.show()
    }

    // 供MainActivity调用的月份变更通知方法
    fun onMonthChanged(year: Int, month: Int) {
        viewModel.setCurrentYearMonth(year, month)
        android.util.Log.d("SettingsFragment", "收到月份变更通知: ${year}年${month}月")
    }



    private fun createTextWatcher(onTextChanged: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged(s?.toString() ?: "")
            }
        }
    }


    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {


                // 观察工资项集合变化
                launch {
                    viewModel.salaryItems.collect { salaryItems ->
                        updateSalaryItemsUI(salaryItems)
                    }
                }

                launch {
                    viewModel.personalSocialInsuranceRate.collect { rate ->
                        if (binding.etPersonalSocialInsuranceRate.text.toString() != rate) {
                            binding.etPersonalSocialInsuranceRate.setText(rate)
                        }
                    }
                }

                launch {
                    viewModel.companySocialInsuranceRate.collect { rate ->
                        if (binding.etCompanySocialInsuranceRate.text.toString() != rate) {
                            binding.etCompanySocialInsuranceRate.setText(rate)
                        }
                    }
                }

                launch {
                    viewModel.personalHousingFundRate.collect { rate ->
                        if (binding.etPersonalHousingFundRate.text.toString() != rate) {
                            binding.etPersonalHousingFundRate.setText(rate)
                        }
                    }
                }

                launch {
                    viewModel.companyHousingFundRate.collect { rate ->
                        if (binding.etCompanyHousingFundRate.text.toString() != rate) {
                            binding.etCompanyHousingFundRate.setText(rate)
                        }
                    }
                }

                // 观察加载状态
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.btnSave.isEnabled = !isLoading
                        binding.btnReset.isEnabled = !isLoading
                    }
                }

                // 观察错误消息
                launch {
                    viewModel.errorMessage.collect { errorMessage ->
                        errorMessage?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearMessages()
                        }
                    }
                }

                // 观察成功消息
                launch {
                    viewModel.successMessage.collect { successMessage ->
                        successMessage?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearMessages()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 在暂停时主动关闭输入法
        try {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.root.windowToken, android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS)
        } catch (e: Exception) {
            android.util.Log.w("SettingsFragment", "关闭输入法失败", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 确保输入法正确关闭
        try {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.root.windowToken, android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS)
        } catch (e: Exception) {
            android.util.Log.w("SettingsFragment", "销毁时关闭输入法失败", e)
        }
        _binding = null
    }
}