package com.example.financialapp

import org.junit.Test
import org.junit.Assert.*

/**
 * 工资项系统单元测试
 */
class SalaryItemTest {

    @Test
    fun testSalaryItemCreation() {
        // 测试基本工资创建
        val basicSalary = SalaryItem.createBasicSalary(15000.0)
        assertEquals("基本工资", basicSalary.name)
        assertEquals(15000.0, basicSalary.amount, 0.01)
        assertTrue(basicSalary.includeTax)
        assertTrue(basicSalary.includeSocialSecurity)
        assertTrue(basicSalary.isDefault)

        // 测试补贴创建
        val allowance = SalaryItem.createAllowance(3000.0)
        assertEquals("补贴", allowance.name)
        assertEquals(3000.0, allowance.amount, 0.01)
        assertTrue(allowance.includeTax)
        assertFalse(allowance.includeSocialSecurity)
        assertFalse(allowance.isDefault)

        // 测试自定义工资项创建
        val overtime = SalaryItem.createCustomItem("加班费", 1500.0, true, false)
        assertEquals("加班费", overtime.name)
        assertEquals(1500.0, overtime.amount, 0.01)
        assertTrue(overtime.includeTax)
        assertFalse(overtime.includeSocialSecurity)
        assertFalse(overtime.isDefault)
    }

    @Test
    fun testSalaryItemCollection() {
        // 创建工资项集合
        val basicSalary = SalaryItem.createBasicSalary(12000.0)
        val allowance = SalaryItem.createAllowance(2000.0)
        val overtime = SalaryItem.createCustomItem("加班费", 1500.0, true, false)
        val bonus = SalaryItem.createCustomItem("奖金", 5000.0, true, false)

        val collection = SalaryItemCollection(
            items = listOf(basicSalary, allowance, overtime, bonus)
        )

        // 测试总工资计算
        assertEquals(20500.0, collection.totalSalary, 0.01)

        // 测试纳税基数计算（所有项目都纳税）
        assertEquals(20500.0, collection.taxableBase, 0.01)

        // 测试社保基数计算（只有基本工资）
        assertEquals(12000.0, collection.socialSecurityBase, 0.01)
    }

    @Test
    fun testSalaryItemCollectionOperations() {
        // 创建默认集合
        val collection = SalaryItemCollection.createDefault()
        assertEquals(2, collection.items.size)

        // 添加工资项
        val overtime = SalaryItem.createCustomItem("加班费", 1500.0, true, false)
        val newCollection = collection.addItem(overtime)
        assertEquals(3, newCollection.items.size)

        // 删除工资项
        val removedCollection = newCollection.removeItem(overtime.id)
        assertEquals(2, removedCollection.items.size)

        // 更新工资项
        val basicSalaryId = SalaryItem.BASIC_SALARY_ID
        val updatedCollection = collection.updateItem(basicSalaryId, 15000.0)
        val basicSalary = updatedCollection.items.find { it.id == basicSalaryId }
        assertEquals(15000.0, basicSalary?.amount ?: 0.0, 0.01)
    }

    @Test
    fun testComplexSalaryStructure() {
        // 测试复杂工资结构
        val items = listOf(
            SalaryItem.createBasicSalary(10000.0),                    // 纳税✓，社保✓
            SalaryItem.createCustomItem("岗位津贴", 2000.0, true, true),  // 纳税✓，社保✓
            SalaryItem.createCustomItem("交通补贴", 500.0, true, false),  // 纳税✓，社保✗
            SalaryItem.createCustomItem("餐费补贴", 800.0, false, false), // 纳税✗，社保✗
            SalaryItem.createCustomItem("加班费", 1200.0, true, false),   // 纳税✓，社保✗
            SalaryItem.createCustomItem("年终奖", 8000.0, true, false)    // 纳税✓，社保✗
        )

        val collection = SalaryItemCollection(items)

        // 总工资：10000 + 2000 + 500 + 800 + 1200 + 8000 = 22500
        assertEquals(22500.0, collection.totalSalary, 0.01)

        // 纳税基数：10000 + 2000 + 500 + 1200 + 8000 = 21700（餐费补贴不纳税）
        assertEquals(21700.0, collection.taxableBase, 0.01)

        // 社保基数：10000 + 2000 = 12000（只有基本工资和岗位津贴）
        assertEquals(12000.0, collection.socialSecurityBase, 0.01)
    }


}
