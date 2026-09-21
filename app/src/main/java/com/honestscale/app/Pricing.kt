package com.honestscale.app

import androidx.compose.runtime.Immutable
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

/** BigDecimal 会破坏稳定性推断，此注解保证 Compose 可以按值跳过未变化的重组。 */
@Immutable
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

    /** 每个单位族（重量 / 体积）中单位价最低的一个；跨族商品互不比较。 */
    fun cheapestPerFamily(prices: List<UnitPrice>): Map<MeasureKind.Family, UnitPrice> =
        prices.groupBy { it.family }.mapValues { (_, group) -> group.minBy { it.yuanPerBaseUnit } }
}
