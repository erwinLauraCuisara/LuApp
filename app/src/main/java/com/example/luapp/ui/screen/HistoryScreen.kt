package com.example.luapp.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.luapp.data.db.DatabaseHelper
import com.example.luapp.data.model.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PAGE_SIZE = 10

@Composable
fun HistoryScreen(onSelectRegister: (Long) -> Unit = {}, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { DatabaseHelper.getInstance(context) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val listState = rememberLazyListState()

    var items by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasMore by remember { mutableStateOf(true) }
    var offset by remember { mutableIntStateOf(0) }

    suspend fun loadMore() {
        if (isLoading && items.isNotEmpty()) return
        if (!hasMore) return
        isLoading = true
        val newItems = withContext(Dispatchers.IO) {
            val list = mutableListOf<HistoryItem>()
            val cursor = db.readableDatabase.rawQuery("""
                SELECT
                    cr.id,
                    cr.opened_at,
                    cr.closed_at,
                    (SELECT COALESCE(SUM(amount), 0) FROM consumptions WHERE cash_register_id = cr.id),
                    (SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE cash_register_id = cr.id),
                    (SELECT COALESCE(SUM(pending_amount), 0) FROM consumptions WHERE cash_register_id = cr.id AND pending_amount > 0)
                FROM cash_registers cr
                WHERE cr.closed_at IS NOT NULL
                ORDER BY cr.closed_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent(), arrayOf(PAGE_SIZE.toString(), offset.toString()))
            while (cursor.moveToNext()) {
                list.add(HistoryItem(
                    id = cursor.getLong(0),
                    openedAt = cursor.getLong(1),
                    closedAt = cursor.getLong(2),
                    consumptionTotal = cursor.getDouble(3),
                    expenseTotal = cursor.getDouble(4),
                    debtTotal = cursor.getDouble(5)
                ))
            }
            cursor.close()
            list
        }
        items = items + newItems
        offset += newItems.size
        hasMore = newItems.size == PAGE_SIZE
        isLoading = false
    }

    LaunchedEffect(Unit) { loadMore() }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= items.size - 3 && !isLoading && hasMore && items.isNotEmpty()
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadMore()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!isLoading && items.isEmpty()) {
            Text(
                "Sin cierres de caja aún",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(onClick = { onSelectRegister(item.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Caja #${item.id}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    dateFormat.format(Date(item.closedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "Apertura: ${dateFormat.format(Date(item.openedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Consumos", style = MaterialTheme.typography.bodyMedium)
                                Text("Bs %.2f".format(item.consumptionTotal), style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gastos", style = MaterialTheme.typography.bodyMedium)
                                Text("Bs %.2f".format(item.expenseTotal), style = MaterialTheme.typography.bodyMedium)
                            }
                            if (item.debtTotal > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Deudas pendientes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                    Text("Bs %.2f".format(item.debtTotal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                            val net = item.consumptionTotal - item.expenseTotal
                            val netColor = if (net >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Neto", style = MaterialTheme.typography.titleSmall, color = netColor)
                                Text(
                                    "Bs %.2f".format(net),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = netColor
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
