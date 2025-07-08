package com.example.financialapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 设置页面ViewModel
 */
class SimpleSettingsViewModel(private val repository: SimpleFinancialRepository) : ViewModel() {

    // 当前显示的年月
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // 工资项集合状态
    private val _salaryItems = MutableStateFlow(SalaryItemCollection.createDefault())
    val salaryItems: StateFlow<SalaryItemCollection> = _salaryItems.asStateFlow()

    // 向后兼容的属性（保留用于调试）
    private val _basicSalary = MutableStateFlow("")
    val basicSalary: StateFlow<String> = _basicSalary.asStateFlow()

    private val _allowance = MutableStateFlow("")
    val allowance: StateFlow<String> = _allowance.asStateFlow()

    private val _grossSalary = MutableStateFlow("")
    val grossSalary: StateFlow<String> = _grossSalary.asStateFlow()
    
    private val _personalSocialInsuranceRate = MutableStateFlow("")
    val personalSocialInsuranceRate: StateFlow<String> = _personalSocialInsuranceRate.asStateFlow()
    
    private val _companySocialInsuranceRate = MutableStateFlow("")
    val companySocialInsuranceRate: StateFlow<String> = _companySocialInsuranceRate.asStateFlow()
    
    private val _personalHousingFundRate = MutableStateFlow("")
    val personalHousingFundRate: StateFlow<String> = _personalHousingFundRate.asStateFlow()
    
    private val _companyHousingFundRate = MutableStateFlow("")
    val companyHousingFundRate: StateFlow<String> = _companyHousingFundRate.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        loadCurrentSettings()
    }

    /**
     * 设置当前年月
     */
    fun setCurrentYearMonth(year: Int, month: Int) {
        android.util.Log.d("SettingsViewModel", "=== 接收月份变更通知 ===")
        android.util.Log.d("SettingsViewModel", "从${_currentYear.value}年${_currentMonth.value}月切换到${year}年${month}月")

        _currentYear.value = year
        _currentMonth.value = month

        // 通知repository切换月份
        repository.setCurrentYearMonth(year, month)

        // 重新加载该月份的设置
        loadCurrentSettings()

        android.util.Log.d("SettingsViewModel", "月份切换完成，已重新加载设置")
    }

    fun goToPreviousMonth() {
        val calendar = Calendar.getInstance().apply {
            set(_currentYear.value, _currentMonth.value - 1, 1)
            add(Calendar.MONTH, -1)
        }
        setCurrentYearMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    fun goToNextMonth() {
        val calendar = Calendar.getInstance().apply {
            set(_currentYear.value, _currentMonth.value - 1, 1)
            add(Calendar.MONTH, 1)
        }
        setCurrentYearMonth(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }
    
    private fun loadCurrentSettings() {
        viewModelScope.launch {
            try {
                // 获取当前显示月份的有效设置
                val currentSettings = repository.getEffectiveSettingsForMonth(_currentYear.value, _currentMonth.value)

                // 更新工资项集合
                _salaryItems.value = currentSettings.salaryItems

                // 向后兼容的属性更新
                _basicSalary.value = if (currentSettings.basicSalary > 0) currentSettings.basicSalary.toString() else ""
                _allowance.value = if (currentSettings.allowance > 0) currentSettings.allowance.toString() else ""
                _grossSalary.value = if (currentSettings.grossSalary > 0) currentSettings.grossSalary.toString() else ""
                _personalSocialInsuranceRate.value = if (currentSettings.personalSocialInsuranceRate > 0) (currentSettings.personalSocialInsuranceRate * 100).toString() else ""
                _companySocialInsuranceRate.value = if (currentSettings.companySocialInsuranceRate > 0) (currentSettings.companySocialInsuranceRate * 100).toString() else ""
                _personalHousingFundRate.value = if (currentSettings.personalHousingFundRate > 0) (currentSettings.personalHousingFundRate * 100).toString() else ""
                _companyHousingFundRate.value = if (currentSettings.companyHousingFundRate > 0) (currentSettings.companyHousingFundRate * 100).toString() else ""

            } catch (e: Exception) {
                android.util.Log.e("SimpleSettingsViewModel", "加载设置失败", e)
                // 设置默认值为空
                _basicSalary.value = ""
                _allowance.value = ""
                _grossSalary.value = ""
                _personalSocialInsuranceRate.value = ""
                _companySocialInsuranceRate.value = ""
                _personalHousingFundRate.value = ""
                _companyHousingFundRate.value = ""
            }
        }
    }
    
    fun updateBasicSalary(value: String) {
        _basicSalary.value = value
        updateGrossSalary()
    }

    fun updateAllowance(value: String) {
        _allowance.value = value
        updateGrossSalary()
    }

    private fun updateGrossSalary() {
        val basic = _basicSalary.value.toDoubleOrNull() ?: 0.0
        val allowance = _allowance.value.toDoubleOrNull() ?: 0.0
        _grossSalary.value = (basic + allowance).toString()
    }

    /**
     * 添加工资项
     */
    fun addSalaryItem(name: String, amount: Double, includeTax: Boolean, includeSocialSecurity: Boolean) {
        val newItem = SalaryItem.createCustomItem(
            name = name,
            amount = amount,
            includeTax = includeTax,
            includeSocialSecurity = includeSocialSecurity
        )

        val currentItems = _salaryItems.value
        val newItems = currentItems.addItem(newItem)
        _salaryItems.value = newItems

        // 同步到Repository
        repository.addSalaryItem(newItem)

        android.util.Log.d("SettingsViewModel", "添加工资项: ${name}, 金额=${amount}")
    }

    /**
     * 删除工资项
     */
    fun removeSalaryItem(itemId: String): Boolean {
        val currentItems = _salaryItems.value
        val itemToRemove = currentItems.items.find { it.id == itemId }

        if (itemToRemove?.isDefault == true) {
            android.util.Log.w("SettingsViewModel", "不能删除默认工资项")
            return false
        }

        val success = repository.removeSalaryItem(itemId)
        if (success) {
            val newItems = currentItems.removeItem(itemId)
            _salaryItems.value = newItems
            android.util.Log.d("SettingsViewModel", "删除工资项: ${itemToRemove?.name}")
        }

        return success
    }

    /**
     * 更新工资项金额
     */
    fun updateSalaryItemAmount(itemId: String, amount: Double) {
        val currentItems = _salaryItems.value
        val currentItem = currentItems.items.find { it.id == itemId }

        // 只在金额确实发生变化时才更新
        if (currentItem?.amount != amount) {
            val newItems = currentItems.updateItem(itemId, amount)
            _salaryItems.value = newItems

            // 同步到Repository
            repository.updateSalaryItemAmount(itemId, amount)

            android.util.Log.d("SettingsViewModel", "更新工资项金额: ${itemId} = ${amount}")
        }
    }

    /**
     * 获取删除确认偏好
     */
    fun shouldSkipDeleteConfirmation(): Boolean {
        return repository.shouldSkipDeleteConfirmation()
    }

    /**
     * 设置删除确认偏好
     */
    fun setSkipDeleteConfirmation(skip: Boolean) {
        repository.setSkipDeleteConfirmation(skip)
    }
    
    fun updatePersonalSocialInsuranceRate(rate: String) {
        _personalSocialInsuranceRate.value = rate
    }
    
    fun updateCompanySocialInsuranceRate(rate: String) {
        _companySocialInsuranceRate.value = rate
    }
    
    fun updatePersonalHousingFundRate(rate: String) {
        _personalHousingFundRate.value = rate
    }
    
    fun updateCompanyHousingFundRate(rate: String) {
        _companyHousingFundRate.value = rate
    }
    
    fun saveSettings() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                withContext(Dispatchers.Default) {
                    // 验证工资项输入
                    val currentSalaryItems = _salaryItems.value

                    // 验证所有工资项金额都有效
                    for (item in currentSalaryItems.items) {
                        if (item.amount < 0) {
                            throw IllegalArgumentException("工资项「${item.name}」的金额不能为负数")
                        }
                    }

                    val personalSocialRate = _personalSocialInsuranceRate.value.toDoubleOrNull()
                    if (personalSocialRate == null || personalSocialRate < 0 || personalSocialRate > 100) {
                        throw IllegalArgumentException("个人社保缴纳比例应在0-100之间")
                    }

                    val companySocialRate = _companySocialInsuranceRate.value.toDoubleOrNull()
                    if (companySocialRate == null || companySocialRate < 0 || companySocialRate > 100) {
                        throw IllegalArgumentException("公司社保缴纳比例应在0-100之间")
                    }

                    val personalHousingRate = _personalHousingFundRate.value.toDoubleOrNull()
                    if (personalHousingRate == null || personalHousingRate < 0 || personalHousingRate > 100) {
                        throw IllegalArgumentException("个人公积金缴纳比例应在0-100之间")
                    }

                    val companyHousingRate = _companyHousingFundRate.value.toDoubleOrNull()
                    if (companyHousingRate == null || companyHousingRate < 0 || companyHousingRate > 100) {
                        throw IllegalArgumentException("公司公积金缴纳比例应在0-100之间")
                    }

                    // 保存设置（在当前显示的月份生效）
                    android.util.Log.d("SimpleSettingsViewModel", "=== 开始保存设置 ===")
                    android.util.Log.d("SimpleSettingsViewModel", "当前ViewModel月份: ${_currentYear.value}年${_currentMonth.value}月")
                    android.util.Log.d("SimpleSettingsViewModel", "保存工资项: 总工资=${currentSalaryItems.totalSalary}, 纳税基数=${currentSalaryItems.taxableBase}, 社保基数=${currentSalaryItems.socialSecurityBase}")

                    repository.saveSettings(
                        year = _currentYear.value,
                        month = _currentMonth.value,
                        salaryItems = currentSalaryItems,
                        personalSocialRate = personalSocialRate / 100,
                        companySocialRate = companySocialRate / 100,
                        personalHousingRate = personalHousingRate / 100,
                        companyHousingRate = companyHousingRate / 100,
                        isHistoricalModification = false
                    )
                }

                _successMessage.value = "设置保存成功！在${_currentYear.value}年${_currentMonth.value}月生效"

                android.util.Log.d("SettingsViewModel", "设置保存完成，应该触发主页财务数据更新")

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "保存设置失败"
                android.util.Log.e("SimpleSettingsViewModel", "保存设置失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetToDefaults() {
        // 锚点系统：重置当前月份的设置
        repository.resetMonthSettings(_currentYear.value, _currentMonth.value)

        // 重新加载设置
        loadCurrentSettings()

        android.util.Log.d("SettingsViewModel", "重置${_currentYear.value}年${_currentMonth.value}月设置")
    }
    
    fun getSettingsHistory(): List<SalarySettingsRecord> {
        return repository.getSettingsHistory()
    }

    fun modifyHistoricalSettings(year: Int, month: Int, basicSalary: Double, allowance: Double, personalSocialRate: Double,
                                companySocialRate: Double, personalHousingRate: Double, companyHousingRate: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                withContext(Dispatchers.Default) {
                    // 创建简单的工资项集合（向后兼容）
                    val salaryItems = SalaryItemCollection(
                        items = listOf(
                            SalaryItem.createBasicSalary(basicSalary),
                            SalaryItem.createAllowance(allowance)
                        )
                    )

                    repository.saveSettings(
                        year = year,
                        month = month,
                        salaryItems = salaryItems,
                        personalSocialRate = personalSocialRate,
                        companySocialRate = companySocialRate,
                        personalHousingRate = personalHousingRate,
                        companyHousingRate = companyHousingRate,
                        isHistoricalModification = true
                    )
                }

                _successMessage.value = "历史数据修改成功！${year}年${month}月的设置已更新"

            } catch (e: Exception) {
                _errorMessage.value = "修改历史数据失败: ${e.message}"
                android.util.Log.e("SimpleSettingsViewModel", "修改历史数据失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
    
    class Factory(private val repository: SimpleFinancialRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SimpleSettingsViewModel::class.java)) {
                return SimpleSettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
