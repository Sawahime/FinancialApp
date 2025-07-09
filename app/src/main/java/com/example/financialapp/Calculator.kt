package com.example.financialapp

/**
 * 计算器类，处理基本的数学运算
 */
class Calculator {
    private var currentValue = 0.0
    private var previousValue = 0.0
    private var operator: String? = null
    private var isNewInput = true
    private var displayExpression = "0" // 显示的完整表达式
    
    /**
     * 输入数字
     */
    fun inputNumber(number: String): String {
        if (isNewInput) {
            // 如果是新输入，但表达式中已有运算符，则追加数字
            if (displayExpression.isNotEmpty() && isOperator(displayExpression.last().toString())) {
                currentValue = if (number == ".") 0.0 else number.toDoubleOrNull() ?: 0.0
                displayExpression += if (number == ".") "0." else number
            } else {
                // 完全新的输入
                currentValue = if (number == ".") 0.0 else number.toDoubleOrNull() ?: 0.0
                displayExpression = if (number == ".") "0." else number
            }
            isNewInput = false
        } else {
            // 继续输入数字
            val lastNumber = extractLastNumber(displayExpression)
            val newNumber = if (number == ".") {
                if (lastNumber.contains(".")) lastNumber else "$lastNumber."
            } else {
                "$lastNumber$number"
            }
            currentValue = newNumber.toDoubleOrNull() ?: currentValue

            // 更新表达式中的最后一个数字
            val operatorIndex = findLastOperatorIndex(displayExpression)
            if (operatorIndex >= 0) {
                displayExpression = displayExpression.substring(0, operatorIndex + 1) + newNumber
            } else {
                displayExpression = newNumber
            }
        }
        return displayExpression
    }
    
    /**
     * 输入运算符
     */
    fun inputOperator(op: String): String {
        if (operator != null && !isNewInput) {
            // 如果已有运算符且用户输入了数字，先计算
            calculate()
            displayExpression = formatNumber(currentValue) + op
        } else {
            // 如果连续输入运算符，替换最后一个运算符
            if (displayExpression.isNotEmpty() && isOperator(displayExpression.last().toString())) {
                displayExpression = displayExpression.dropLast(1) + op
            } else {
                displayExpression += op
            }
        }
        previousValue = currentValue
        operator = op
        isNewInput = true
        return displayExpression
    }
    
    /**
     * 执行计算
     */
    fun calculate(): String {
        if (operator != null) {
            when (operator) {
                "+" -> currentValue = previousValue + currentValue
                "-" -> currentValue = previousValue - currentValue
                "×" -> currentValue = previousValue * currentValue
                "÷" -> {
                    if (currentValue != 0.0) {
                        currentValue = previousValue / currentValue
                    } else {
                        // 除零错误
                        clear()
                        return "错误"
                    }
                }
            }
            operator = null
            isNewInput = true
            displayExpression = formatNumber(currentValue)
        }
        return displayExpression
    }
    
    /**
     * 清空所有
     */
    fun clear(): String {
        currentValue = 0.0
        previousValue = 0.0
        operator = null
        isNewInput = true
        displayExpression = "0"
        return displayExpression
    }

    /**
     * 删除最后一位
     */
    fun delete(): String {
        if (displayExpression.length > 1) {
            displayExpression = displayExpression.dropLast(1)
            // 如果删除后是运算符，重置状态
            if (displayExpression.isNotEmpty() && isOperator(displayExpression.last().toString())) {
                isNewInput = true
            } else {
                // 重新解析当前值
                val lastNumber = extractLastNumber(displayExpression)
                currentValue = lastNumber.toDoubleOrNull() ?: 0.0
                isNewInput = false
            }
        } else {
            clear()
        }
        return displayExpression
    }
    
    /**
     * 获取当前值
     */
    fun getCurrentValue(): Double = currentValue
    
    /**
     * 格式化数字显示
     */
    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            // 保留合理的小数位数，去除尾随零
            String.format("%.8f", value).trimEnd('0').trimEnd('.')
        }
    }
    
    /**
     * 重置为指定值
     */
    fun setValue(value: Double): String {
        currentValue = value
        previousValue = 0.0
        operator = null
        isNewInput = true
        displayExpression = formatNumber(currentValue)
        return displayExpression
    }

    /**
     * 判断是否为运算符
     */
    private fun isOperator(char: String): Boolean {
        return char in listOf("+", "-", "×", "÷")
    }

    /**
     * 从表达式中提取最后一个数字
     */
    private fun extractLastNumber(expression: String): String {
        var i = expression.length - 1
        while (i >= 0 && !isOperator(expression[i].toString())) {
            i--
        }
        return if (i < expression.length - 1) {
            expression.substring(i + 1)
        } else {
            "0"
        }
    }

    /**
     * 找到表达式中最后一个运算符的位置
     */
    private fun findLastOperatorIndex(expression: String): Int {
        for (i in expression.length - 1 downTo 0) {
            if (isOperator(expression[i].toString())) {
                return i
            }
        }
        return -1
    }
}
