package com.example.overpowered.shop

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
import com.example.overpowered.data.FrameCatalog
import com.example.overpowered.profile.components.FramedProfilePicture
import java.util.*
import kotlin.random.Random

data class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val category: String,
    val color: Color = Color(0xFF667EEA),
    val description: String = ""
)

// Catalog of all available items (placeholder still)
object ShopCatalog {
    val allFrames = FrameCatalog.getAllFrames().map { frame ->
        ShopItem(
            id = frame.id,
            name = frame.name,
            price = frame.price,
            category = "Frames",
            color = Color(0xFF667EEA), // Preview color
            description = ""
        )
    }

    val allTitles = listOf(
        ShopItem("title_1", "Overpowered", 5, "Titles", Color(0xFF96CEB4)),
        ShopItem("title_2", "Legendary", 5, "Titles", Color(0xFFFECB52)),
        ShopItem("title_3", "Epic", 5, "Titles", Color(0xFFFF6B6B)),
        ShopItem("title_4", "Master", 5, "Titles", Color(0xFF9B59B6)),
        ShopItem("title_5", "Champion", 5, "Titles", Color(0xFFF39C12)),
        ShopItem("title_6", "Elite", 5, "Titles", Color(0xFF1ABC9C)),
        ShopItem("title_7", "Supreme", 5, "Titles", Color(0xFFE74C3C)),
        ShopItem("title_8", "Divine", 5, "Titles", Color(0xFFFDD835)),
    )

    val allThemes = listOf(
        ShopItem("theme_1", "Ocean", 15, "Themes", Color(0xFF4A90E2)),
        ShopItem("theme_2", "Flame", 15, "Themes", Color(0xFFE74C3C)),
        ShopItem("theme_3", "Void", 15, "Themes", Color(0xFF2C3E50)),
        ShopItem("theme_4", "Aurora", 15, "Themes", Color(0xFF9C27B0)),
        ShopItem("theme_5", "Desert", 15, "Themes", Color(0xFFE67E22)),
        ShopItem("theme_6", "Frost", 15, "Themes", Color(0xFF00BCD4)),
        ShopItem("theme_7", "Jungle", 15, "Themes", Color(0xFF4CAF50)),
        ShopItem("theme_8", "Storm", 15, "Themes", Color(0xFF607D8B)),
    )
}

// Get today's date in EST for shop rotation
fun getTodayDateEST(): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
}

// Get daily shop rotation based on date seed
fun getDailyShopItems(): Triple<List<ShopItem>, List<ShopItem>, List<ShopItem>> {
    val dateSeed = getTodayDateEST().hashCode().toLong()
    val random = Random(dateSeed)

    // Select 5 random items from each category
    val dailyFrames = ShopCatalog.allFrames.shuffled(random).take(5)
    val dailyTitles = ShopCatalog.allTitles.shuffled(random).take(5)
    val dailyThemes = ShopCatalog.allThemes.shuffled(random).take(5)

    return Triple(dailyFrames, dailyTitles, dailyThemes)
}

@Composable
fun ShopScreen(
    playerMoney: Int,
    purchasedItems: Set<String>,
    onPurchase: (Int, String) -> Unit
) {
    // Get today's shop rotation
    val (allFrames, allTitles, allThemes) = remember(getTodayDateEST()) {
        getDailyShopItems()
    }

    // Filter out purchased items
    val frames = allFrames.filter { it.id !in purchasedItems }
    val titles = allTitles.filter { it.id !in purchasedItems }
    val themes = allThemes.filter { it.id !in purchasedItems }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            // Shop header with reset timer
            ShopHeader()
        }

        // Frames Section - only show if items available
        if (frames.isNotEmpty()) {
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
        }

        // Titles Section - only show if items available
        if (titles.isNotEmpty()) {
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
        }

        // Themes Section - only show if items available
        if (themes.isNotEmpty()) {
            item {
                ShopCategory(
                    title = "Themes:",
                    items = themes,
                    playerMoney = playerMoney,
                    onPurchase = onPurchase
                )
            }
        }

        // Show message when all items purchased
        if (frames.isEmpty() && titles.isEmpty() && themes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉",
                            fontSize = 48.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Items Purchased!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A5568),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You've bought everything in the shop. Check back later for new items!",
                            fontSize = 14.sp,
                            color = Color(0xFF718096),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ShopHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily Shop",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = getTimeUntilMidnightEST(),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

// Calculate time until midnight EST
fun getTimeUntilMidnightEST(): String {
    val now = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
    val midnight = Calendar.getInstance(TimeZone.getTimeZone("America/New_York")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_MONTH, 1)
    }

    val diff = midnight.timeInMillis - now.timeInMillis
    val hours = (diff / (1000 * 60 * 60)) % 24
    val minutes = (diff / (1000 * 60)) % 60

    return "Resets in ${hours}h ${minutes}m"
}

@Composable
fun ShopCategory(
    title: String,
    items: List<ShopItem>,
    playerMoney: Int,
    onPurchase: (Int, String) -> Unit
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
    onPurchase: (Int, String) -> Unit
) {
    var showInsufficientFundsDialog by remember { mutableStateOf(false) }
    val canAfford = playerMoney >= item.price

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        // Show actual frame preview
                        FramedProfilePicture(
                            profileImageUrl = null,
                            frameId = item.id,
                            size = 80.dp,
                            iconSize = 40.dp
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

        Text(
            text = item.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4A5568),
            textAlign = TextAlign.Center
        )

        // Price button
        Button(
            onClick = {
                if (canAfford) {
                    onPurchase(item.price, item.id)
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
                        text = "You need $${item.price} but only have $${playerMoney}.",
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