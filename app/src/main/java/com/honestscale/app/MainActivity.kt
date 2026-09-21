package com.honestscale.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : Activity() {
    private val forest = Color.rgb(18, 110, 79)
    private val ink = Color.rgb(31, 42, 38)
    private lateinit var resultA: TextView
    private lateinit var resultB: TextView
    private lateinit var comparison: TextView
    private lateinit var productA: ProductForm
    private lateinit var productB: ProductForm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            setBackgroundColor(Color.rgb(247, 247, 242))
        }
        content.addView(title("心里那杆秤", 28, forest))
        content.addView(label("把包装规格换成每克 / 每毫升的真实价格", 15, Color.DKGRAY, 0, 4, 0, 16))

        productA = productForm("商品 A", "牛奶（箱装）", "48", "250", "12", "1")
        resultA = resultView()
        content.addView(productA.root)
        content.addView(resultA)

        productB = productForm("商品 B", "牛奶（单盒）", "6", "500", "1", "1")
        resultB = resultView()
        content.addView(productB.root)
        content.addView(resultB)

        val calculate = TextView(this).apply {
            text = "计算并比较"
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.CENTER
            setBackgroundColor(forest)
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { calculate() }
        }
        content.addView(calculate, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        comparison = label("填写后点击计算", 17, ink, 0, 16, 0, 0).apply { gravity = Gravity.CENTER }
        content.addView(comparison)
        setContentView(ScrollView(this).apply { addView(content) })
        calculate()
    }

    private fun productForm(
        heading: String, name: String, price: String, amount: String, perPack: String, packs: String,
    ): ProductForm {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(title(heading, 19, ink))
        val nameInput = input("商品名称", name, false)
        val priceInput = input("总价（元）", price, true)
        val amountInput = input("每件规格", amount, true)
        val unit = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, MeasureKind.entries.map { it.displayName })
            setSelection(MeasureKind.MILLILITRE.ordinal)
        }
        val itemInput = input("每包 / 每箱有几件", perPack, true)
        val packInput = input("买了几包 / 几箱", packs, true)
        root.addView(nameInput); root.addView(priceInput); root.addView(amountInput)
        root.addView(unit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        root.addView(itemInput); root.addView(packInput)
        return ProductForm(root, nameInput, priceInput, amountInput, unit, itemInput, packInput)
    }

    private fun calculate() {
        val a = read(productA, resultA)
        val b = read(productB, resultB)
        if (a == null || b == null) {
            comparison.text = "请检查标红的输入项"
            return
        }
        comparison.text = when (Pricing.comparison(a, b)) {
            Comparison.FIRST_CHEAPER -> "结论：商品 A 的单位价更低，更划算"
            Comparison.SECOND_CHEAPER -> "结论：商品 B 的单位价更低，更划算"
            Comparison.TIE -> "结论：两件商品单位价相同"
            null -> "一个按重量、一个按体积，暂不能直接比较"
        }
    }

    private fun read(form: ProductForm, output: TextView): UnitPrice? = try {
        val input = ProductInput(
            priceYuan = decimal(form.price),
            amountPerItem = decimal(form.amount),
            measure = MeasureKind.entries[form.unit.selectedItemPosition],
            itemsPerPack = integer(form.items),
            packCount = integer(form.packs),
        )
        val value = Pricing.calculate(input)
        val price = value.yuanPerBaseUnit.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        output.text = "总量：${format(value.totalBaseAmount)} ${value.family.baseUnit}   ·   单位价：$price 元 / ${value.family.baseUnit}"
        output.setTextColor(forest)
        value
    } catch (_: Exception) {
        output.text = "请输入有效的正数规格与数量"
        output.setTextColor(Color.rgb(180, 30, 30))
        null
    }

    private fun decimal(field: EditText) = BigDecimal(field.text.toString().trim())
    private fun integer(field: EditText) = field.text.toString().trim().toInt()
    private fun format(value: BigDecimal) = value.stripTrailingZeros().toPlainString()
    private fun input(hint: String, value: String, number: Boolean) = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 16f
        inputType = if (number) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) }
    }
    private fun title(text: String, size: Int, color: Int) = TextView(this).apply { this.text = text; textSize = size.toFloat(); setTextColor(color) }
    private fun label(text: String, size: Int, color: Int, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(color); setPadding(dp(l), dp(t), dp(r), dp(b))
    }
    private fun resultView() = label("", 15, forest, 4, 10, 4, 18)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private data class ProductForm(
        val root: View, val name: EditText, val price: EditText, val amount: EditText,
        val unit: Spinner, val items: EditText, val packs: EditText,
    )
}
