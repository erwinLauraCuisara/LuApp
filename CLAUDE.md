# CLAUDE.md — Reglas y arquitectura del proyecto

## Stack
- Android con Jetpack Compose + Material3
- Kotlin
- AGP 9.2.1, Kotlin 2.2.20
- **Sin Room** — SQLite directo con `SQLiteOpenHelper` (AGP 9.x es incompatible con KSP/Room)
- Sin Navigation component — navegación por estado en `MainActivity`
- Sin iconos extendidos (`material-icons-extended`) — usar texto o los iconos básicos del core

## Base de datos
- Helper: `data/db/DatabaseHelper.kt` — singleton, versión actual: **6**
- Versión de la BD en `DatabaseHelper` debe incrementarse con cada cambio de schema
- `onUpgrade` usa migraciones incrementales (`if (oldVersion < N)`) excepto que se indique limpiar todo
- FK activadas en `onConfigure` con `setForeignKeyConstraintsEnabled(true)`
- Métodos de negocio de caja van en `DatabaseHelper`: `getActiveCashRegisterId()`, `getOrCreateActiveCashRegisterId()`, `closeCashRegister(id)`

## Arquitectura de navegación
- `MainActivity` tiene un `var detailCashRegisterId: Long?`
- Si es non-null → muestra `CashRegisterDetailScreen` (pantalla completa, sin bottom nav)
- Si es null → muestra el `Scaffold` con `NavigationBar` de 4 tabs
- No usar Navigation component ni fragments

## Pantallas (tabs)
0. Consumos — `ConsumptionScreen`
1. Historial — `HistoryScreen`
2. Gastos — `ExpenseScreen`
3. Chicas — `BuddiesScreen`

## Modelos (`data/model/`)
- `Buddy(id, name)`
- `ConsumptionItem(id, concept, customerName?, buddyIds, buddyNames, amount, amountQr, appointmentFee, pendingAmount?, details?, createdAt)`
- `ExpenseItem(id, concept, amount, details?, createdAt)`
- `HistoryItem(id, openedAt, closedAt, consumptionTotal, expenseTotal, debtTotal)`

## Moneda
- Símbolo: `Bs` (no `$`)
- Formato: `"Bs %.2f".format(amount)`

## Patrones de UI
- Lista vacía: `Text` centrado en un `Box(Modifier.weight(1f).fillMaxWidth())`
- FAB: siempre fuera del `if/else` de la lista, dentro del `Box` con `Alignment.BottomEnd`
- Formularios: `ModalBottomSheet` con `skipPartiallyExpanded = true` y `Modifier.fillMaxHeight()`
- Botones del form (Guardar/Cancelar): fuera del scroll, fijos con `HorizontalDivider` encima
- Keyboard: `imePadding()` en el Column del sheet, campos en Column con `weight(1f).verticalScroll()`
- Detalle de item: `AlertDialog` con Editar en `confirmButton`, Cerrar en `dismissButton`, Eliminar como `TextButton` dentro del `text`
- Confirmación de borrado: `AlertDialog` separado

## DB — operaciones
- Siempre en `Dispatchers.IO` con `withContext`
- `loadData()` debe ser suspend fun definida dentro del Composable
- Reload inmediato después de insert/update/delete

## Convenciones
- Todo el código en inglés (nombres de variables, funciones, clases, tablas SQL)
- Strings de UI en español
- Sin comentarios excepto cuando el WHY no es obvio
- No agregar features no pedidas
