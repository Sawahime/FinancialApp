package com.example.financialapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.financialapp.databinding.DialogAddExpenseBinding
import com.example.financialapp.databinding.DialogAddOtherIncomeBinding
import com.example.financialapp.databinding.DialogRecordsManagementBinding
import com.example.financialapp.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SimpleHomeViewModel by viewModels {
        SimpleHomeViewModel.Factory((requireActivity().application as FinancialApplication).simpleRepository)
    }

    private val monthFormat = SimpleDateFormat("yyyy年M月", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 延迟设置，避免阻塞主线程
        view.post {
            setupUI()
            observeViewModel()
        }
    }

    private fun setupUI() {
        // 设置工资详情展开按钮
        binding.llSalaryHeader.setOnClickListener {
            viewModel.toggleSalaryDetailsExpanded()
        }

        // 设置社保&公积金展开按钮
        binding.llSocialInsuranceHeader.setOnClickListener {
            toggleSocialInsuranceDetails()
        }

        binding.btnAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        binding.btnAddOtherIncome.setOnClickListener {
            showAddOtherIncomeDialog()
        }

        binding.btnViewRecords.setOnClickListener {
            showRecordsManagementDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {


                // 观察财务数据
                launch {
                    viewModel.financialData.collect { financialData ->
                        updateFinanceDisplay(financialData)
                    }
                }

                // 观察工资详情展开状态
                launch {
                    viewModel.salaryDetailsExpanded.collect { expanded ->
                        updateSalaryDetailsVisibility(expanded)
                    }
                }

                // 观察工资项变化
                launch {
                    viewModel.salaryItems.collect { salaryItems ->
                        updateSalaryDetailsContent(salaryItems)
                    }
                }

                // 观察错误消息
                launch {
                    viewModel.errorMessage.collect { errorMessage ->
                        errorMessage?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearErrorMessage()
                        }
                    }
                }

                // 观察加载状态
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        // 可以在这里显示/隐藏加载指示器
                    }
                }
            }
        }
    }



    private fun updateFinanceDisplay(financialData: FinancialData?) {
        if (financialData == null) {
            // 显示默认值或提示用户设置工资
            binding.tvSalary.text = "税前工资：请先设置工资"
            binding.tvSocialInsuranceAndHousingFund.text = "社保&公积金：--"
            binding.tvSocialInsurance.text = "社保缴纳：--"
            binding.tvHousingFund.text = "公积金：--"
            binding.tvTax.text = "个人所得税：--"
            binding.tvNetIncome.text = "实际收入：--"
            return
        }

        // 计算社保&公积金总额
        val totalSocialAndHousing = financialData.socialInsurance + financialData.housingFund

        // 更新UI显示
        binding.tvSalary.text = "税前工资：${currencyFormat.format(financialData.grossSalary)}"
        binding.tvSocialInsuranceAndHousingFund.text = "社保&公积金：${currencyFormat.format(totalSocialAndHousing)}"
        binding.tvSocialInsurance.text = "社保缴纳：${currencyFormat.format(financialData.socialInsurance)}"
        binding.tvHousingFund.text = "公积金：${currencyFormat.format(financialData.housingFund)}"
        binding.tvTax.text = "个人所得税：${currencyFormat.format(financialData.tax)}"
        binding.tvNetIncome.text = "实际收入：${currencyFormat.format(financialData.netIncome)}"

        // 更新支出和其他收入
        binding.tvExpenses.text = "本月支出：${currencyFormat.format(financialData.expenses)}"
        binding.tvOtherIncome.text = "其他收入：${currencyFormat.format(financialData.otherIncome)}"

        // 计算月度结余（实际收入 + 其他收入 - 支出）
        val netBalance = financialData.netIncome + financialData.otherIncome - financialData.expenses
        binding.tvNetBalance.text = "月度结余：${currencyFormat.format(netBalance)}"
    }

    /**
     * 切换社保&公积金详情的展开/收起状态
     */
    private fun toggleSocialInsuranceDetails() {
        val detailsLayout = binding.llSocialInsuranceDetails
        val expandIcon = binding.ivSocialInsuranceExpand

        if (detailsLayout.visibility == View.GONE) {
            // 展开
            detailsLayout.visibility = View.VISIBLE
            expandIcon.animate().rotation(180f).setDuration(200).start()
        } else {
            // 收起
            detailsLayout.visibility = View.GONE
            expandIcon.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            // 关闭输入法
            try {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(dialogBinding.root.windowToken, 0)
            } catch (e: Exception) {
                // 忽略输入法相关的异常
            }
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val amountText = dialogBinding.etAmount.text.toString().trim()
            val category = dialogBinding.etCategory.text.toString().trim()
            val description = dialogBinding.etDescription.text.toString().trim()

            if (amountText.isEmpty()) {
                dialogBinding.etAmount.error = "请输入支出金额"
                return@setOnClickListener
            }

            if (category.isEmpty()) {
                dialogBinding.etCategory.error = "请输入支出类别"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                dialogBinding.etAmount.error = "请输入有效的金额"
                return@setOnClickListener
            }

            viewModel.addExpenseRecord(
                amount = amount,
                category = category,
                description = description.ifEmpty { null }
            )

            // 关闭输入法
            try {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(dialogBinding.root.windowToken, 0)
            } catch (e: Exception) {
                // 忽略输入法相关的异常
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddOtherIncomeDialog() {
        val dialogBinding = DialogAddOtherIncomeBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            // 关闭输入法
            try {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(dialogBinding.root.windowToken, 0)
            } catch (e: Exception) {
                // 忽略输入法相关的异常
            }
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val amountText = dialogBinding.etAmount.text.toString().trim()
            val description = dialogBinding.etDescription.text.toString().trim()

            if (amountText.isEmpty()) {
                dialogBinding.etAmount.error = "请输入收入金额"
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                dialogBinding.etAmount.error = "请输入有效的金额"
                return@setOnClickListener
            }

            viewModel.addOtherIncome(
                amount = amount,
                description = description.ifEmpty { null }
            )

            // 关闭输入法
            try {
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(dialogBinding.root.windowToken, 0)
            } catch (e: Exception) {
                // 忽略输入法相关的异常
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRecordsManagementDialog() {
        val dialogBinding = DialogRecordsManagementBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // 设置RecyclerView
        dialogBinding.recyclerViewRecords.layoutManager = LinearLayoutManager(requireContext())

        // 初始显示支出记录
        showExpenseRecords(dialogBinding)

        // Tab切换监听
        dialogBinding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showExpenseRecords(dialogBinding)
                    1 -> showIncomeRecords(dialogBinding)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showExpenseRecords(dialogBinding: DialogRecordsManagementBinding) {
        val records = viewModel.getExpenseRecords()
        if (records.isEmpty()) {
            dialogBinding.recyclerViewRecords.visibility = View.GONE
            dialogBinding.tvEmptyState.visibility = View.VISIBLE
            dialogBinding.tvEmptyState.text = "暂无支出记录"
        } else {
            dialogBinding.recyclerViewRecords.visibility = View.VISIBLE
            dialogBinding.tvEmptyState.visibility = View.GONE

            val adapter = ExpenseRecordsAdapter(records) { recordId ->
                // 删除记录
                viewModel.deleteExpenseRecord(recordId)
                showExpenseRecords(dialogBinding) // 刷新列表
            }
            dialogBinding.recyclerViewRecords.adapter = adapter
        }
    }

    private fun showIncomeRecords(dialogBinding: DialogRecordsManagementBinding) {
        val records = viewModel.getOtherIncomeRecords()
        if (records.isEmpty()) {
            dialogBinding.recyclerViewRecords.visibility = View.GONE
            dialogBinding.tvEmptyState.visibility = View.VISIBLE
            dialogBinding.tvEmptyState.text = "暂无收入记录"
        } else {
            dialogBinding.recyclerViewRecords.visibility = View.VISIBLE
            dialogBinding.tvEmptyState.visibility = View.GONE

            val adapter = IncomeRecordsAdapter(records) { recordId ->
                // 删除记录
                viewModel.deleteOtherIncomeRecord(recordId)
                showIncomeRecords(dialogBinding) // 刷新列表
            }
            dialogBinding.recyclerViewRecords.adapter = adapter
        }
    }

    override fun onPause() {
        super.onPause()
        // 在暂停时主动关闭输入法
        try {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.root.windowToken, android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS)
        } catch (e: Exception) {
            android.util.Log.w("HomeFragment", "关闭输入法失败", e)
        }
    }

    /**
     * 更新工资详情可见性
     */
    private fun updateSalaryDetailsVisibility(expanded: Boolean) {
        binding.llSalaryDetails.visibility = if (expanded) View.VISIBLE else View.GONE
        binding.ivSalaryExpand.setImageResource(
            if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        )
    }

    /**
     * 更新工资详情内容
     */
    private fun updateSalaryDetailsContent(salaryItems: SalaryItemCollection) {
        // 清空现有内容
        binding.llSalaryDetails.removeAllViews()

        // 为每个工资项添加详情行
        for (item in salaryItems.sortedItems) {
            if (item.amount > 0) {  // 只显示有金额的工资项
                addSalaryDetailItem(item)
            }
        }
    }

    /**
     * 添加单个工资项详情
     */
    private fun addSalaryDetailItem(item: SalaryItem) {
        val textView = TextView(requireContext()).apply {
            text = "  • ${item.name}：${item.amount} 元"
            textSize = 14f
            setTextColor(resources.getColor(R.color.md_theme_on_surface_variant, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4
            }
        }
        binding.llSalaryDetails.addView(textView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 确保输入法正确关闭
        try {
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.root.windowToken, android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS)
        } catch (e: Exception) {
            android.util.Log.w("HomeFragment", "销毁时关闭输入法失败", e)
        }
        _binding = null
    }
}