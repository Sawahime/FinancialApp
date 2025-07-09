package com.example.financialapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 简化的主页ViewModel
 */
class SimpleHomeViewModel(private val repository: SimpleFinancialRepository) : ViewModel() {
    
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()
    
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()
    
    private val _financialData = MutableStateFlow<FinancialData?>(null)
    val financialData: StateFlow<FinancialData?> = _financialData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 工资项详情展开状态
    private val _salaryDetailsExpanded = MutableStateFlow(false)
    val salaryDetailsExpanded: StateFlow<Boolean> = _salaryDetailsExpanded.asStateFlow()

    // 当前工资项集合
    val salaryItems: StateFlow<SalaryItemCollection> = repository.salaryItems
    
    init {
        // 设置初始月份
        repository.setCurrentYearMonth(_currentYear.value, _currentMonth.value)

        // 延迟初始化，避免主线程阻塞
        viewModelScope.launch {
            // 使用Dispatchers.Default进行计算密集型操作
            delay(200) // 给UI初始化更多时间

            // 分别监听数据变更和设置变更
            launch {
                combine(
                    repository.grossSalary,
                    repository.socialInsuranceRate,
                    repository.housingFundRate,
                    repository.expenses,
                    repository.otherIncome
                ) { _, _, _, _, _ ->
                    // 在后台线程计算
                    withContext(Dispatchers.Default) {
                        repository.calculateFinancialData()
                    }
                }.collect { data ->
                    _financialData.value = data
                    android.util.Log.d("HomeViewModel", "财务数据已更新(数据变更): 工资=${data.grossSalary}, 净收入=${data.netIncome}")
                }
            }

            // 单独监听设置变更
            launch {
                repository.settingsChanged.collect { _ ->
                    // 设置变更时重新计算财务数据
                    val data = withContext(Dispatchers.Default) {
                        repository.calculateFinancialData()
                    }
                    _financialData.value = data
                    android.util.Log.d("HomeViewModel", "财务数据已更新(设置变更): 工资=${data.grossSalary}, 净收入=${data.netIncome}")
                }
            }
        }
    }
    
    fun setCurrentYearMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month

        // 同步到Application（全局状态）
        if (repository is SimpleFinancialRepository) {
            // 这里需要通过其他方式获取Application实例
            // 暂时先直接更新repository
        }

        // 通知repository切换到新月份
        repository.setCurrentYearMonth(year, month)

        // 重新计算财务数据（因为设置可能不同）
        viewModelScope.launch {
            delay(100) // 等待repository更新完成
            val data = withContext(Dispatchers.Default) {
                repository.calculateFinancialData()
            }
            _financialData.value = data
        }
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

    /**
     * 切换工资详情展开状态
     */
    fun toggleSalaryDetailsExpanded() {
        _salaryDetailsExpanded.value = !_salaryDetailsExpanded.value
        android.util.Log.d("HomeViewModel", "工资详情展开状态: ${_salaryDetailsExpanded.value}")
    }
    
    fun addExpenseRecord(amount: Double, category: String, description: String? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.addExpenseRecord(_currentYear.value, _currentMonth.value, amount, category, description)
                }
            } catch (e: Exception) {
                _errorMessage.value = "添加支出记录失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "添加支出失败", e)
            }
        }
    }
    
    fun addOtherIncome(amount: Double, description: String? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.addOtherIncomeRecord(_currentYear.value, _currentMonth.value, amount, description)
                }
            } catch (e: Exception) {
                _errorMessage.value = "添加其他收入失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "添加其他收入失败", e)
            }
        }
    }

    /**
     * 为指定日期添加支出记录
     */
    fun addExpenseRecordForDate(year: Int, month: Int, amount: Double, category: String, description: String? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.addExpenseRecord(year, month, amount, category, description)
                }
                android.util.Log.d("SimpleHomeViewModel", "添加支出记录到${year}年${month}月: ${amount}元")
            } catch (e: Exception) {
                _errorMessage.value = "添加支出记录失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "添加支出失败", e)
            }
        }
    }

    /**
     * 为指定日期添加收入记录
     */
    fun addOtherIncomeForDate(year: Int, month: Int, amount: Double, description: String? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.addOtherIncomeRecord(year, month, amount, description)
                }
                android.util.Log.d("SimpleHomeViewModel", "添加收入记录到${year}年${month}月: ${amount}元")
            } catch (e: Exception) {
                _errorMessage.value = "添加其他收入失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "添加其他收入失败", e)
            }
        }
    }




    
    fun getExpenseRecords(): List<ExpenseRecord> {
        return repository.getExpenseRecords(_currentYear.value, _currentMonth.value)
    }

    fun getOtherIncomeRecords(): List<IncomeRecord> {
        return repository.getOtherIncomeRecords(_currentYear.value, _currentMonth.value)
    }

    /**
     * 获取指定日期的支出记录
     */
    fun getExpenseRecordsForDate(year: Int, month: Int): List<ExpenseRecord> {
        return repository.getExpenseRecords(year, month)
    }

    /**
     * 获取指定日期的收入记录
     */
    fun getOtherIncomeRecordsForDate(year: Int, month: Int): List<IncomeRecord> {
        return repository.getOtherIncomeRecords(year, month)
    }

    fun deleteExpenseRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.deleteExpenseRecord(_currentYear.value, _currentMonth.value, recordId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除支出记录失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "删除支出失败", e)
            }
        }
    }

    fun deleteOtherIncomeRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.deleteOtherIncomeRecord(_currentYear.value, _currentMonth.value, recordId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除收入记录失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "删除收入失败", e)
            }
        }
    }

    /**
     * 删除指定日期的支出记录
     */
    fun deleteExpenseRecordForDate(year: Int, month: Int, recordId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.deleteExpenseRecord(year, month, recordId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除支出记录失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "删除支出失败", e)
            }
        }
    }

    /**
     * 删除指定日期的收入记录
     */
    fun deleteOtherIncomeRecordForDate(year: Int, month: Int, recordId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    repository.deleteOtherIncomeRecord(year, month, recordId)
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除收入记录失败: ${e.message}"
                android.util.Log.e("SimpleHomeViewModel", "删除收入失败", e)
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    class Factory(private val repository: SimpleFinancialRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SimpleHomeViewModel::class.java)) {
                return SimpleHomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
