package com.example.overpowered.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import com.example.overpowered.shop.ShopItem

@Composable
fun EditProfileScreen(
    playerName: String,
    profileImageUrl: String?,
    purchasedItems: Set<String>,
    selectedFrame: String?,
    selectedTitle: String?,
    selectedTheme: String?,
    onPlayerNameChange: (String) -> Unit,
    onProfileImageChange: (Uri?) -> Unit,
    onFrameSelect: (String?) -> Unit,
    onTitleSelect: (String?) -> Unit,
    onThemeSelect: (String?) -> Unit,
    onBackClick: () -> Unit
) {
    val (tempName, setTempName) = remember { mutableStateOf(playerName) }

    // All available items (same as shop data)
    val allFrames = listOf(
        ShopItem("frame_1", "Autumn", 10, "Frames", Color(0xFFFF6B35)),
        ShopItem("frame_2", "Confetti", 10, "Frames", Color(0xFF4ECDC4)),
        ShopItem("frame_3", "Classic", 10, "Frames", Color(0xFF45B7D1))
    )
    val allTitles = listOf(
        ShopItem("title_1", "Overpowered", 5, "Titles", Color(0xFF96CEB4)),
        ShopItem("title_2", "Legendary", 5, "Titles", Color(0xFFFECB52)),
        ShopItem("title_3", "Epic", 5, "Titles", Color(0xFFFF6B6B))
    )
    val allThemes = listOf(
        ShopItem("theme_1", "Ocean", 15, "Themes", Color(0xFF4A90E2)),
        ShopItem("theme_2", "Flame", 15, "Themes", Color(0xFFE74C3C)),
        ShopItem("theme_3", "Void", 15, "Themes", Color(0xFF2C3E50))
    )

    // Filter to only owned items
    val ownedFrames = allFrames.filter { it.id in purchasedItems }
    val ownedTitles = allTitles.filter { it.id in purchasedItems }
    val ownedThemes = allThemes.filter { it.id in purchasedItems }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onProfileImageChange(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 130.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)

    ) {
        // Back button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Back to Profile",
                    tint = Color(0xFF4A5568),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Picture (editable)
        Card(
            modifier = Modifier
                .size(120.dp)
                .clickable { imagePickerLauncher.launch("image/*") },
            shape = RoundedCornerShape(60.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF667EEA))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = profileImageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(60.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }

        // Change Photo button
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF667EEA)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Change Photo",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Edit form section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5568)
                )

                // Player Name field
                Column{
                    Text(
                        text = "Player Name",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = setTempName,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667EEA),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Customization Sections

            // Frame Selection
            CustomizationSection(
                title = "Frame:",
                items = ownedFrames,
                selectedItemId = selectedFrame,
                onItemSelect = onFrameSelect,
                emptyMessage = "Visit the shop to buy frames!"
            )

            // Title Selection
            CustomizationSection(
                title = "Title:",
                items = ownedTitles,
                selectedItemId = selectedTitle,
                onItemSelect = onTitleSelect,
                emptyMessage = "Visit the shop to buy titles!"
            )

            // Theme Selection
            CustomizationSection(
                title = "Theme:",
                items = ownedThemes,
                selectedItemId = selectedTheme,
                onItemSelect = onThemeSelect,
                emptyMessage = "Visit the shop to buy themes!"
            )

            // Save button
            Button(
                onClick = {
                    onPlayerNameChange(tempName)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF48BB78)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Save Changes",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CustomizationSection(
    title: String,
    items: List<ShopItem>,
    selectedItemId: String?,
    onItemSelect: (String?) -> Unit,
    emptyMessage: String
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4A5568),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        if (items.isEmpty()) {
            // Show empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No items owned",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = emptyMessage,
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
                    )
                }
            }
        } else {
            // Show owned items in horizontal scroll
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                // Add "None" option first
                item {
                    SelectableItemCard(
                        item = null,
                        isSelected = selectedItemId == null,
                        onSelect = { onItemSelect(null) }
                    )
                }

                items(items) { item ->
                    SelectableItemCard(
                        item = item,
                        isSelected = selectedItemId == item.id,
                        onSelect = { onItemSelect(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableItemCard(
    item: ShopItem?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Item preview card
        val cardModifier = if (isSelected) {
            Modifier
                .size(100.dp)
                .selectable(
                    selected = isSelected,
                    onClick = onSelect
                )
                .border(
                    3.dp,
                    Color(0xFF667EEA),
                    RoundedCornerShape(12.dp)
                )
        } else {
            Modifier
                .size(100.dp)
                .selectable(
                    selected = isSelected,
                    onClick = onSelect
                )
        }

        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = item?.color ?: Color(0xFFE2E8F0)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (item == null) {
                    // "None" option
                    Text(
                        text = "None",
                        color = Color(0xFF4A5568),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Item name
        Text(
            text = item?.name ?: "None",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF667EEA) else Color(0xFF4A5568)
        )
    }
}