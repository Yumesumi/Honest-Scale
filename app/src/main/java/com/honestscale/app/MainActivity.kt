package com.honestscale.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableHighRefreshRate()
        enableEdgeToEdge()
        setContent {
            HonestScaleTheme {
                CompareScreen()
            }
        }
    }
}

/** 一件商品的录入状态；字段保持字符串，编辑过程中不强行解析。 */
data class ProductEntry(
    val id: Long,
    val name: String = "",
    val price: String = "",
    val amount: String = "",
    val unit: MeasureKind = MeasureKind.MILLILITRE,
    val itemsPerPack: String = "1",
    val packCount: String = "1",
)

private data class Quote(val unitPrice: UnitPrice? = null)

private fun ProductEntry.quote() = try {
    Quote(
        Pricing.calculate(
            ProductInput(
                priceYuan = BigDecimal(price.trim()),
                amountPerItem = BigDecimal(amount.trim()),
                measure = unit,
                itemsPerPack = itemsPerPack.trim().toInt(),
                packCount = packCount.trim().toInt(),
            )
        )
    )
} catch (_: Exception) {
    Quote()
}

/** 7 个字段一行摊平保存 / 按块还原，进程重建与旋转不丢数据。 */
private val productsSaver = listSaver<List<ProductEntry>, Any>(
    save = { products ->
        products.flatMap {
            listOf<Any>(it.id, it.name, it.price, it.amount, it.unit.ordinal, it.itemsPerPack, it.packCount)
        }
    },
    restore = { flat ->
        flat.chunked(7).map { v ->
            ProductEntry(
                id = v[0] as Long, name = v[1] as String, price = v[2] as String, amount = v[3] as String,
                unit = MeasureKind.entries[v[4] as Int], itemsPerPack = v[5] as String, packCount = v[6] as String,
            )
        }
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompareScreen() {
    var products by rememberSaveable(stateSaver = productsSaver) {
        mutableStateOf(
            listOf(
                ProductEntry(1, name = "牛奶（箱装）", price = "48", amount = "250", itemsPerPack = "12"),
                ProductEntry(2, name = "牛奶（单卖）", price = "6", amount = "500"),
            )
        )
    }
    val quotes = products.map { it.quote() }
    val winners = Pricing.cheapestPerFamily(quotes.mapNotNull { it.unitPrice })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("心里那杆秤", fontWeight = FontWeight.Bold)
                        Text("剥掉包装，看清真实单价", style = MaterialTheme.typography.bodySmall)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(products, key = { _, entry -> entry.id }) { index, entry ->
                val quote = quotes[index]
                val familyValidCount = quote.unitPrice
                    ?.let { up -> quotes.count { it.unitPrice?.family == up.family } } ?: 0
                val isWinner = quote.unitPrice?.let { up ->
                    val winner = winners[up.family]
                    familyValidCount > 1 && winner != null &&
                        up.yuanPerBaseUnit.compareTo(winner.yuanPerBaseUnit) == 0
                } ?: false

                ProductCard(
                    entry = entry,
                    quote = quote,
                    isWinner = isWinner,
                    canDelete = products.size > 1,
                    onEdit = { updated -> products = products.map { if (it.id == entry.id) updated else it } },
                    onDelete = { products = products.filterNot { it.id == entry.id } },
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                OutlinedButton(
                    onClick = { products = products + ProductEntry((products.maxOfOrNull { it.id } ?: 0L) + 1) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("添加商品")
                }
            }
            item {
                ConclusionCard(products = products, quotes = quotes)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(
    entry: ProductEntry,
    quote: Quote,
    isWinner: Boolean,
    canDelete: Boolean,
    onEdit: (ProductEntry) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unitPrice = quote.unitPrice
    val border by animateColorAsState(
        targetValue = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "cardBorder",
    )

    OutlinedCard(modifier = modifier.fillMaxWidth(), border = BorderStroke(1.dp, border)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = entry.name,
                    onValueChange = { onEdit(entry.copy(name = it)) },
                    label = { Text("商品名称") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(visible = canDelete, enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            }
            OutlinedTextField(
                value = entry.price,
                onValueChange = { onEdit(entry.copy(price = it)) },
                label = { Text("总价（元）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = entry.amount,
                onValueChange = { onEdit(entry.copy(amount = it)) },
                label = { Text("每件规格") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                MeasureKind.entries.forEachIndexed { i, kind ->
                    SegmentedButton(
                        selected = entry.unit == kind,
                        onClick = { onEdit(entry.copy(unit = kind)) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = MeasureKind.entries.size),
                    ) {
                        Text(kind.displayName)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = entry.itemsPerPack,
                    onValueChange = { onEdit(entry.copy(itemsPerPack = it)) },
                    label = { Text("每包含件数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = entry.packCount,
                    onValueChange = { onEdit(entry.copy(packCount = it)) },
                    label = { Text("购买包数") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    if (unitPrice == null) {
                        Text(
                            "填写有效的价格与规格",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        val priceColor by animateColorAsState(
                            targetValue = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            label = "priceColor",
                        )
                        val price = unitPrice.yuanPerBaseUnit
                            .setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                        Text(
                            "单位价 $price 元 / ${unitPrice.family.baseUnit}",
                            color = priceColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "总量 ${format(unitPrice.totalBaseAmount)} ${unitPrice.family.baseUnit}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isWinner,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "最划算",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConclusionCard(products: List<ProductEntry>, quotes: List<Quote>) {
    val valid = quotes.mapIndexedNotNull { i, q -> q.unitPrice?.let { i to it } }
    val winners = Pricing.cheapestPerFamily(valid.map { it.second })
    fun displayName(index: Int) = products[index].name.ifBlank { "商品 ${'A' + index}" }

    val text = when {
        valid.isEmpty() -> "填写价格与规格后，各单位价会自动算出"
        valid.size == 1 -> "各单位价已算出——再添加一件同类商品，就能开秤比较"
        else -> winners.map { (family, winner) ->
            val tied = valid
                .filter { (_, price) -> price.family == family && price.yuanPerBaseUnit.compareTo(winner.yuanPerBaseUnit) == 0 }
                .joinToString("、") { (index, _) -> displayName(index) }
            "${family.baseUnit} 组最划算：$tied"
        }.joinToString("\n")
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        AnimatedContent(
            targetState = text,
            transitionSpec = { (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut()) },
            label = "conclusion",
        ) { value ->
            Text(
                value,
                Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun format(value: BigDecimal) = value.stripTrailingZeros().toPlainString()

/** 请求屏幕在当前分辨率下的最高刷新率，避免被系统默认锁在 60Hz。 */
private fun android.app.Activity.enableHighRefreshRate() {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display
    } else {
        @Suppress("DEPRECATION") windowManager.defaultDisplay
    }
    val mode = display?.supportedModes
        ?.filter { it.physicalWidth == display.mode.physicalWidth && it.physicalHeight == display.mode.physicalHeight }
        ?.maxByOrNull { it.refreshRate } ?: return
    window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
}
