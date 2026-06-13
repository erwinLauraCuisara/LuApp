package com.example.luapp.ui.screen

import android.content.ContentValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.luapp.data.model.ConsumptionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { DatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var activeCashRegisterId by remember { mutableStateOf<Long?>(null) }
    var cashRegisterOpenedAt by remember { mutableStateOf<Long?>(null) }
    var consumptions by remember { mutableStateOf<List<ConsumptionItem>>(emptyList()) }
    var buddies by remember { mutableStateOf<List<Buddy>>(emptyList()) }

    var showForm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var viewingItem by remember { mutableStateOf<ConsumptionItem?>(null) }
    var deletingItem by remember { mutableStateOf<ConsumptionItem?>(null) }
    var showCloseConfirm by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var totalExpenses by remember { mutableStateOf(0.0) }

    var formConcept by remember { mutableStateOf("") }
    var formCustomerName by remember { mutableStateOf("") }
    var formAmount by remember { mutableStateOf("") }
    var formBuddyId by remember { mutableStateOf<Long?>(null) }
    var formBuddyName by remember { mutableStateOf("") }
    var formFee by remember { mutableStateOf("") }
    var formPendingAmount by remember { mutableStateOf("") }
    var formDetails by remember { mutableStateOf("") }
    var showBuddyPicker by remember { mutableStateOf(false) }
    var buddySearch by remember { mutableStateOf("") }

    fun resetForm() {
        formConcept = ""; formCustomerName = ""; formAmount = ""; formBuddyId = null
        formBuddyName = ""; formFee = ""; formPendingAmount = ""; formDetails = ""; editingId = null
    }

    suspend fun loadData() {
        val (regId, regOpenedAt) = withContext(Dispatchers.IO) {
            val cursor = db.readableDatabase.rawQuery(
                "SELECT id, opened_at FROM cash_registers WHERE closed_at IS NULL LIMIT 1", null
            )
            val result = if (cursor.moveToFirst()) Pair(cursor.getLong(0), cursor.getLong(1)) else Pair(null, null)
            cursor.close()
            result
        }
        activeCashRegisterId = regId
        cashRegisterOpenedAt = regOpenedAt

        consumptions = withContext(Dispatchers.IO) {
            if (regId == null) return@withContext emptyList()
            val list = mutableListOf<ConsumptionItem>()
            val cursor = db.readableDatabase.rawQuery("""
                SELECT c.id, c.concept, c.customer_name, c.buddy_id, b.name, c.amount, c.appointment_fee, c.pending_amount, c.details, c.created_at
                FROM consumptions c
                LEFT JOIN buddies b ON c.buddy_id = b.id
                WHERE c.cash_register_id = ?
                ORDER BY c.created_at DESC
            """.trimIndent(), arrayOf(regId.toString()))
            while (cursor.moveToNext()) {
                list.add(ConsumptionItem(
                    id = cursor.getLong(0),
                    concept = cursor.getString(1),
                    customerName = if (cursor.isNull(2)) null else cursor.getString(2),
                    buddyId = if (cursor.isNull(3)) null else cursor.getLong(3),
                    buddyName = if (cursor.isNull(4)) null else cursor.getString(4),
                    amount = cursor.getDouble(5),
                    appointmentFee = cursor.getDouble(6),
                    pendingAmount = if (cursor.isNull(7)) null else cursor.getDouble(7),
                    details = if (cursor.isNull(8)) null else cursor.getString(8),
                    createdAt = cursor.getLong(9)
                ))
            }
            cursor.close()
            list
        }
        buddies = withContext(Dispatchers.IO) {
            val list = mutableListOf<Buddy>()
            val cursor = db.readableDatabase.rawQuery("SELECT id, name FROM buddies ORDER BY name", null)
            while (cursor.moveToNext()) { list.add(Buddy(cursor.getLong(0), cursor.getString(1))) }
            cursor.close()
            list
        }
        totalExpenses = withContext(Dispatchers.IO) {
            if (regId == null) return@withContext 0.0
            val cursor = db.readableDatabase.rawQuery(
                "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE cash_register_id = ?",
                arrayOf(regId.toString())
            )
            val total = if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
            cursor.close()
            total
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val totalAmount = consumptions.sumOf { it.amount }
    val totalDebts = consumptions.sumOf { it.pendingAmount ?: 0.0 }
    val buddyPayments = consumptions
        .filter { it.buddyId != null }
        .groupBy { it.buddyId!! }
        .map { (_, items) ->
            val name = items.first().buddyName.orEmpty().ifEmpty { "Sin nombre" }
            name to items.sortedByDescending { it.createdAt }
        }
        .sortedBy { it.first }

    Column(modifier = modifier.fillMaxSize()) {
        // Header totals + close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable(enabled = activeCashRegisterId != null) { showPreview = true },
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Total: Bs %.2f".format(totalAmount), style = MaterialTheme.typography.titleMedium)
                if (totalDebts > 0)
                    Text(
                        "Deudas por cobrar: Bs %.2f".format(totalDebts),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                if (cashRegisterOpenedAt != null)
                    Text(
                        "Desde ${dateFormat.format(Date(cashRegisterOpenedAt!!))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                if (activeCashRegisterId != null)
                    Text(
                        "Ver resumen →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
            }
            if (activeCashRegisterId != null) {
                TextButton(onClick = { showCloseConfirm = true }) {
                    Text("Cerrar caja", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        HorizontalDivider()

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (consumptions.isEmpty()) {
                Text(
                    "No hay consumos",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(consumptions, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = {
                                Text(item.concept, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.customerName != null)
                                        Text(item.customerName, style = MaterialTheme.typography.bodySmall)
                                    if (item.buddyName != null)
                                        Text(item.buddyName, style = MaterialTheme.typography.bodySmall)
                                    if (item.pendingAmount != null && item.pendingAmount > 0)
                                        Text(
                                            "⚠ Con deuda",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                }
                            },
                            trailingContent = { Text("Bs %.2f".format(item.amount), style = MaterialTheme.typography.bodyLarge) },
                            modifier = Modifier.clickable { viewingItem = item }
                        )
                        HorizontalDivider()
                    }
                }
            }

            FloatingActionButton(
                onClick = { resetForm(); showForm = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }

    // --- Close cash register confirmation ---
    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("Cerrar caja") },
            text = { Text("¿Confirmar cierre de caja? Los consumos y gastos actuales pasarán al historial.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            activeCashRegisterId?.let { id ->
                                withContext(Dispatchers.IO) { db.closeCashRegister(id) }
                            }
                            showCloseConfirm = false
                            loadData()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Cerrar caja") }
            },
            dismissButton = { TextButton(onClick = { showCloseConfirm = false }) { Text("Cancelar") } }
        )
    }

    // --- Detail dialog ---
    viewingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { viewingItem = null },
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
                    Text(
                        dateFormat.format(Date(item.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { deletingItem = item; viewingItem = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Eliminar") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingId = item.id
                    formConcept = item.concept
                    formCustomerName = item.customerName ?: ""
                    formAmount = item.amount.toString()
                    formBuddyId = item.buddyId
                    formBuddyName = item.buddyName ?: ""
                    formFee = if (item.appointmentFee > 0) item.appointmentFee.toString() else ""
                    formPendingAmount = if (item.pendingAmount != null && item.pendingAmount > 0) item.pendingAmount.toString() else ""
                    formDetails = item.details ?: ""
                    viewingItem = null
                    showForm = true
                }) { Text("Editar") }
            },
            dismissButton = { TextButton(onClick = { viewingItem = null }) { Text("Cerrar") } }
        )
    }

    // --- Add / Edit bottom sheet ---
    if (showForm) {
        val canSave = formConcept.isNotBlank() && formAmount.toDoubleOrNull() != null

        ModalBottomSheet(
            onDismissRequest = { showForm = false; resetForm() },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (editingId == null) "Nuevo consumo" else "Editar consumo",
                        style = MaterialTheme.typography.titleLarge
                    )
                    OutlinedTextField(
                        value = formConcept, onValueChange = { formConcept = it },
                        label = { Text("Concepto *") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = formAmount, onValueChange = { formAmount = it },
                        label = { Text("Monto *") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, prefix = { Text("Bs") }
                    )
                    OutlinedCard(
                        onClick = { buddySearch = ""; showBuddyPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Chica (opcional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (formBuddyName.isNotBlank()) formBuddyName else "Sin asignar")
                            }
                            if (formBuddyId != null)
                                TextButton(onClick = { formBuddyId = null; formBuddyName = "" }) { Text("Quitar") }
                        }
                    }
                    OutlinedTextField(
                        value = formCustomerName, onValueChange = { formCustomerName = it },
                        label = { Text("Nombre cliente (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = formFee, onValueChange = { formFee = it },
                        label = { Text("Comisión de cita (opcional)") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, prefix = { Text("Bs") }
                    )
                    OutlinedTextField(
                        value = formPendingAmount, onValueChange = { formPendingAmount = it },
                        label = { Text("Monto pendiente / Deuda (opcional)") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, prefix = { Text("Bs") }
                    )
                    OutlinedTextField(
                        value = formDetails, onValueChange = { formDetails = it },
                        label = { Text("Detalles (opcional)") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 5
                    )
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showForm = false; resetForm() }) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = canSave,
                        onClick = {
                            scope.launch {
                                val amount = formAmount.toDoubleOrNull() ?: return@launch
                                val fee = formFee.toDoubleOrNull() ?: 0.0
                                val pending = formPendingAmount.toDoubleOrNull()
                                withContext(Dispatchers.IO) {
                                    val cashId = db.getOrCreateActiveCashRegisterId()
                                    val values = ContentValues().apply {
                                        put("cash_register_id", cashId)
                                        put("concept", formConcept.trim())
                                        if (formCustomerName.isNotBlank()) put("customer_name", formCustomerName.trim()) else putNull("customer_name")
                                        put("amount", amount)
                                        put("appointment_fee", fee)
                                        if (formBuddyId != null) put("buddy_id", formBuddyId) else putNull("buddy_id")
                                        if (pending != null && pending > 0) put("pending_amount", pending) else putNull("pending_amount")
                                        if (formDetails.isNotBlank()) put("details", formDetails.trim()) else putNull("details")
                                    }
                                    if (editingId == null)
                                        db.writableDatabase.insert("consumptions", null, values)
                                    else
                                        db.writableDatabase.update("consumptions", values, "id = ?", arrayOf(editingId.toString()))
                                }
                                loadData()
                                showForm = false
                                resetForm()
                            }
                        }
                    ) { Text(if (editingId == null) "Agregar" else "Guardar") }
                }
            }
        }
    }

    // --- Buddy picker ---
    if (showBuddyPicker) {
        val filtered = buddies.filter { it.name.contains(buddySearch, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showBuddyPicker = false },
            title = { Text("Seleccionar chica") },
            text = {
                Column {
                    OutlinedTextField(
                        value = buddySearch, onValueChange = { buddySearch = it },
                        label = { Text("Buscar por nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        if (filtered.isEmpty()) {
                            item { Text("Sin resultados", modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(filtered, key = { it.id }) { buddy ->
                                Text(
                                    text = buddy.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { formBuddyId = buddy.id; formBuddyName = buddy.name; showBuddyPicker = false }
                                        .padding(vertical = 12.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
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

    // --- Active register preview ---
    if (showPreview && activeCashRegisterId != null && cashRegisterOpenedAt != null) {
        ModalBottomSheet(
            onDismissRequest = { showPreview = false },
            sheetState = previewSheetState,
            modifier = Modifier.fillMaxHeight()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Resumen de caja activa", style = MaterialTheme.typography.titleLarge) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Apertura: ${dateFormat.format(Date(cashRegisterOpenedAt!!))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Consumos"); Text("Bs %.2f".format(totalAmount))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gastos"); Text("Bs %.2f".format(totalExpenses))
                            }
                            if (totalDebts > 0)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Deudas pendientes", color = MaterialTheme.colorScheme.error)
                                    Text("Bs %.2f".format(totalDebts), color = MaterialTheme.colorScheme.error)
                                }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Neto", style = MaterialTheme.typography.titleSmall)
                                Text("Bs %.2f".format(totalAmount - totalExpenses), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
                item { Text("Pago a chicas", style = MaterialTheme.typography.titleMedium) }
                if (buddyPayments.isEmpty()) {
                    item {
                        Text(
                            "Sin chicas asignadas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                } else {
                    items(buddyPayments, key = { it.first }) { (name, items) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text("${items.size} consumo${if (items.size != 1) "s" else ""}") },
                            trailingContent = { Text("Bs %.2f".format(items.sumOf { it.appointmentFee })) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // --- Delete confirmation ---
    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Eliminar consumo") },
            text = { Text("¿Eliminar \"${item.concept}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { db.writableDatabase.delete("consumptions", "id = ?", arrayOf(item.id.toString())) }
                            loadData()
                            deletingItem = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deletingItem = null }) { Text("Cancelar") } }
        )
    }
}
