package com.honestscale.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PricingTest {
    @Test fun `calculates nested package in millilitres`() {
        val actual = Pricing.calculate(ProductInput(BigDecimal("48"), BigDecimal("250"), MeasureKind.MILLILITRE, 12))
        assertEquals(BigDecimal("3000"), actual.totalBaseAmount)
        assertEquals(0, BigDecimal("0.016").compareTo(actual.yuanPerBaseUnit))
    }

    @Test fun `converts kilograms to grams`() {
        val actual = Pricing.calculate(ProductInput(BigDecimal("25"), BigDecimal("2"), MeasureKind.KILOGRAM, 3))
        assertEquals(BigDecimal("6000"), actual.totalBaseAmount)
    }

    @Test fun `finds the cheaper product`() {
        val box = Pricing.calculate(ProductInput(BigDecimal("48"), BigDecimal("250"), MeasureKind.MILLILITRE, 12))
        val single = Pricing.calculate(ProductInput(BigDecimal("6"), BigDecimal("500"), MeasureKind.MILLILITRE))
        assertEquals(Comparison.SECOND_CHEAPER, Pricing.comparison(box, single))
    }
}
