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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.luapp.data.model.ExpenseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { DatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var activeCashRegisterId by remember { mutableStateOf<Long?>(null) }
    var cashRegisterOpenedAt by remember { mutableStateOf<Long?>(null) }
    var expenses by remember { mutableStateOf<List<ExpenseItem>>(emptyList()) }

    var showForm by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var viewingItem by remember { mutableStateOf<ExpenseItem?>(null) }
    var deletingItem by remember { mutableStateOf<ExpenseItem?>(null) }
    var showCloseConfirm by remember { mutableStateOf(false) }

    var formConcept by remember { mutableStateOf("") }
    var formAmount by remember { mutableStateOf("") }
    var formDetails by remember { mutableStateOf("") }

    fun resetForm() {
        formConcept = ""; formAmount = ""; formDetails = ""; editingId = null
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

        expenses = withContext(Dispatchers.IO) {
            if (regId == null) return@withContext emptyList()
            val list = mutableListOf<ExpenseItem>()
            val cursor = db.readableDatabase.rawQuery(
                "SELECT id, concept, amount, details, created_at FROM expenses WHERE cash_register_id = ? ORDER BY created_at DESC",
                arrayOf(regId.toString())
            )
            while (cursor.moveToNext()) {
                list.add(ExpenseItem(
                    id = cursor.getLong(0),
                    concept = cursor.getString(1),
                    amount = cursor.getDouble(2),
                    details = if (cursor.isNull(3)) null else cursor.getString(3),
                    createdAt = cursor.getLong(4)
                ))
            }
            cursor.close()
            list
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val totalAmount = expenses.sumOf { it.amount }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Bs %.2f".format(totalAmount), style = MaterialTheme.typography.headlineMedium)
                    if (cashRegisterOpenedAt != null)
                        Text(
                            "Desde ${dateFormat.format(Date(cashRegisterOpenedAt!!))}",
                            style = MaterialTheme.typography.labelSmall
                        )
                }
                if (activeCashRegisterId != null) {
                    TextButton(onClick = { showCloseConfirm = true }) {
                        Text("Cerrar caja", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (expenses.isEmpty()) {
                Text(
                    "No hay gastos",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(expenses, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.concept, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            trailingContent = { Text("Bs %.2f".format(item.amount), style = MaterialTheme.typography.bodyLarge) },
                            modifier = Modifier.clickable { viewingItem = item }
                        )
                        HorizontalDivider()
                    }
                }
            }

            FloatingActionButton(
                onClick = { resetForm(); showForm = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar gasto")
            }
        }
    }

    // --- Close confirmation ---
    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("Cerrar caja") },
            text = { Text("¿Confirmar cierre de caja? Los gastos actuales pasarán al historial.") },
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
                    Text("Monto: Bs %.2f".format(item.amount))
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
                    formAmount = item.amount.toString()
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
                        if (editingId == null) "Nuevo gasto" else "Editar gasto",
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
                                withContext(Dispatchers.IO) {
                                    val cashId = db.getOrCreateActiveCashRegisterId()
                                    val values = ContentValues().apply {
                                        put("cash_register_id", cashId)
                                        put("concept", formConcept.trim())
                                        put("amount", amount)
                                        if (formDetails.isNotBlank()) put("details", formDetails.trim()) else putNull("details")
                                    }
                                    if (editingId == null)
                                        db.writableDatabase.insert("expenses", null, values)
                                    else
                                        db.writableDatabase.update("expenses", values, "id = ?", arrayOf(editingId.toString()))
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

    // --- Delete confirmation ---
    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Eliminar gasto") },
            text = { Text("¿Eliminar \"${item.concept}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { db.writableDatabase.delete("expenses", "id = ?", arrayOf(item.id.toString())) }
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
