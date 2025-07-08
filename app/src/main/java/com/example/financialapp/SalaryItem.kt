package com.example.financialapp

/**
 * 工资项数据类
 */
data class SalaryItem(
    val id: String,                    // 唯一标识
    val name: String,                  // 工资项名称（如：基本工资、补贴、加班费等）
    val amount: Double,                // 金额
    val includeTax: Boolean,           // 是否纳入税收计算基数
    val includeSocialSecurity: Boolean, // 是否纳入社保公积金计算基数
    val isDefault: Boolean = false,    // 是否为默认项（不可删除）
    val order: Int = 0                 // 显示顺序
) {
    companion object {
        // 预设工资项ID
        const val BASIC_SALARY_ID = "basic_salary"
        const val ALLOWANCE_ID = "allowance"
        
        /**
         * 创建默认的基本工资项
         */
        fun createBasicSalary(amount: Double = 0.0): SalaryItem {
            return SalaryItem(
                id = BASIC_SALARY_ID,
                name = "基本工资",
                amount = amount,
                includeTax = true,
                includeSocialSecurity = true,
                isDefault = true,
                order = 1
            )
        }
        
        /**
         * 创建默认的补贴项
         */
        fun createAllowance(amount: Double = 0.0): SalaryItem {
            return SalaryItem(
                id = ALLOWANCE_ID,
                name = "补贴",
                amount = amount,
                includeTax = true,
                includeSocialSecurity = false,
                isDefault = false,
                order = 2
            )
        }
        
        /**
         * 创建自定义工资项
         */
        fun createCustomItem(
            name: String,
            amount: Double = 0.0,
            includeTax: Boolean = true,
            includeSocialSecurity: Boolean = false
        ): SalaryItem {
            return SalaryItem(
                id = "custom_${System.currentTimeMillis()}",
                name = name,
                amount = amount,
                includeTax = includeTax,
                includeSocialSecurity = includeSocialSecurity,
                isDefault = false,
                order = Int.MAX_VALUE
            )
        }
    }
}

/**
 * 工资项集合，包含计算方法
 */
data class SalaryItemCollection(
    val items: List<SalaryItem>
) {
    /**
     * 计算总工资
     */
    val totalSalary: Double
        get() = items.sumOf { it.amount }
    
    /**
     * 计算纳税基数
     */
    val taxableBase: Double
        get() = items.filter { it.includeTax }.sumOf { it.amount }
    
    /**
     * 计算社保公积金基数
     */
    val socialSecurityBase: Double
        get() = items.filter { it.includeSocialSecurity }.sumOf { it.amount }
    
    /**
     * 获取基本工资金额
     */
    val basicSalaryAmount: Double
        get() = items.find { it.id == SalaryItem.BASIC_SALARY_ID }?.amount ?: 0.0
    
    /**
     * 按顺序排序的工资项
     */
    val sortedItems: List<SalaryItem>
        get() = items.sortedBy { it.order }
    
    /**
     * 添加工资项
     */
    fun addItem(item: SalaryItem): SalaryItemCollection {
        return copy(items = items + item)
    }
    
    /**
     * 删除工资项
     */
    fun removeItem(itemId: String): SalaryItemCollection {
        return copy(items = items.filter { it.id != itemId })
    }
    
    /**
     * 更新工资项
     */
    fun updateItem(itemId: String, newAmount: Double): SalaryItemCollection {
        return copy(
            items = items.map { item ->
                if (item.id == itemId) {
                    item.copy(amount = newAmount)
                } else {
                    item
                }
            }
        )
    }
    
    companion object {
        /**
         * 创建默认的工资项集合
         */
        fun createDefault(): SalaryItemCollection {
            return SalaryItemCollection(
                items = listOf(
                    SalaryItem.createBasicSalary(),
                    SalaryItem.createAllowance()
                )
            )
        }
    }
}
