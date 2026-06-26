package com.example.luapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.luapp.ui.screen.BuddiesScreen
import com.example.luapp.ui.screen.CashRegisterDetailScreen
import com.example.luapp.ui.screen.ConsumptionScreen
import com.example.luapp.ui.screen.ExpenseScreen
import com.example.luapp.ui.screen.HistoryScreen
import com.example.luapp.ui.theme.LuAppTheme

@OptIn(ExperimentalMaterial3Api::class)
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
                    data class TabItem(val label: String, val icon: ImageVector)

                    val tabs = listOf(
                        TabItem("Consumos", Icons.Default.ShoppingCart),
                        TabItem("Historial", Icons.Default.DateRange),
                        TabItem("Gastos", Icons.Default.List),
                        TabItem("Chicas", Icons.Default.Person),
                    )

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(tabs[selectedTab].label) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                tabs.forEachIndexed { index, tab ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) }
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
