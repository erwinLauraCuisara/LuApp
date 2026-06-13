# LuApp — Resumen del proyecto

## Qué es
App Android para administrar el negocio de un local nocturno/bar. Registra consumos de clientes, gastos del local, y las chicas (hostesses/buddies) asociadas a cada consumo. Todo se agrupa en "cajas" que se abren y cierran como cortes de turno.

## Schema de la BD (SQLite, versión 4)

```
cash_registers   → id, opened_at, closed_at (NULL = activa)
buddies          → id, name
consumptions     → id, cash_register_id*, concept, customer_name?, buddy_id?, amount,
                   appointment_fee, pending_amount?, details?, created_at
expenses         → id, cash_register_id*, concept, amount, details?, created_at
```
`*` FK a cash_registers. La caja activa es la que tiene `closed_at IS NULL`.

## Lógica de caja (cash register)
- Siempre existe máximo una caja activa
- Al insertar consumo o gasto: `getOrCreateActiveCashRegisterId()` busca la activa o crea una
- "Cerrar caja": setea `closed_at` en la caja activa → los registros pasan al historial
- Los screens de Consumos y Gastos filtran por `cash_register_id = activaCajaId`
- El historial muestra solo cajas con `closed_at NOT NULL`

## Pantallas implementadas

### ConsumptionScreen
- Lista consumos de la caja activa
- Header fijo: total Bs, deudas por cobrar (si hay), fecha apertura, botón "Cerrar caja"
- Click en item → AlertDialog con detalle + editar/eliminar
- FAB (+) → ModalBottomSheet con form: concepto*, monto*, chica (picker con búsqueda), nombre cliente, comisión, monto pendiente, detalles
- Flag "⚠ Con deuda" en lista si pending_amount > 0

### ExpenseScreen
- Igual que ConsumptionScreen pero más simple (sin buddy, sin deuda)
- Campos: concepto*, monto*, detalles

### HistoryScreen
- Lista de cajas cerradas como Cards clickeables
- Cada card: Caja #N, fecha cierre, totales de consumos/gastos/deudas, neto
- Click → navega a CashRegisterDetailScreen

### CashRegisterDetailScreen
- Pantalla completa (sin bottom nav) con TopAppBar y "← Atrás"
- Card resumen: fechas, consumos, gastos, deudas, neto
- Sección consumos (read-only)
- Sección gastos (read-only)
- Navegación: `detailCashRegisterId` en MainActivity, BackHandler intercepta el botón físico

### BuddiesScreen
- Lista de buddies (hostesses) con nombre
- Agregar/editar/eliminar con AlertDialogs
- Se usan como picker en ConsumptionScreen

## Estructura de archivos relevantes
```
data/
  db/DatabaseHelper.kt        ← singleton SQLite, métodos de caja
  model/
    Buddy.kt
    ConsumptionItem.kt
    ExpenseItem.kt
    HistoryItem.kt
ui/
  screen/
    ConsumptionScreen.kt
    ExpenseScreen.kt
    HistoryScreen.kt
    BuddiesScreen.kt
    CashRegisterDetailScreen.kt
  theme/                      ← generado por Android Studio, no tocar
MainActivity.kt               ← maneja tab y detailCashRegisterId
```

## Decisiones técnicas clave
- **Sin Room/KSP**: AGP 9.2.1 es incompatible → SQLite directo con DatabaseHelper
- **Sin Navigation component**: estado simple en MainActivity con `detailCashRegisterId: Long?`
- **Sin material-icons-extended**: causa conflictos de build → usar texto o básicos del core
- **ModalBottomSheet para forms**: más espacio que AlertDialog para formularios con varios campos
- **Botones fijos sobre teclado**: columna scrollable con `weight(1f)` + botones fuera del scroll con `imePadding()` en el contenedor
