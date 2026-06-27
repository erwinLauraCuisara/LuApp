package com.example.luapp.ui.screen

import android.content.ContentValues
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.core.content.FileProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.luapp.data.db.DatabaseHelper
import com.example.luapp.data.model.Buddy
import com.example.luapp.ui.pdf.generateCashRegisterPdf
import com.example.luapp.data.model.ConsumptionItem
import com.example.luapp.data.model.ExpenseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRegisterDetailScreen(cashRegisterId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { DatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val consumptionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expenseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var openedAt by remember { mutableStateOf(0L) }
    var closedAt by remember { mutableStateOf(0L) }
    var consumptions by remember { mutableStateOf<List<ConsumptionItem>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<ExpenseItem>>(emptyList()) }
    var buddies by remember { mutableStateOf<List<Buddy>>(emptyList()) }

    // Consumption detail/edit/delete state
    var viewingConsumption by remember { mutableStateOf<ConsumptionItem?>(null) }
    var deletingConsumption by remember { mutableStateOf<ConsumptionItem?>(null) }
    var showConsumptionForm by remember { mutableStateOf(false) }
    var editingConsumptionId by remember { mutableStateOf<Long?>(null) }
    var cFormConcept by remember { mutableStateOf("") }
    var cFormCustomerName by remember { mutableStateOf("") }
    var cFormAmountCash by remember { mutableStateOf("") }
    var cFormAmountQr by remember { mutableStateOf("") }
    var cFormBuddies by remember { mutableStateOf<List<Buddy>>(emptyList()) }
    var cTempBuddies by remember { mutableStateOf<List<Buddy>>(emptyList()) }
    var cFormFee by remember { mutableStateOf("") }
    var cFormPending by remember { mutableStateOf("") }
    var cFormDetails by remember { mutableStateOf("") }
    var showBuddyPicker by remember { mutableStateOf(false) }
    var buddySearch by remember { mutableStateOf("") }

    // Expense detail/edit/delete state
    var viewingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var deletingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var showExpenseForm by remember { mutableStateOf(false) }
    var editingExpenseId by remember { mutableStateOf<Long?>(null) }
    var eFormConcept by remember { mutableStateOf("") }
    var eFormAmount by remember { mutableStateOf("") }
    var eFormDetails by remember { mutableStateOf("") }

    var viewingBuddyPayment by remember { mutableStateOf<Triple<String, List<ConsumptionItem>, Double>?>(null) }

    fun resetConsumptionForm() {
        cFormConcept = ""; cFormCustomerName = ""; cFormAmountCash = ""; cFormAmountQr = ""
        cFormBuddies = emptyList(); cFormFee = ""
        cFormPending = ""; cFormDetails = ""; editingConsumptionId = null
    }

    fun resetExpenseForm() {
        eFormConcept = ""; eFormAmount = ""; eFormDetails = ""; editingExpenseId = null
    }

    suspend fun loadData() {
        withContext(Dispatchers.IO) {
            val regCursor = db.readableDatabase.rawQuery(
                "SELECT opened_at, closed_at FROM cash_registers WHERE id = ?",
                arrayOf(cashRegisterId.toString())
            )
            if (regCursor.moveToFirst()) { openedAt = regCursor.getLong(0); closedAt = regCursor.getLong(1) }
            regCursor.close()

            val cList = mutableListOf<ConsumptionItem>()
            val cCursor = db.readableDatabase.rawQuery("""
                SELECT id, concept, customer_name, amount, appointment_fee, pending_amount, details, created_at, amount_qr
                FROM consumptions
                WHERE cash_register_id = ? ORDER BY created_at DESC
            """.trimIndent(), arrayOf(cashRegisterId.toString()))
            while (cCursor.moveToNext()) {
                cList.add(ConsumptionItem(
                    id = cCursor.getLong(0), concept = cCursor.getString(1),
                    customerName = if (cCursor.isNull(2)) null else cCursor.getString(2),
                    amount = cCursor.getDouble(3), appointmentFee = cCursor.getDouble(4),
                    pendingAmount = if (cCursor.isNull(5)) null else cCursor.getDouble(5),
                    details = if (cCursor.isNull(6)) null else cCursor.getString(6),
                    createdAt = cCursor.getLong(7),
                    amountQr = cCursor.getDouble(8)
                ))
            }
            cCursor.close()
            if (cList.isNotEmpty()) {
                val ids = cList.joinToString(",") { it.id.toString() }
                val buddyMap = mutableMapOf<Long, MutableList<Pair<Long, String>>>()
                val cbCursor = db.readableDatabase.rawQuery("""
                    SELECT cb.consumption_id, cb.buddy_id, b.name
                    FROM consumption_buddies cb JOIN buddies b ON cb.buddy_id = b.id
                    WHERE cb.consumption_id IN ($ids)
                    ORDER BY b.name
                """, null)
                while (cbCursor.moveToNext()) {
                    buddyMap.getOrPut(cbCursor.getLong(0)) { mutableListOf() }
                        .add(cbCursor.getLong(1) to cbCursor.getString(2))
                }
                cbCursor.close()
                consumptions = cList.map { c ->
                    val pairs = buddyMap[c.id] ?: emptyList()
                    c.copy(buddyIds = pairs.map { it.first }, buddyNames = pairs.map { it.second })
                }
            } else {
                consumptions = cList
            }

            val eList = mutableListOf<ExpenseItem>()
            val eCursor = db.readableDatabase.rawQuery(
                "SELECT id, concept, amount, details, created_at FROM expenses WHERE cash_register_id = ? ORDER BY created_at DESC",
                arrayOf(cashRegisterId.toString())
            )
            while (eCursor.moveToNext()) {
                eList.add(ExpenseItem(
                    id = eCursor.getLong(0), concept = eCursor.getString(1),
                    amount = eCursor.getDouble(2),
                    details = if (eCursor.isNull(3)) null else eCursor.getString(3),
                    createdAt = eCursor.getLong(4)
                ))
            }
            eCursor.close()
            expenses = eList

            val bList = mutableListOf<Buddy>()
            val bCursor = db.readableDatabase.rawQuery("SELECT id, name FROM buddies ORDER BY name", null)
            while (bCursor.moveToNext()) { bList.add(Buddy(bCursor.getLong(0), bCursor.getString(1))) }
            bCursor.close()
            buddies = bList
        }
    }

    LaunchedEffect(cashRegisterId) { loadData() }
    BackHandler { onBack() }

    val totalConsumptions = consumptions.sumOf { it.amount + it.amountQr }
    val totalCash = consumptions.sumOf { it.amount }
    val totalQr = consumptions.sumOf { it.amountQr }
    val totalExpenses = expenses.sumOf { it.amount }
    val totalDebts = consumptions.sumOf { it.pendingAmount ?: 0.0 }
    val buddyPayments: List<Triple<String, List<ConsumptionItem>, Double>> = run {
        val shareMap = mutableMapOf<Long, Double>()
        val nameMap = mutableMapOf<Long, String>()
        val consMap = mutableMapOf<Long, MutableList<ConsumptionItem>>()
        consumptions.forEach { c ->
            val n = c.buddyIds.size
            if (n == 0) return@forEach
            val share = c.appointmentFee / n
            c.buddyIds.forEachIndexed { i, id ->
                shareMap[id] = (shareMap[id] ?: 0.0) + share
                nameMap[id] = c.buddyNames.getOrElse(i) { "?" }
                consMap.getOrPut(id) { mutableListOf() }.add(c)
            }
        }
        shareMap.keys.map { id ->
            Triple(nameMap[id]!!, consMap[id]!! as List<ConsumptionItem>, shareMap[id]!!)
        }.sortedBy { it.first }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Caja #$cashRegisterId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    TextButton(
                        enabled = closedAt > 0,
                        onClick = {
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    generateCashRegisterPdf(
                                        context, cashRegisterId, openedAt, closedAt, consumptions, expenses
                                    )
                                }
                                val uri = FileProvider.getUriForFile(
                                    context, "${context.packageName}.provider", file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
                            }
                        }
                    ) { Text("PDF") }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Apertura: ${dateFormat.format(Date(openedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Cierre: ${dateFormat.format(Date(closedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Consumos"); Text("Bs %.2f".format(totalConsumptions)) }
                        if (totalQr > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Efectivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Bs %.2f".format(totalCash), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("QR", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Bs %.2f".format(totalQr), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Gastos"); Text("Bs %.2f".format(totalExpenses)) }
                        if (totalDebts > 0)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Deudas pendientes", color = MaterialTheme.colorScheme.error)
                                Text("Bs %.2f".format(totalDebts), color = MaterialTheme.colorScheme.error)
                            }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Neto", style = MaterialTheme.typography.titleSmall)
                            Text("Bs %.2f".format(totalConsumptions - totalExpenses), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            item { Text("Pago a chicas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
            if (buddyPayments.isEmpty()) {
                item { Text("Sin chicas asignadas", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp)) }
            } else {
                items(buddyPayments, key = { it.first }) { (name, cons, total) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text("${cons.size} consumo${if (cons.size != 1) "s" else ""}") },
                        trailingContent = { Text("Bs %.2f".format(total)) },
                        modifier = Modifier.clickable { viewingBuddyPayment = Triple(name, cons, total) }
                    )
                    HorizontalDivider()
                }
            }

            item { Text("Consumos (${consumptions.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
            if (consumptions.isEmpty()) {
                item { Text("Sin consumos", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp)) }
            } else {
                items(consumptions, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.concept, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (item.customerName != null) Text(item.customerName, style = MaterialTheme.typography.bodySmall)
                                if (item.buddyNames.isNotEmpty()) Text(item.buddyNames.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                if (item.pendingAmount != null && item.pendingAmount > 0)
                                    Text("⚠ Con deuda", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingContent = { Text("Bs %.2f".format(item.amount + item.amountQr)) },
                        modifier = Modifier.clickable { viewingConsumption = item }
                    )
                    HorizontalDivider()
                }
            }

            item { Text("Gastos (${expenses.size})", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp)) }
            if (expenses.isEmpty()) {
                item { Text("Sin gastos", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp)) }
            } else {
                items(expenses, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.concept, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingContent = { Text("Bs %.2f".format(item.amount)) },
                        modifier = Modifier.clickable { viewingExpense = item }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // ─── Buddy payment detail dialog ───
    viewingBuddyPayment?.let { (name, items, total) ->
        AlertDialog(
            onDismissRequest = { viewingBuddyPayment = null },
            title = { Text(name) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total a cobrar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Bs %.2f".format(total), style = MaterialTheme.typography.labelMedium)
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    items.sortedByDescending { it.createdAt }.forEach { item ->
                        val share = item.appointmentFee / item.buddyIds.size
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(item.concept, style = MaterialTheme.typography.bodyMedium)
                                Text(dateFormat.format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Bs %.2f".format(share))
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { viewingBuddyPayment = null }) { Text("Cerrar") } }
        )
    }

    // ─── Consumption detail dialog ───
    viewingConsumption?.let { item ->
        AlertDialog(
            onDismissRequest = { viewingConsumption = null },
            title = { Text(item.concept) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.customerName != null) Text("Cliente: ${item.customerName}")
                    if (item.buddyNames.isNotEmpty())
                        Text("Chica${if (item.buddyNames.size > 1) "s" else ""}: ${item.buddyNames.joinToString(", ")}")
                    if (item.amountQr == 0.0) {
                        Text("Monto: Bs %.2f".format(item.amount))
                    } else if (item.amount == 0.0) {
                        Text("Monto QR: Bs %.2f".format(item.amountQr))
                    } else {
                        Text("Efectivo: Bs %.2f".format(item.amount))
                        Text("QR: Bs %.2f".format(item.amountQr))
                    }
                    if (item.appointmentFee > 0) Text("Comisión: Bs %.2f".format(item.appointmentFee))
                    if (item.pendingAmount != null && item.pendingAmount > 0)
                        Text("⚠ Deuda pendiente: Bs %.2f".format(item.pendingAmount), color = MaterialTheme.colorScheme.error)
                    if (!item.details.isNullOrBlank()) Text("Detalles: ${item.details}")
                    Text(dateFormat.format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { deletingConsumption = item; viewingConsumption = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingConsumptionId = item.id
                    cFormConcept = item.concept; cFormCustomerName = item.customerName ?: ""
                    cFormAmountCash = if (item.amount > 0) item.amount.toString() else ""
                    cFormAmountQr = if (item.amountQr > 0) item.amountQr.toString() else ""
                    cFormBuddies = buddies.filter { it.id in item.buddyIds }
                    cFormFee = if (item.appointmentFee > 0) item.appointmentFee.toString() else ""
                    cFormPending = if (item.pendingAmount != null && item.pendingAmount > 0) item.pendingAmount.toString() else ""
                    cFormDetails = item.details ?: ""
                    viewingConsumption = null; showConsumptionForm = true
                }) { Text("Editar") }
            },
            dismissButton = { TextButton(onClick = { viewingConsumption = null }) { Text("Cerrar") } }
        )
    }

    // ─── Consumption edit sheet ───
    if (showConsumptionForm) {
        val canSave = cFormConcept.isNotBlank()
        ModalBottomSheet(onDismissRequest = { showConsumptionForm = false; resetConsumptionForm() }, sheetState = consumptionSheetState, modifier = Modifier.fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Editar consumo", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = cFormConcept, onValueChange = { cFormConcept = it }, label = { Text("Concepto *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = cFormAmountCash, onValueChange = { cFormAmountCash = it }, label = { Text("Monto efectivo (opcional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, prefix = { Text("Bs") })
                    OutlinedTextField(value = cFormAmountQr, onValueChange = { cFormAmountQr = it }, label = { Text("Monto QR (opcional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, prefix = { Text("Bs") })
                    OutlinedCard(onClick = { buddySearch = ""; cTempBuddies = cFormBuddies; showBuddyPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Chicas (opcional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (cFormBuddies.isEmpty()) "Sin asignar" else cFormBuddies.joinToString(", ") { it.name })
                            }
                            if (cFormBuddies.isNotEmpty()) TextButton(onClick = { cFormBuddies = emptyList() }) { Text("Quitar") }
                        }
                    }
                    OutlinedTextField(value = cFormCustomerName, onValueChange = { cFormCustomerName = it }, label = { Text("Nombre cliente (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = cFormFee, onValueChange = { cFormFee = it }, label = { Text("Comisión (opcional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, prefix = { Text("Bs") })
                    OutlinedTextField(value = cFormPending, onValueChange = { cFormPending = it }, label = { Text("Monto pendiente / Deuda (opcional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, prefix = { Text("Bs") })
                    OutlinedTextField(value = cFormDetails, onValueChange = { cFormDetails = it }, label = { Text("Detalles (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showConsumptionForm = false; resetConsumptionForm() }) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = canSave, onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val values = ContentValues().apply {
                                    put("concept", cFormConcept.trim())
                                    if (cFormCustomerName.isNotBlank()) put("customer_name", cFormCustomerName.trim()) else putNull("customer_name")
                                    put("amount", cFormAmountCash.toDoubleOrNull() ?: 0.0)
                                    put("amount_qr", cFormAmountQr.toDoubleOrNull() ?: 0.0)
                                    put("appointment_fee", cFormFee.toDoubleOrNull() ?: 0.0)
                                    putNull("buddy_id")
                                    val pending = cFormPending.toDoubleOrNull()
                                    if (pending != null && pending > 0) put("pending_amount", pending) else putNull("pending_amount")
                                    if (cFormDetails.isNotBlank()) put("details", cFormDetails.trim()) else putNull("details")
                                }
                                db.writableDatabase.update("consumptions", values, "id = ?", arrayOf(editingConsumptionId.toString()))
                                db.writableDatabase.delete("consumption_buddies", "consumption_id = ?", arrayOf(editingConsumptionId.toString()))
                                for (buddy in cFormBuddies) {
                                    db.writableDatabase.insert("consumption_buddies", null, ContentValues().apply {
                                        put("consumption_id", editingConsumptionId!!)
                                        put("buddy_id", buddy.id)
                                    })
                                }
                            }
                            loadData(); showConsumptionForm = false; resetConsumptionForm()
                        }
                    }) { Text("Guardar") }
                }
            }
        }
    }

    // ─── Buddy picker (multi-select) ───
    if (showBuddyPicker) {
        val filtered = buddies.filter { it.name.contains(buddySearch, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showBuddyPicker = false },
            title = { Text("Seleccionar chicas") },
            text = {
                Column {
                    OutlinedTextField(value = buddySearch, onValueChange = { buddySearch = it }, label = { Text("Buscar") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        if (filtered.isEmpty()) {
                            item { Text("Sin resultados", modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(filtered, key = { it.id }) { buddy ->
                                val isSelected = buddy in cTempBuddies
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { cTempBuddies = if (isSelected) cTempBuddies - buddy else cTempBuddies + buddy }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = isSelected, onCheckedChange = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(buddy.name, style = MaterialTheme.typography.bodyLarge)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { cFormBuddies = cTempBuddies; showBuddyPicker = false }) { Text("Listo") } },
            dismissButton = { TextButton(onClick = { showBuddyPicker = false }) { Text("Cancelar") } }
        )
    }

    // ─── Consumption delete confirmation ───
    deletingConsumption?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingConsumption = null },
            title = { Text("Eliminar consumo") },
            text = { Text("¿Eliminar \"${item.concept}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { db.writableDatabase.delete("consumptions", "id = ?", arrayOf(item.id.toString())) }
                        loadData(); deletingConsumption = null
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deletingConsumption = null }) { Text("Cancelar") } }
        )
    }

    // ─── Expense detail dialog ───
    viewingExpense?.let { item ->
        AlertDialog(
            onDismissRequest = { viewingExpense = null },
            title = { Text(item.concept) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Monto: Bs %.2f".format(item.amount))
                    if (!item.details.isNullOrBlank()) Text("Detalles: ${item.details}")
                    Text(dateFormat.format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { deletingExpense = item; viewingExpense = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingExpenseId = item.id
                    eFormConcept = item.concept; eFormAmount = item.amount.toString(); eFormDetails = item.details ?: ""
                    viewingExpense = null; showExpenseForm = true
                }) { Text("Editar") }
            },
            dismissButton = { TextButton(onClick = { viewingExpense = null }) { Text("Cerrar") } }
        )
    }

    // ─── Expense edit sheet ───
    if (showExpenseForm) {
        val canSave = eFormConcept.isNotBlank() && eFormAmount.toDoubleOrNull() != null
        ModalBottomSheet(onDismissRequest = { showExpenseForm = false; resetExpenseForm() }, sheetState = expenseSheetState, modifier = Modifier.fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Editar gasto", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = eFormConcept, onValueChange = { eFormConcept = it }, label = { Text("Concepto *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = eFormAmount, onValueChange = { eFormAmount = it }, label = { Text("Monto *") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, prefix = { Text("Bs") })
                    OutlinedTextField(value = eFormDetails, onValueChange = { eFormDetails = it }, label = { Text("Detalles (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showExpenseForm = false; resetExpenseForm() }) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = canSave, onClick = {
                        scope.launch {
                            val amount = eFormAmount.toDoubleOrNull() ?: return@launch
                            withContext(Dispatchers.IO) {
                                val values = ContentValues().apply {
                                    put("concept", eFormConcept.trim())
                                    put("amount", amount)
                                    if (eFormDetails.isNotBlank()) put("details", eFormDetails.trim()) else putNull("details")
                                }
                                db.writableDatabase.update("expenses", values, "id = ?", arrayOf(editingExpenseId.toString()))
                            }
                            loadData(); showExpenseForm = false; resetExpenseForm()
                        }
                    }) { Text("Guardar") }
                }
            }
        }
    }

    // ─── Expense delete confirmation ───
    deletingExpense?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Eliminar gasto") },
            text = { Text("¿Eliminar \"${item.concept}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { db.writableDatabase.delete("expenses", "id = ?", arrayOf(item.id.toString())) }
                        loadData(); deletingExpense = null
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deletingExpense = null }) { Text("Cancelar") } }
        )
    }
}
