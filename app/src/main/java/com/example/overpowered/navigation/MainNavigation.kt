package com.example.overpowered

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overpowered.ui.theme.OVERPOWEREDTheme

enum class Tab { Today, Rewards, Shop }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    var tab by remember { mutableStateOf(Tab.Today) }

    Scaffold(
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { tab = Tab.Today },
                containerColor = if (tab == Tab.Today)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Filled.List, contentDescription = "Rewards")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            BottomAppBar(
                actions = {
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { tab = Tab.Rewards },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Today",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.weight(2f))
                    IconButton(
                        onClick = { tab = Tab.Shop },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "Shop",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            )
        }
    ) { inner ->
        Surface(Modifier.padding(inner)) {
            when (tab) {
                Tab.Rewards -> Screen("Rewards")
                Tab.Today   -> Screen("Today")
                Tab.Shop    -> Screen("Shop")
            }
        }
    }
}

@Composable
private fun Screen(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(24.dp)
    )
}

@Composable
fun Greeting(name: String) {
    androidx.compose.material3.Text(
        text = "Hello $name!",
        style = MaterialTheme.typography.headlineMedium
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OVERPOWEREDTheme {
        Greeting("Android")
    }
}
