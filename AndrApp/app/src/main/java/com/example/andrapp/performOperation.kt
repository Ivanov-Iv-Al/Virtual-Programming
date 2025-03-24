// Calculator.kt
package com.example.andrapp

import android.widget.TextView

fun performOperation(
    num1: Float,
    num2: Float,
    operation: Operation,
    resultView: TextView
) {
    val resultValue = when (operation) {
        Operation.ADD -> num1 + num2
        Operation.SUBTRACT -> num1 - num2
        Operation.MULTIPLY -> num1 * num2
        Operation.DIVIDE -> if (num2 != 0f) num1 / num2 else Float.NaN
    }

    resultView.text = when {
        resultValue.isNaN() -> "Ошибка: деление на ноль"
        else -> resultValue.toString()
    }
}

enum class Operation {
    ADD, SUBTRACT, MULTIPLY, DIVIDE
}

