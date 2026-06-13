package com.example.luapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.luapp.ui.screen.BuddiesScreen
import com.example.luapp.ui.screen.CashRegisterDetailScreen
import com.example.luapp.ui.screen.ConsumptionScreen
import com.example.luapp.ui.screen.ExpenseScreen
import com.example.luapp.ui.screen.HistoryScreen
import com.example.luapp.ui.theme.LuAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuAppTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var detailCashRegisterId by remember { mutableStateOf<Long?>(null) }

                if (detailCashRegisterId != null) {
                    CashRegisterDetailScreen(
                        cashRegisterId = detailCashRegisterId!!,
                        onBack = { detailCashRegisterId = null }
                    )
                } else {
                    val tabs = listOf("Consumos", "Historial", "Gastos", "Chicas")

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                tabs.forEachIndexed { index, label ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        icon = {},
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        when (selectedTab) {
                            0 -> ConsumptionScreen(modifier = Modifier.padding(innerPadding))
                            1 -> HistoryScreen(
                                onSelectRegister = { id -> detailCashRegisterId = id },
                                modifier = Modifier.padding(innerPadding)
                            )
                            2 -> ExpenseScreen(modifier = Modifier.padding(innerPadding))
                            3 -> BuddiesScreen(modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}
