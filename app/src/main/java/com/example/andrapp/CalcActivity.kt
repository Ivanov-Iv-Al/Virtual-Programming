package com.example.andrapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CalcActivity : AppCompatActivity() {
    private lateinit var result: TextView
    private lateinit var Vvod_1: EditText
    private lateinit var Vvod_2: EditText
    private lateinit var buttonAdd: Button
    private lateinit var buttonSubtract: Button
    private lateinit var buttonMultiply: Button
    private lateinit var buttonDivide: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calc) // Убедитесь, что у вас есть соответствующий layout файл

        result = findViewById(R.id.Text_result)
        Vvod_1 = findViewById(R.id.Vvod_1)
        Vvod_2 = findViewById(R.id.Vvod_2)
        buttonAdd = findViewById(R.id.button1)
        buttonSubtract = findViewById(R.id.button2)
        buttonMultiply = findViewById(R.id.button4)
        buttonDivide = findViewById(R.id.button3)

        buttonAdd.setOnClickListener { performOperation(Operation.ADD) }
        buttonSubtract.setOnClickListener { performOperation(Operation.SUBTRACT) }
        buttonMultiply.setOnClickListener { performOperation(Operation.MULTIPLY) }
        buttonDivide.setOnClickListener { performOperation(Operation.DIVIDE) }
    }

    private fun performOperation(operation: Operation) {
        val num1 = Vvod_1.text.toString().toFloatOrNull() ?: 0f
        val num2 = Vvod_2.text.toString().toFloatOrNull() ?: 0f
        val resultValue = when (operation) {
            Operation.ADD -> num1 + num2
            Operation.SUBTRACT -> num1 - num2
            Operation.MULTIPLY -> num1 * num2
            Operation.DIVIDE -> if (num2 != 0f) num1 / num2 else Float.NaN
        }
        result.text = when {
            resultValue.isNaN() -> "Ошибка: деление на ноль"
            else -> resultValue.toString()
        }
    }

    private enum class Operation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }
}