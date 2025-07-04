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
        // 设置文本监听器
        binding.etGrossSalary.addTextChangedListener(createTextWatcher { text ->
            viewModel.updateGrossSalary(text)
        })

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


                // 观察各个字段的值
                launch {
                    viewModel.grossSalary.collect { salary ->
                        if (binding.etGrossSalary.text.toString() != salary) {
                            binding.etGrossSalary.setText(salary)
                        }
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