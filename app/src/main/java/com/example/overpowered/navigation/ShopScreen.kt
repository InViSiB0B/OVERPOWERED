package com.example.overpowered.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val category: String,
    val color: Color = Color(0xFF667EEA),
    val description: String = ""
)

@Composable
fun ShopScreen(
    playerMoney: Int,
    onPurchase: (Int) -> Unit
) {
    // Sample data for each category
    val frames = listOf(
        ShopItem("frame_1", "Autumn", 10, "Frames", Color(0xFFFF6B35)),
        ShopItem("frame_2", "Confetti", 10, "Frames", Color(0xFF4ECDC4)),
        ShopItem("frame_3", "Classic", 10, "Frames", Color(0xFF45B7D1))
    )

    val titles = listOf(
        ShopItem("title_1", "Overpowered", 5, "Titles", Color(0xFF96CEB4)),
        ShopItem("title_2", "Legendary", 5, "Titles", Color(0xFFFECB52)),
        ShopItem("title_3", "Epic", 5, "Titles", Color(0xFFFF6B6B))
    )

    val themes = listOf(
        ShopItem("theme_1", "Ocean", 15, "Themes", Color(0xFF4A90E2)),
        ShopItem("theme_2", "Flame", 15, "Themes", Color(0xFFE74C3C)),
        ShopItem("theme_3", "Void", 15, "Themes", Color(0xFF2C3E50))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Frames Section
        item {
            ShopCategory(
                title = "Frames:",
                items = frames,
                playerMoney = playerMoney,
                onPurchase = onPurchase
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Titles Section
        item {
            ShopCategory(
                title = "Titles:",
                items = titles,
                playerMoney = playerMoney,
                onPurchase = onPurchase
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Themes Section
        item {
            ShopCategory(
                title = "Themes:",
                items = themes,
                playerMoney = playerMoney,
                onPurchase = onPurchase
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ShopCategory(
    title: String,
    items: List<ShopItem>,
    playerMoney: Int,
    onPurchase: (Int) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4A5568),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(items) { item ->
                ShopItemCard(
                    item = item,
                    playerMoney = playerMoney,
                    onPurchase = onPurchase
                )
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    playerMoney: Int,
    onPurchase: (Int) -> Unit
) {
    var showInsufficientFundsDialog by remember { mutableStateOf(false) }
    val canAfford = playerMoney >= item.price

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Item preview card
        Card(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = item.color),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (item.category) {
                    "Frames" -> {
                        // Frame preview - border design
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .background(
                                    Color.White.copy(alpha = 0.9f),
                                    RoundedCornerShape(8.dp)
                                )
                        )
                    }
                    "Titles" -> {
                        // Title preview - text
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    "Themes" -> {
                        // Theme preview - color blocks
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Price button
        Button(
            onClick = {
                if (canAfford) {
                    onPurchase(item.price)
                } else {
                    showInsufficientFundsDialog = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canAfford) Color(0xFFE2E8F0) else Color(0xFFE2E8F0).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .height(32.dp)
                .width(80.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = "Buy",
                    modifier = Modifier.size(14.dp),
                    tint = if (canAfford) Color(0xFF4A5568) else Color(0xFF4A5568).copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.price.toString(),
                    color = if (canAfford) Color(0xFF4A5568) else Color(0xFF4A5568).copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Insufficient funds dialog
    if (showInsufficientFundsDialog) {
        AlertDialog(
            onDismissRequest = { showInsufficientFundsDialog = false },
            title = {
                Text(
                    text = "Not Enough Money",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "You need ${item.price} but only have ${playerMoney}.",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete some tasks to earn more money!",
                        fontSize = 14.sp,
                        color = Color(0xFF667EEA),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showInsufficientFundsDialog = false }
                ) {
                    Text("Got it!")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}