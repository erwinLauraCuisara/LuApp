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
    var cFormAmount by remember { mutableStateOf("") }
    var cFormBuddyId by remember { mutableStateOf<Long?>(null) }
    var cFormBuddyName by remember { mutableStateOf("") }
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

    var viewingBuddyPayment by remember { mutableStateOf<Pair<String, List<ConsumptionItem>>?>(null) }

    fun resetConsumptionForm() {
        cFormConcept = ""; cFormCustomerName = ""; cFormAmount = ""
        cFormBuddyId = null; cFormBuddyName = ""; cFormFee = ""
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
                SELECT c.id, c.concept, c.customer_name, c.buddy_id, b.name, c.amount, c.appointment_fee, c.pending_amount, c.details, c.created_at
                FROM consumptions c LEFT JOIN buddies b ON c.buddy_id = b.id
                WHERE c.cash_register_id = ? ORDER BY c.created_at DESC
            """.trimIndent(), arrayOf(cashRegisterId.toString()))
            while (cCursor.moveToNext()) {
                cList.add(ConsumptionItem(
                    id = cCursor.getLong(0), concept = cCursor.getString(1),
                    customerName = if (cCursor.isNull(2)) null else cCursor.getString(2),
                    buddyId = if (cCursor.isNull(3)) null else cCursor.getLong(3),
                    buddyName = if (cCursor.isNull(4)) null else cCursor.getString(4),
                    amount = cCursor.getDouble(5), appointmentFee = cCursor.getDouble(6),
                    pendingAmount = if (cCursor.isNull(7)) null else cCursor.getDouble(7),
                    details = if (cCursor.isNull(8)) null else cCursor.getString(8),
                    createdAt = cCursor.getLong(9)
                ))
            }
            cCursor.close()
            consumptions = cList

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

    val totalConsumptions = consumptions.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val totalDebts = consumptions.sumOf { it.pendingAmount ?: 0.0 }
    val buddyPayments = consumptions
        .filter { it.buddyId != null }
        .groupBy { it.buddyId!! }
        .map { (_, items) ->
            val name = items.first().buddyName.orEmpty().ifEmpty { "Sin nombre" }
            name to items.sortedByDescending { it.createdAt }
        }
        .sortedBy { it.first }

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
                items(buddyPayments, key = { it.first }) { (name, items) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text("${items.size} consumo${if (items.size != 1) "s" else ""}") },
                        trailingContent = { Text("Bs %.2f".format(items.sumOf { it.appointmentFee })) },
                        modifier = Modifier.clickable { viewingBuddyPayment = name to items }
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
                                if (item.buddyName != null) Text(item.buddyName, style = MaterialTheme.typography.bodySmall)
                                if (item.pendingAmount != null && item.pendingAmount > 0)
                                    Text("⚠ Con deuda", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingContent = { Text("Bs %.2f".format(item.amount)) },
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
    viewingBuddyPayment?.let { (name, items) ->
        AlertDialog(
            onDismissRequest = { viewingBuddyPayment = null },
            title = { Text(name) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total a pagar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Bs %.2f".format(items.sumOf { it.appointmentFee }), style = MaterialTheme.typography.labelMedium)
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(item.concept, style = MaterialTheme.typography.bodyMedium)
                                Text(dateFormat.format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Bs %.2f".format(item.appointmentFee))
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
                    if (item.buddyName != null) Text("Chica: ${item.buddyName}")
                    Text("Monto: Bs %.2f".format(item.amount))
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
                    cFormAmount = item.amount.toString(); cFormBuddyId = item.buddyId
                    cFormBuddyName = item.buddyName ?: ""
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
        val canSave = cFormConcept.isNotBlank() && cFormAmount.toDoubleOrNull() != null
        ModalBottomSheet(onDismissRequest = { showConsumptionForm = false; resetConsumptionForm() }, sheetState = consumptionSheetState, modifier = Modifier.fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Editar consumo", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = cFormConcept, onValueChange = { cFormConcept = it }, label = { Text("Concepto *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = cFormAmount, onValueChange = { cFormAmount = it }, label = { Text("Monto *") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, prefix = { Text("Bs") })
                    OutlinedCard(onClick = { buddySearch = ""; showBuddyPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Chica (opcional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (cFormBuddyName.isNotBlank()) cFormBuddyName else "Sin asignar")
                            }
                            if (cFormBuddyId != null) TextButton(onClick = { cFormBuddyId = null; cFormBuddyName = "" }) { Text("Quitar") }
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
                            val amount = cFormAmount.toDoubleOrNull() ?: return@launch
                            withContext(Dispatchers.IO) {
                                val values = ContentValues().apply {
                                    put("concept", cFormConcept.trim())
                                    if (cFormCustomerName.isNotBlank()) put("customer_name", cFormCustomerName.trim()) else putNull("customer_name")
                                    put("amount", amount)
                                    put("appointment_fee", cFormFee.toDoubleOrNull() ?: 0.0)
                                    if (cFormBuddyId != null) put("buddy_id", cFormBuddyId) else putNull("buddy_id")
                                    val pending = cFormPending.toDoubleOrNull()
                                    if (pending != null && pending > 0) put("pending_amount", pending) else putNull("pending_amount")
                                    if (cFormDetails.isNotBlank()) put("details", cFormDetails.trim()) else putNull("details")
                                }
                                db.writableDatabase.update("consumptions", values, "id = ?", arrayOf(editingConsumptionId.toString()))
                            }
                            loadData(); showConsumptionForm = false; resetConsumptionForm()
                        }
                    }) { Text("Guardar") }
                }
            }
        }
    }

    // ─── Buddy picker ───
    if (showBuddyPicker) {
        val filtered = buddies.filter { it.name.contains(buddySearch, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showBuddyPicker = false },
            title = { Text("Seleccionar chica") },
            text = {
                Column {
                    OutlinedTextField(value = buddySearch, onValueChange = { buddySearch = it }, label = { Text("Buscar") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        if (filtered.isEmpty()) {
                            item { Text("Sin resultados", modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(filtered, key = { it.id }) { buddy ->
                                Text(buddy.name, modifier = Modifier.fillMaxWidth().clickable { cFormBuddyId = buddy.id; cFormBuddyName = buddy.name; showBuddyPicker = false }.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {},
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
