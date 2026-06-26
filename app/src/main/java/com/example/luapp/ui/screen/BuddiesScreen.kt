package com.example.luapp.ui.screen

import android.content.ContentValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.luapp.data.db.DatabaseHelper
import com.example.luapp.data.model.Buddy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BuddiesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { DatabaseHelper.getInstance(context) }
    val scope = rememberCoroutineScope()

    var buddies by remember { mutableStateOf<List<Buddy>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBuddy by remember { mutableStateOf<Buddy?>(null) }
    var buddyToDelete by remember { mutableStateOf<Buddy?>(null) }
    var dialogName by remember { mutableStateOf("") }

    suspend fun loadBuddies() {
        buddies = withContext(Dispatchers.IO) {
            val list = mutableListOf<Buddy>()
            val cursor = db.readableDatabase.rawQuery("SELECT id, name FROM buddies ORDER BY name", null)
            while (cursor.moveToNext()) {
                list.add(Buddy(cursor.getLong(0), cursor.getString(1)))
            }
            cursor.close()
            list
        }
    }

    LaunchedEffect(Unit) { loadBuddies() }

    Box(modifier = modifier.fillMaxSize()) {
        if (buddies.isEmpty()) {
            Text(
                "No hay chicas registradas",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(buddies, key = { it.id }) { buddy ->
                    ListItem(
                        headlineContent = {
                            Text(buddy.name, style = MaterialTheme.typography.bodyLarge)
                        },
                        leadingContent = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = buddy.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editingBuddy = buddy; dialogName = buddy.name }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { buddyToDelete = buddy }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        FloatingActionButton(
            onClick = { dialogName = ""; showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar chica")
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar chica") },
            text = {
                OutlinedTextField(
                    value = dialogName,
                    onValueChange = { dialogName = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = dialogName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val values = ContentValues().apply { put("name", dialogName.trim()) }
                                db.writableDatabase.insert("buddies", null, values)
                            }
                            loadBuddies()
                            showAddDialog = false
                        }
                    }
                ) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }

    editingBuddy?.let { buddy ->
        AlertDialog(
            onDismissRequest = { editingBuddy = null },
            title = { Text("Editar chica") },
            text = {
                OutlinedTextField(
                    value = dialogName,
                    onValueChange = { dialogName = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = dialogName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val values = ContentValues().apply { put("name", dialogName.trim()) }
                                db.writableDatabase.update("buddies", values, "id = ?", arrayOf(buddy.id.toString()))
                            }
                            loadBuddies()
                            editingBuddy = null
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editingBuddy = null }) { Text("Cancelar") }
            }
        )
    }

    buddyToDelete?.let { buddy ->
        AlertDialog(
            onDismissRequest = { buddyToDelete = null },
            title = { Text("Eliminar") },
            text = { Text("¿Eliminar a ${buddy.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            db.writableDatabase.delete("buddies", "id = ?", arrayOf(buddy.id.toString()))
                        }
                        loadBuddies()
                        buddyToDelete = null
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { buddyToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}
