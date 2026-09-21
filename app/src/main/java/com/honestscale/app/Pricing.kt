package com.honestscale.app

import java.math.BigDecimal
import java.math.RoundingMode

enum class MeasureKind(val displayName: String, val family: Family, val multiplier: BigDecimal) {
    GRAM("g", Family.MASS, BigDecimal.ONE),
    KILOGRAM("kg", Family.MASS, BigDecimal("1000")),
    MILLILITRE("ml", Family.VOLUME, BigDecimal.ONE),
    LITRE("L", Family.VOLUME, BigDecimal("1000"));

    enum class Family(val baseUnit: String) { MASS("g"), VOLUME("ml") }
}

data class ProductInput(
    val priceYuan: BigDecimal,
    val amountPerItem: BigDecimal,
    val measure: MeasureKind,
    val itemsPerPack: Int = 1,
    val packCount: Int = 1,
)

data class UnitPrice(
    val family: MeasureKind.Family,
    val totalBaseAmount: BigDecimal,
    val yuanPerBaseUnit: BigDecimal,
)

object Pricing {
    fun calculate(input: ProductInput): UnitPrice {
        require(input.priceYuan >= BigDecimal.ZERO) { "价格不能为负数" }
        require(input.amountPerItem > BigDecimal.ZERO) { "单件规格必须大于 0" }
        require(input.itemsPerPack > 0 && input.packCount > 0) { "包装数量必须大于 0" }

        val total = input.amountPerItem
            .multiply(input.measure.multiplier)
            .multiply(BigDecimal(input.itemsPerPack))
            .multiply(BigDecimal(input.packCount))
        return UnitPrice(
            family = input.measure.family,
            totalBaseAmount = total,
            yuanPerBaseUnit = input.priceYuan.divide(total, 8, RoundingMode.HALF_UP),
        )
    }

    fun comparison(first: UnitPrice, second: UnitPrice): Comparison? {
        if (first.family != second.family) return null
        return when (first.yuanPerBaseUnit.compareTo(second.yuanPerBaseUnit)) {
            -1 -> Comparison.FIRST_CHEAPER
            1 -> Comparison.SECOND_CHEAPER
            else -> Comparison.TIE
        }
    }
}

enum class Comparison { FIRST_CHEAPER, SECOND_CHEAPER, TIE }
