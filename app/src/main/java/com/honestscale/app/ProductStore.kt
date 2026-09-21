package com.honestscale.app

import android.content.Context

/** 商品清单的轻量持久化：退出 / 进程重建后恢复上一次的录入与结果。 */
object ProductStore {
    private const val PREFS = "honest_scale"
    private const val KEY_PRODUCTS = "products"
    private const val FIELD_SEP = '\u0002'
    private const val ENTRY_SEP = '\u0001'

    fun save(context: Context, products: List<ProductEntry>) {
        val encoded = products.joinToString(ENTRY_SEP.toString()) { entry ->
            listOf(
                entry.id, entry.name, entry.price, entry.amount,
                entry.unit.ordinal, entry.itemsPerPack, entry.packCount,
            ).joinToString(FIELD_SEP.toString())
        }
        prefs(context).edit().putString(KEY_PRODUCTS, encoded).commit()
    }

    fun load(context: Context): List<ProductEntry>? = try {
        prefs(context).getString(KEY_PRODUCTS, null)
            ?.takeIf { it.isNotEmpty() }
            ?.split(ENTRY_SEP)
            ?.map { raw ->
                val v = raw.split(FIELD_SEP)
                ProductEntry(
                    id = v[0].toLong(), name = v[1], price = v[2], amount = v[3],
                    unit = MeasureKind.entries[v[4].toInt()],
                    itemsPerPack = v[5], packCount = v[6],
                )
            }?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
