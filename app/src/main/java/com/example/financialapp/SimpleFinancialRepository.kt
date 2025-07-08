package com.example.financialapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 简化的财务数据仓库（内存存储）
 * 用于快速测试应用基本功能
 */
class SimpleFinancialRepository {

    // 工资设置历史（按时间顺序存储）
    private val salarySettingsHistory = mutableListOf<SalarySettingsRecord>()

    // 用户偏好设置
    private var skipDeleteConfirmation = false

    // 当前显示的设置值（用于UI显示）
    private val _salaryItems = MutableStateFlow(SalaryItemCollection.createDefault())
    val salaryItems: StateFlow<SalaryItemCollection> = _salaryItems.asStateFlow()

    // 向后兼容的属性
    private val _basicSalary = MutableStateFlow(0.0)
    val basicSalary: StateFlow<Double> = _basicSalary.asStateFlow()

    private val _allowance = MutableStateFlow(0.0)
    val allowance: StateFlow<Double> = _allowance.asStateFlow()

    private val _grossSalary = MutableStateFlow(0.0)
    val grossSalary: StateFlow<Double> = _grossSalary.asStateFlow()

    private val _socialInsuranceRate = MutableStateFlow(0.08)
    val socialInsuranceRate: StateFlow<Double> = _socialInsuranceRate.asStateFlow()

    private val _housingFundRate = MutableStateFlow(0.12)
    val housingFundRate: StateFlow<Double> = _housingFundRate.asStateFlow()

    // 设置变更通知
    private val _settingsChanged = MutableStateFlow(0L)
    val settingsChanged: StateFlow<Long> = _settingsChanged.asStateFlow()

    // 按月份存储的数据
    private val monthlyExpenses = mutableMapOf<String, Double>()
    private val monthlyOtherIncome = mutableMapOf<String, Double>()
    private val monthlyExpenseRecords = mutableMapOf<String, MutableList<ExpenseRecord>>()
    private val monthlyOtherIncomeRecords = mutableMapOf<String, MutableList<IncomeRecord>>()

    // 当前选中月份的数据流
    private val _currentYearMonth = MutableStateFlow("")
    private val _expenses = MutableStateFlow(0.0)
    val expenses: StateFlow<Double> = _expenses.asStateFlow()

    private val _otherIncome = MutableStateFlow(0.0)
    val otherIncome: StateFlow<Double> = _otherIncome.asStateFlow()
    
    private fun getMonthKey(year: Int, month: Int): String = "${year}-${month.toString().padStart(2, '0')}"

    fun setCurrentYearMonth(year: Int, month: Int) {
        val monthKey = getMonthKey(year, month)
        _currentYearMonth.value = monthKey

        // 检查是否需要自动继承上月设置
        checkAndInheritPreviousMonth(year, month)

        // 更新当前月份的数据流
        _expenses.value = monthlyExpenses[monthKey] ?: 0.0
        _otherIncome.value = monthlyOtherIncome[monthKey] ?: 0.0

        // 更新当前显示的设置值
        updateCurrentDisplayValues()

        // 触发设置变更通知（因为月份切换可能导致有效设置变化）
        _settingsChanged.value = System.currentTimeMillis()

        android.util.Log.d("Repository", "切换到${year}年${month}月")
    }

    /**
     * 锚点系统中不需要自动继承，只在查询时计算
     */
    private fun checkAndInheritPreviousMonth(year: Int, month: Int) {
        // 锚点系统中，月份切换时不自动创建记录
        // 继承逻辑在getEffectiveSettingsForMonth中处理
        android.util.Log.d("Repository", "锚点系统：月份切换到${year}年${month}月，不自动创建记录")
    }

    fun saveSettings(year: Int, month: Int, salaryItems: SalaryItemCollection, personalSocialRate: Double,
                    companySocialRate: Double, personalHousingRate: Double, companyHousingRate: Double,
                    isHistoricalModification: Boolean = false) {

        android.util.Log.d("Repository", "=== 锚点系统：保存设置 ===")
        android.util.Log.d("Repository", "设置${year}年${month}月: 总工资=${salaryItems.totalSalary}, 纳税基数=${salaryItems.taxableBase}, 社保基数=${salaryItems.socialSecurityBase}")

        // 锚点系统：保存设置即创建手动锚点
        createManualAnchor(year, month, salaryItems, personalSocialRate, companySocialRate, personalHousingRate, companyHousingRate)

        // 更新当前显示的工资项
        _salaryItems.value = salaryItems

        // 更新当前显示的值
        updateCurrentDisplayValues()

        // 触发设置变更通知
        _settingsChanged.value = System.currentTimeMillis()

        android.util.Log.d("Repository", "锚点系统：设置保存完成")
        debugPrintAnchors()
    }

    /**
     * 添加工资项
     */
    fun addSalaryItem(item: SalaryItem) {
        val currentItems = _salaryItems.value
        val newItems = currentItems.addItem(item)
        _salaryItems.value = newItems

        android.util.Log.d("Repository", "添加工资项: ${item.name}, 金额=${item.amount}")
    }

    /**
     * 删除工资项
     */
    fun removeSalaryItem(itemId: String): Boolean {
        val currentItems = _salaryItems.value
        val itemToRemove = currentItems.items.find { it.id == itemId }

        if (itemToRemove == null) {
            android.util.Log.w("Repository", "要删除的工资项不存在: $itemId")
            return false
        }

        if (itemToRemove.isDefault) {
            android.util.Log.w("Repository", "不能删除默认工资项: ${itemToRemove.name}")
            return false
        }

        val newItems = currentItems.removeItem(itemId)
        _salaryItems.value = newItems

        android.util.Log.d("Repository", "删除工资项: ${itemToRemove.name}")
        return true
    }

    /**
     * 更新工资项金额
     */
    fun updateSalaryItemAmount(itemId: String, amount: Double) {
        val currentItems = _salaryItems.value
        val newItems = currentItems.updateItem(itemId, amount)
        _salaryItems.value = newItems

        android.util.Log.d("Repository", "更新工资项金额: $itemId = $amount")
    }

    /**
     * 获取删除确认偏好设置
     */
    fun shouldSkipDeleteConfirmation(): Boolean {
        return skipDeleteConfirmation
    }

    /**
     * 设置删除确认偏好
     */
    fun setSkipDeleteConfirmation(skip: Boolean) {
        skipDeleteConfirmation = skip
        android.util.Log.d("Repository", "设置删除确认偏好: $skip")
    }

    private fun updateCurrentDisplayValues() {
        val currentMonthKey = _currentYearMonth.value
        android.util.Log.d("Repository", "更新显示值，当前月份: $currentMonthKey")

        if (currentMonthKey.isNotEmpty()) {
            val parts = currentMonthKey.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val settings = getEffectiveSettingsForMonth(year, month)

            android.util.Log.d("Repository", "更新前 - 工资项: ${_salaryItems.value.items.size}个, 总工资: ${_grossSalary.value}, 社保: ${_socialInsuranceRate.value}, 公积金: ${_housingFundRate.value}")

            // 更新工资项集合
            _salaryItems.value = settings.salaryItems

            // 向后兼容的属性更新
            _basicSalary.value = settings.basicSalary
            _allowance.value = settings.allowance
            _grossSalary.value = settings.grossSalary
            _socialInsuranceRate.value = settings.personalSocialInsuranceRate
            _housingFundRate.value = settings.personalHousingFundRate

            android.util.Log.d("Repository", "更新后 - 工资项: ${_salaryItems.value.items.size}个, 总工资: ${_grossSalary.value}, 社保: ${_socialInsuranceRate.value}, 公积金: ${_housingFundRate.value}")
        }
    }

    fun getEffectiveSettingsForMonth(year: Int, month: Int): SalarySettingsRecord {
        // 锚点系统：首先查找该月份的直接记录
        val directRecord = salarySettingsHistory.find { record ->
            val recordDate = java.util.Calendar.getInstance().apply {
                timeInMillis = record.effectiveDate
            }
            val recordYear = recordDate.get(java.util.Calendar.YEAR)
            val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1
            recordYear == year && recordMonth == month
        }

        if (directRecord != null) {
            val recordType = when {
                directRecord.isAnchor && !directRecord.isAutoAnchor -> "手动锚点"
                directRecord.isAnchor && directRecord.isAutoAnchor -> "自动锚点"
                else -> "继承记录"
            }
            android.util.Log.d("Repository", "获取${year}年${month}月设置: 工资=${directRecord.grossSalary}, 类型=${recordType}")
            return directRecord
        }

        // 没有直接记录，查找最近的锚点进行继承
        val nearestAnchor = salarySettingsHistory
            .filter { record ->
                if (!record.isAnchor) return@filter false

                val recordDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = record.effectiveDate
                }
                val recordYear = recordDate.get(java.util.Calendar.YEAR)
                val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1

                // 只考虑目标月份之前的锚点
                recordYear < year || (recordYear == year && recordMonth < month)
            }
            .maxByOrNull { it.effectiveDate }

        if (nearestAnchor != null) {
            // 检查是否有下一个锚点阻断继承
            val nextAnchor = salarySettingsHistory
                .filter { record ->
                    if (!record.isAnchor) return@filter false

                    val recordDate = java.util.Calendar.getInstance().apply {
                        timeInMillis = record.effectiveDate
                    }
                    val recordYear = recordDate.get(java.util.Calendar.YEAR)
                    val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1

                    // 找到目标月份之后的第一个锚点
                    recordYear > year || (recordYear == year && recordMonth > month)
                }
                .minByOrNull { it.effectiveDate }

            val anchorDate = java.util.Calendar.getInstance().apply {
                timeInMillis = nearestAnchor.effectiveDate
            }
            val anchorYear = anchorDate.get(java.util.Calendar.YEAR)
            val anchorMonth = anchorDate.get(java.util.Calendar.MONTH) + 1

            // 如果没有下一个锚点，或者目标月份在继承范围内，则继承
            val shouldInherit = if (nextAnchor != null) {
                val nextDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = nextAnchor.effectiveDate
                }
                val nextYear = nextDate.get(java.util.Calendar.YEAR)
                val nextMonth = nextDate.get(java.util.Calendar.MONTH) + 1

                // 目标月份在两个锚点之间
                (year < nextYear || (year == nextYear && month < nextMonth))
            } else {
                // 没有下一个锚点，检查是否在合理的继承范围内
                val currentDate = java.util.Calendar.getInstance()
                val currentYear = currentDate.get(java.util.Calendar.YEAR)
                val currentMonth = currentDate.get(java.util.Calendar.MONTH) + 1

                // 只继承到当前系统时间
                year < currentYear || (year == currentYear && month <= currentMonth)
            }

            if (shouldInherit) {
                android.util.Log.d("Repository", "获取${year}年${month}月设置: 工资=${nearestAnchor.grossSalary}, 来源=继承自${anchorYear}年${anchorMonth}月锚点")
                return nearestAnchor
            }
        }

        // 没有可继承的锚点，返回默认值
        android.util.Log.d("Repository", "获取${year}年${month}月设置: 工资=0.0, 来源=无记录")
        return getDefaultSettings()
    }

    private fun getDefaultSettings(): SalarySettingsRecord {
        return SalarySettingsRecord(
            id = 0,
            salaryItems = SalaryItemCollection.createDefault(),
            personalSocialInsuranceRate = 0.0,
            companySocialInsuranceRate = 0.0,
            personalHousingFundRate = 0.0,
            companyHousingFundRate = 0.0,
            effectiveDate = 0,
            createdAt = System.currentTimeMillis(),
            isHistoricalModification = false,
            modifiedYear = null,
            modifiedMonth = null,
            isAnchor = false,
            isAutoAnchor = false
        )
    }

    fun getSettingsHistory(): List<SalarySettingsRecord> {
        return salarySettingsHistory.sortedByDescending { it.effectiveDate }
    }

    fun getSettingsForCurrentMonth(): SalarySettingsRecord {
        val currentMonthKey = _currentYearMonth.value
        if (currentMonthKey.isNotEmpty()) {
            val parts = currentMonthKey.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            return getEffectiveSettingsForMonth(year, month)
        }
        return getDefaultSettings()
    }

    /**
     * 锚点系统：创建手动锚点
     */
    private fun createManualAnchor(year: Int, month: Int, salaryItems: SalaryItemCollection,
                                 personalSocialRate: Double, companySocialRate: Double,
                                 personalHousingRate: Double, companyHousingRate: Double) {

        android.util.Log.d("Repository", "创建手动锚点: ${year}年${month}月")

        // 1. 创建或更新当前月份的锚点
        val anchorRecord = createAnchorRecord(year, month, salaryItems, personalSocialRate,
                                            companySocialRate, personalHousingRate, companyHousingRate,
                                            isManual = true)

        // 2. 找到下一个锚点
        val nextAnchor = findNextAnchor(year, month)

        // 3. 更新当前锚点到下一个锚点之间的所有月份
        updateMonthsBetweenAnchors(anchorRecord, nextAnchor)

        android.util.Log.d("Repository", "手动锚点创建完成")
    }

    /**
     * 创建锚点记录
     */
    private fun createAnchorRecord(year: Int, month: Int, salaryItems: SalaryItemCollection,
                                 personalSocialRate: Double, companySocialRate: Double,
                                 personalHousingRate: Double, companyHousingRate: Double,
                                 isManual: Boolean): SalarySettingsRecord {

        val effectiveDate = java.util.Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val newRecord = SalarySettingsRecord(
            id = System.currentTimeMillis(),
            salaryItems = salaryItems,
            personalSocialInsuranceRate = personalSocialRate,
            companySocialInsuranceRate = companySocialRate,
            personalHousingFundRate = personalHousingRate,
            companyHousingFundRate = companyHousingRate,
            effectiveDate = effectiveDate,
            createdAt = System.currentTimeMillis(),
            isHistoricalModification = false,
            modifiedYear = null,
            modifiedMonth = null,
            isAnchor = true,
            isAutoAnchor = !isManual
        )

        // 检查是否已经存在该月份的记录
        val existingIndex = salarySettingsHistory.indexOfFirst { record ->
            val recordDate = java.util.Calendar.getInstance().apply {
                timeInMillis = record.effectiveDate
            }
            val recordYear = recordDate.get(java.util.Calendar.YEAR)
            val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1
            recordYear == year && recordMonth == month
        }

        if (existingIndex != -1) {
            salarySettingsHistory[existingIndex] = newRecord
            android.util.Log.d("Repository", "更新${year}年${month}月锚点")
        } else {
            salarySettingsHistory.add(newRecord)
            android.util.Log.d("Repository", "新增${year}年${month}月锚点")
        }

        return newRecord
    }

    /**
     * 查找下一个锚点
     */
    private fun findNextAnchor(currentYear: Int, currentMonth: Int): SalarySettingsRecord? {
        return salarySettingsHistory
            .filter { record ->
                if (!record.isAnchor) return@filter false

                val recordDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = record.effectiveDate
                }
                val recordYear = recordDate.get(java.util.Calendar.YEAR)
                val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1

                // 找到当前月份之后的第一个锚点
                recordYear > currentYear || (recordYear == currentYear && recordMonth > currentMonth)
            }
            .minByOrNull { it.effectiveDate }
    }

    /**
     * 更新两个锚点之间的所有月份
     */
    private fun updateMonthsBetweenAnchors(currentAnchor: SalarySettingsRecord, nextAnchor: SalarySettingsRecord?) {
        val currentDate = java.util.Calendar.getInstance().apply {
            timeInMillis = currentAnchor.effectiveDate
        }
        val currentYear = currentDate.get(java.util.Calendar.YEAR)
        val currentMonth = currentDate.get(java.util.Calendar.MONTH) + 1

        val endYear: Int
        val endMonth: Int

        if (nextAnchor != null) {
            val nextDate = java.util.Calendar.getInstance().apply {
                timeInMillis = nextAnchor.effectiveDate
            }
            endYear = nextDate.get(java.util.Calendar.YEAR)
            endMonth = nextDate.get(java.util.Calendar.MONTH) + 1
        } else {
            // 没有下一个锚点，更新到当前系统时间
            val systemDate = java.util.Calendar.getInstance()
            endYear = systemDate.get(java.util.Calendar.YEAR)
            endMonth = systemDate.get(java.util.Calendar.MONTH) + 1
        }

        android.util.Log.d("Repository", "更新${currentYear}年${currentMonth}月到${endYear}年${endMonth}月之间的记录")

        val calendar = java.util.Calendar.getInstance()
        calendar.set(currentYear, currentMonth - 1, 1)

        while (true) {
            calendar.add(java.util.Calendar.MONTH, 1)
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1

            // 如果到达结束月份，停止
            if (year > endYear || (year == endYear && month >= endMonth)) {
                break
            }

            // 检查该月份是否已经是锚点
            val isExistingAnchor = salarySettingsHistory.any { record ->
                val recordDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = record.effectiveDate
                }
                val recordYear = recordDate.get(java.util.Calendar.YEAR)
                val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1
                recordYear == year && recordMonth == month && record.isAnchor
            }

            if (!isExistingAnchor) {
                // 创建继承记录（非锚点）
                createInheritedRecord(year, month, currentAnchor)
            }
        }
    }

    /**
     * 创建继承记录（非锚点）
     */
    private fun createInheritedRecord(year: Int, month: Int, sourceAnchor: SalarySettingsRecord) {
        val effectiveDate = java.util.Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val inheritedRecord = SalarySettingsRecord(
            id = System.currentTimeMillis() + (year * 100 + month), // 确保ID唯一
            salaryItems = sourceAnchor.salaryItems,
            personalSocialInsuranceRate = sourceAnchor.personalSocialInsuranceRate,
            companySocialInsuranceRate = sourceAnchor.companySocialInsuranceRate,
            personalHousingFundRate = sourceAnchor.personalHousingFundRate,
            companyHousingFundRate = sourceAnchor.companyHousingFundRate,
            effectiveDate = effectiveDate,
            createdAt = System.currentTimeMillis(),
            isHistoricalModification = false,
            modifiedYear = null,
            modifiedMonth = null,
            isAnchor = false,      // 继承记录不是锚点
            isAutoAnchor = false
        )

        // 检查是否已经存在该月份的记录
        val existingIndex = salarySettingsHistory.indexOfFirst { record ->
            val recordDate = java.util.Calendar.getInstance().apply {
                timeInMillis = record.effectiveDate
            }
            val recordYear = recordDate.get(java.util.Calendar.YEAR)
            val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1
            recordYear == year && recordMonth == month
        }

        if (existingIndex != -1) {
            // 如果已存在记录且不是锚点，则替换
            if (!salarySettingsHistory[existingIndex].isAnchor) {
                salarySettingsHistory[existingIndex] = inheritedRecord
                android.util.Log.d("Repository", "更新${year}年${month}月继承记录")
            }
        } else {
            salarySettingsHistory.add(inheritedRecord)
            android.util.Log.d("Repository", "新增${year}年${month}月继承记录")
        }
    }



    // 调试方法：打印锚点信息
    fun debugPrintAnchors() {
        android.util.Log.d("Repository", "=== 锚点系统状态 ===")
        salarySettingsHistory.sortedBy { it.effectiveDate }.forEach { record ->
            val date = java.util.Calendar.getInstance().apply {
                timeInMillis = record.effectiveDate
            }
            val year = date.get(java.util.Calendar.YEAR)
            val month = date.get(java.util.Calendar.MONTH) + 1
            val type = when {
                record.isAnchor && !record.isAutoAnchor -> "手动锚点"
                record.isAnchor && record.isAutoAnchor -> "自动锚点"
                else -> "继承记录"
            }
            android.util.Log.d("Repository", "${year}年${month}月: 基本工资=${record.basicSalary}, 补贴=${record.allowance}, 总工资=${record.grossSalary}, 类型=${type}")
        }
        android.util.Log.d("Repository", "=== 锚点状态结束 ===")
    }

    /**
     * 重置某个月份的设置（删除锚点）
     */
    fun resetMonthSettings(year: Int, month: Int) {
        android.util.Log.d("Repository", "=== 重置${year}年${month}月设置 ===")

        // 找到要删除的锚点
        val targetAnchor = salarySettingsHistory.find { record ->
            val recordDate = java.util.Calendar.getInstance().apply {
                timeInMillis = record.effectiveDate
            }
            val recordYear = recordDate.get(java.util.Calendar.YEAR)
            val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1
            recordYear == year && recordMonth == month && record.isAnchor
        }

        if (targetAnchor == null) {
            android.util.Log.d("Repository", "${year}年${month}月没有锚点，无需重置")
            return
        }

        // 找到上一个锚点和下一个锚点
        val previousAnchor = findPreviousAnchor(year, month)
        val nextAnchor = findNextAnchor(year, month)

        // 删除当前锚点
        salarySettingsHistory.remove(targetAnchor)
        android.util.Log.d("Repository", "删除${year}年${month}月锚点")

        // 重新生成上一个锚点到下一个锚点之间的记录
        if (previousAnchor != null) {
            updateMonthsBetweenAnchors(previousAnchor, nextAnchor)
        } else {
            // 没有上一个锚点，删除到下一个锚点之间的所有记录
            clearRecordsUntilNextAnchor(year, month, nextAnchor)
        }

        // 更新显示值
        updateCurrentDisplayValues()
        _settingsChanged.value = System.currentTimeMillis()

        android.util.Log.d("Repository", "重置完成")
        debugPrintAnchors()
    }

    /**
     * 查找上一个锚点
     */
    private fun findPreviousAnchor(currentYear: Int, currentMonth: Int): SalarySettingsRecord? {
        return salarySettingsHistory
            .filter { record ->
                if (!record.isAnchor) return@filter false

                val recordDate = java.util.Calendar.getInstance().apply {
                    timeInMillis = record.effectiveDate
                }
                val recordYear = recordDate.get(java.util.Calendar.YEAR)
                val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1

                // 找到当前月份之前的最近锚点
                recordYear < currentYear || (recordYear == currentYear && recordMonth < currentMonth)
            }
            .maxByOrNull { it.effectiveDate }
    }

    /**
     * 清理到下一个锚点之间的记录
     */
    private fun clearRecordsUntilNextAnchor(startYear: Int, startMonth: Int, nextAnchor: SalarySettingsRecord?) {
        val endYear: Int
        val endMonth: Int

        if (nextAnchor != null) {
            val nextDate = java.util.Calendar.getInstance().apply {
                timeInMillis = nextAnchor.effectiveDate
            }
            endYear = nextDate.get(java.util.Calendar.YEAR)
            endMonth = nextDate.get(java.util.Calendar.MONTH) + 1
        } else {
            // 没有下一个锚点，清理到当前系统时间
            val systemDate = java.util.Calendar.getInstance()
            endYear = systemDate.get(java.util.Calendar.YEAR)
            endMonth = systemDate.get(java.util.Calendar.MONTH) + 1
        }

        val toRemove = salarySettingsHistory.filter { record ->
            if (record.isAnchor) return@filter false // 不删除锚点

            val recordDate = java.util.Calendar.getInstance().apply {
                timeInMillis = record.effectiveDate
            }
            val recordYear = recordDate.get(java.util.Calendar.YEAR)
            val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1

            // 删除指定范围内的非锚点记录
            (recordYear > startYear || (recordYear == startYear && recordMonth >= startMonth)) &&
            (recordYear < endYear || (recordYear == endYear && recordMonth < endMonth))
        }

        toRemove.forEach { record ->
            salarySettingsHistory.remove(record)
            val recordDate = java.util.Calendar.getInstance().apply {
                timeInMillis = record.effectiveDate
            }
            val recordYear = recordDate.get(java.util.Calendar.YEAR)
            val recordMonth = recordDate.get(java.util.Calendar.MONTH) + 1
            android.util.Log.d("Repository", "清理${recordYear}年${recordMonth}月记录")
        }
    }

    /**
     * 清理旧系统的错误记录
     */
    fun cleanupOldSystemRecords() {
        android.util.Log.d("Repository", "清理旧系统记录，准备使用锚点系统")

        // 清理所有记录，重新开始
        salarySettingsHistory.clear()

        // 触发设置变更通知
        _settingsChanged.value = System.currentTimeMillis()
        android.util.Log.d("Repository", "旧系统记录已清理，锚点系统已就绪")
    }





    fun addExpenseRecord(year: Int, month: Int, amount: Double, category: String, description: String?): ExpenseRecord {
        val monthKey = getMonthKey(year, month)
        val record = ExpenseRecord(
            id = System.currentTimeMillis(),
            amount = amount,
            category = category,
            description = description,
            date = System.currentTimeMillis()
        )

        // 添加记录
        monthlyExpenseRecords.getOrPut(monthKey) { mutableListOf() }.add(record)

        // 更新总支出
        val newTotal = (monthlyExpenses[monthKey] ?: 0.0) + amount
        monthlyExpenses[monthKey] = newTotal

        // 如果是当前月份，更新数据流
        if (monthKey == _currentYearMonth.value) {
            _expenses.value = newTotal
        }

        return record
    }

    fun addOtherIncomeRecord(year: Int, month: Int, amount: Double, description: String?): IncomeRecord {
        val monthKey = getMonthKey(year, month)
        val record = IncomeRecord(
            id = System.currentTimeMillis(),
            amount = amount,
            description = description,
            date = System.currentTimeMillis()
        )

        // 添加记录
        monthlyOtherIncomeRecords.getOrPut(monthKey) { mutableListOf() }.add(record)

        // 更新总收入
        val newTotal = (monthlyOtherIncome[monthKey] ?: 0.0) + amount
        monthlyOtherIncome[monthKey] = newTotal

        // 如果是当前月份，更新数据流
        if (monthKey == _currentYearMonth.value) {
            _otherIncome.value = newTotal
        }

        return record
    }

    fun getExpenseRecords(year: Int, month: Int): List<ExpenseRecord> {
        val monthKey = getMonthKey(year, month)
        return monthlyExpenseRecords[monthKey]?.toList() ?: emptyList()
    }

    fun getOtherIncomeRecords(year: Int, month: Int): List<IncomeRecord> {
        val monthKey = getMonthKey(year, month)
        return monthlyOtherIncomeRecords[monthKey]?.toList() ?: emptyList()
    }

    fun deleteExpenseRecord(year: Int, month: Int, recordId: Long): Boolean {
        val monthKey = getMonthKey(year, month)
        val records = monthlyExpenseRecords[monthKey] ?: return false

        val record = records.find { it.id == recordId } ?: return false
        records.remove(record)

        // 更新总支出
        val newTotal = (monthlyExpenses[monthKey] ?: 0.0) - record.amount
        monthlyExpenses[monthKey] = newTotal.coerceAtLeast(0.0)

        // 如果是当前月份，更新数据流
        if (monthKey == _currentYearMonth.value) {
            _expenses.value = newTotal.coerceAtLeast(0.0)
        }

        return true
    }

    fun deleteOtherIncomeRecord(year: Int, month: Int, recordId: Long): Boolean {
        val monthKey = getMonthKey(year, month)
        val records = monthlyOtherIncomeRecords[monthKey] ?: return false

        val record = records.find { it.id == recordId } ?: return false
        records.remove(record)

        // 更新总收入
        val newTotal = (monthlyOtherIncome[monthKey] ?: 0.0) - record.amount
        monthlyOtherIncome[monthKey] = newTotal.coerceAtLeast(0.0)

        // 如果是当前月份，更新数据流
        if (monthKey == _currentYearMonth.value) {
            _otherIncome.value = newTotal.coerceAtLeast(0.0)
        }

        return true
    }
    
    // 计算财务数据
    fun calculateFinancialData(): FinancialData {
        // 获取当前月份的有效设置
        val currentSettings = getSettingsForCurrentMonth()

        val salaryItems = currentSettings.salaryItems
        val totalSalary = salaryItems.totalSalary
        val taxableBase = salaryItems.taxableBase
        val socialSecurityBase = salaryItems.socialSecurityBase

        // 社保公积金基数：根据工资项设置计算
        val socialInsurance = socialSecurityBase * currentSettings.personalSocialInsuranceRate
        val housingFund = socialSecurityBase * currentSettings.personalHousingFundRate

        // 纳税基数：根据工资项设置计算，减去社保公积金
        val taxableIncome = taxableBase - socialInsurance - housingFund

        // 使用累积计税法计算税额
        val tax = calculateCumulativeTax()
        val netIncome = totalSalary - socialInsurance - housingFund - tax

        android.util.Log.d("Repository", "财务计算: 总工资=${totalSalary}, 纳税基数=${taxableBase}, 社保基数=${socialSecurityBase}")
        android.util.Log.d("Repository", "社保=${socialInsurance}, 公积金=${housingFund}, 税额=${tax}, 实发=${netIncome}")

        return FinancialData(
            grossSalary = totalSalary,
            socialInsurance = socialInsurance,
            housingFund = housingFund,
            tax = tax,
            netIncome = netIncome,
            expenses = _expenses.value,
            otherIncome = _otherIncome.value
        )
    }
    
    /**
     * 累积计税法：计算当前月份应缴个人所得税
     */
    private fun calculateCumulativeTax(): Double {
        val currentMonthKey = _currentYearMonth.value
        if (currentMonthKey.isEmpty()) return 0.0

        val parts = currentMonthKey.split("-")
        val currentYear = parts[0].toInt()
        val currentMonth = parts[1].toInt()

        // 计算年初到当前月份的累计应纳税所得额
        val cumulativeTaxableIncome = calculateCumulativeTaxableIncome(currentYear, currentMonth)

        // 计算累计应纳税额
        val cumulativeTax = calculateAnnualTax(cumulativeTaxableIncome)

        // 计算年初到上月的累计应纳税额
        val previousCumulativeTax = if (currentMonth > 1) {
            val previousCumulativeTaxableIncome = calculateCumulativeTaxableIncome(currentYear, currentMonth - 1)
            calculateAnnualTax(previousCumulativeTaxableIncome)
        } else {
            0.0
        }

        // 当月应缴税额 = 累计应纳税额 - 累计已缴税额
        val currentMonthTax = cumulativeTax - previousCumulativeTax

        android.util.Log.d("Repository", "累积计税: ${currentYear}年${currentMonth}月")
        android.util.Log.d("Repository", "累计应纳税所得额: ${cumulativeTaxableIncome}")
        android.util.Log.d("Repository", "累计应纳税额: ${cumulativeTax}")
        android.util.Log.d("Repository", "上月累计已缴税额: ${previousCumulativeTax}")
        android.util.Log.d("Repository", "当月应缴税额: ${currentMonthTax}")

        return currentMonthTax.coerceAtLeast(0.0)
    }

    /**
     * 计算年初到指定月份的累计应纳税所得额
     */
    private fun calculateCumulativeTaxableIncome(year: Int, month: Int): Double {
        var cumulativeTaxableIncome = 0.0

        for (m in 1..month) {
            val settings = getEffectiveSettingsForMonth(year, m)
            val salaryItems = settings.salaryItems
            val taxableBase = salaryItems.taxableBase
            val socialSecurityBase = salaryItems.socialSecurityBase

            // 社保公积金基数：根据工资项设置
            val socialInsurance = socialSecurityBase * settings.personalSocialInsuranceRate
            val housingFund = socialSecurityBase * settings.personalHousingFundRate

            // 纳税基数：根据工资项设置减去社保公积金
            val monthlyTaxableIncome = taxableBase - socialInsurance - housingFund

            // 减去起征点（每月5000元）
            val monthlyTaxable = (monthlyTaxableIncome - 5000.0).coerceAtLeast(0.0)
            cumulativeTaxableIncome += monthlyTaxable
        }

        return cumulativeTaxableIncome
    }

    /**
     * 根据全年累计应纳税所得额计算年度应纳税额
     * 使用累积计税法的税率表和速算扣除数
     */
    private fun calculateAnnualTax(cumulativeTaxableIncome: Double): Double {
        return when {
            cumulativeTaxableIncome <= 0 -> 0.0
            cumulativeTaxableIncome <= 36000 -> cumulativeTaxableIncome * 0.03 - 0
            cumulativeTaxableIncome <= 144000 -> cumulativeTaxableIncome * 0.10 - 2520
            cumulativeTaxableIncome <= 300000 -> cumulativeTaxableIncome * 0.20 - 16920
            cumulativeTaxableIncome <= 420000 -> cumulativeTaxableIncome * 0.25 - 31920
            cumulativeTaxableIncome <= 660000 -> cumulativeTaxableIncome * 0.30 - 52920
            cumulativeTaxableIncome <= 960000 -> cumulativeTaxableIncome * 0.35 - 85920
            else -> cumulativeTaxableIncome * 0.45 - 181920
        }
    }
}

data class FinancialData(
    val grossSalary: Double,
    val socialInsurance: Double,
    val housingFund: Double,
    val tax: Double,
    val netIncome: Double,
    val expenses: Double,
    val otherIncome: Double
)

data class ExpenseRecord(
    val id: Long,
    val amount: Double,
    val category: String,
    val description: String?,
    val date: Long
)

data class IncomeRecord(
    val id: Long,
    val amount: Double,
    val description: String?,
    val date: Long
)

data class SalarySettingsRecord(
    val id: Long,
    val salaryItems: SalaryItemCollection,   // 工资项集合
    val personalSocialInsuranceRate: Double,
    val companySocialInsuranceRate: Double,
    val personalHousingFundRate: Double,
    val companyHousingFundRate: Double,
    val effectiveDate: Long, // 生效日期
    val createdAt: Long, // 创建时间
    val isHistoricalModification: Boolean, // 是否为历史修改
    val modifiedYear: Int?, // 修改的年份（仅历史修改时有值）
    val modifiedMonth: Int?, // 修改的月份（仅历史修改时有值）
    // 锚点系统
    val isAnchor: Boolean = false,           // 是否为锚点
    val isAutoAnchor: Boolean = false        // 是否为自动锚点（当前月份自动生成）
) {
    // 向后兼容的计算属性
    val basicSalary: Double get() = salaryItems.basicSalaryAmount
    val allowance: Double get() = salaryItems.items.find { it.id == SalaryItem.ALLOWANCE_ID }?.amount ?: 0.0
    val grossSalary: Double get() = salaryItems.totalSalary
    val taxableBase: Double get() = salaryItems.taxableBase
    val socialSecurityBase: Double get() = salaryItems.socialSecurityBase
}
