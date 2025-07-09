package com.example.financialapp

/**
 * 支出分类数据模型
 */
data class ExpenseCategory(
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false // 是否为系统默认分类
)

/**
 * 支出子分类数据模型
 */
data class ExpenseSubCategory(
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val isDefault: Boolean = false // 是否为系统默认子分类
)

/**
 * 分类管理器，提供默认分类和子分类
 */
object CategoryManager {
    
    /**
     * 获取默认分类列表
     */
    fun getDefaultCategories(): List<ExpenseCategory> {
        return listOf(
            ExpenseCategory(1, "伙食", true),
            ExpenseCategory(2, "交通", true),
            ExpenseCategory(3, "购物", true),
            ExpenseCategory(4, "娱乐", true),
            ExpenseCategory(5, "医疗", true),
            ExpenseCategory(6, "教育", true),
            ExpenseCategory(7, "住房", true),
            ExpenseCategory(8, "通讯", true),
            ExpenseCategory(9, "其他", true)
        )
    }
    
    /**
     * 获取默认子分类列表
     */
    fun getDefaultSubCategories(): List<ExpenseSubCategory> {
        return listOf(
            // 伙食类
            ExpenseSubCategory(1, 1, "早餐", true),
            ExpenseSubCategory(2, 1, "午餐", true),
            ExpenseSubCategory(3, 1, "晚餐", true),
            ExpenseSubCategory(4, 1, "夜宵", true),
            ExpenseSubCategory(5, 1, "零食", true),
            ExpenseSubCategory(6, 1, "饮料", true),
            
            // 交通类
            ExpenseSubCategory(7, 2, "公交", true),
            ExpenseSubCategory(8, 2, "地铁", true),
            ExpenseSubCategory(9, 2, "出租车", true),
            ExpenseSubCategory(10, 2, "网约车", true),
            ExpenseSubCategory(11, 2, "加油", true),
            ExpenseSubCategory(12, 2, "停车费", true),
            
            // 购物类
            ExpenseSubCategory(13, 3, "服装", true),
            ExpenseSubCategory(14, 3, "日用品", true),
            ExpenseSubCategory(15, 3, "化妆品", true),
            ExpenseSubCategory(16, 3, "电子产品", true),
            ExpenseSubCategory(17, 3, "家具", true),
            
            // 娱乐类
            ExpenseSubCategory(18, 4, "电影", true),
            ExpenseSubCategory(19, 4, "游戏", true),
            ExpenseSubCategory(20, 4, "旅游", true),
            ExpenseSubCategory(21, 4, "运动", true),
            ExpenseSubCategory(22, 4, "聚餐", true),
            
            // 医疗类
            ExpenseSubCategory(23, 5, "挂号费", true),
            ExpenseSubCategory(24, 5, "药品", true),
            ExpenseSubCategory(25, 5, "体检", true),
            ExpenseSubCategory(26, 5, "治疗费", true),
            
            // 教育类
            ExpenseSubCategory(27, 6, "学费", true),
            ExpenseSubCategory(28, 6, "书籍", true),
            ExpenseSubCategory(29, 6, "培训", true),
            ExpenseSubCategory(30, 6, "考试费", true),
            
            // 住房类
            ExpenseSubCategory(31, 7, "房租", true),
            ExpenseSubCategory(32, 7, "水费", true),
            ExpenseSubCategory(33, 7, "电费", true),
            ExpenseSubCategory(34, 7, "燃气费", true),
            ExpenseSubCategory(35, 7, "物业费", true),
            
            // 通讯类
            ExpenseSubCategory(36, 8, "话费", true),
            ExpenseSubCategory(37, 8, "网费", true),
            ExpenseSubCategory(38, 8, "流量费", true)
        )
    }
    
    /**
     * 根据分类ID获取子分类
     */
    fun getSubCategoriesByCategory(categoryId: Long, allSubCategories: List<ExpenseSubCategory>): List<ExpenseSubCategory> {
        return allSubCategories.filter { it.categoryId == categoryId }
    }
}
