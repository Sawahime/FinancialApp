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

                _grossSalary.value = if (currentSettings.grossSalary > 0) currentSettings.grossSalary.toString() else ""
                _personalSocialInsuranceRate.value = (currentSettings.personalSocialInsuranceRate * 100).toString()
                _companySocialInsuranceRate.value = (currentSettings.companySocialInsuranceRate * 100).toString()
                _personalHousingFundRate.value = (currentSettings.personalHousingFundRate * 100).toString()
                _companyHousingFundRate.value = (currentSettings.companyHousingFundRate * 100).toString()

            } catch (e: Exception) {
                android.util.Log.e("SimpleSettingsViewModel", "加载设置失败", e)
                // 设置默认值为0
                _grossSalary.value = "0"
                _personalSocialInsuranceRate.value = "0"
                _companySocialInsuranceRate.value = "0"
                _personalHousingFundRate.value = "0"
                _companyHousingFundRate.value = "0"
            }
        }
    }
    
    fun updateGrossSalary(salary: String) {
        _grossSalary.value = salary
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
                    // 验证输入
                    val salary = _grossSalary.value.toDoubleOrNull()
                    if (salary == null || salary < 0) {
                        throw IllegalArgumentException("请输入有效的工资金额（可以为0）")
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
                    android.util.Log.d("SimpleSettingsViewModel", "保存工资: ${salary}")

                    repository.saveSettings(
                        year = _currentYear.value,
                        month = _currentMonth.value,
                        grossSalary = salary,
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

    fun modifyHistoricalSettings(year: Int, month: Int, salary: Double, personalSocialRate: Double,
                                companySocialRate: Double, personalHousingRate: Double, companyHousingRate: Double) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                withContext(Dispatchers.Default) {
                    repository.saveSettings(
                        year = year,
                        month = month,
                        grossSalary = salary,
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
