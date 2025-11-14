package com.example.overpowered.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.foundation.border
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import com.example.overpowered.data.FrameCatalog
import com.example.overpowered.data.ProfileFrame
import com.example.overpowered.data.Title
import com.example.overpowered.data.TitleCatalog

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

    // Get actual frames from FrameCatalog
    val allFrames = FrameCatalog.getAllFrames()

    // Get actual titles from TitleCatalog
    val allTitles = TitleCatalog.getAllTitles()

    val allThemes = listOf(
        CustomizationItem("theme_1", "Ocean", "Themes"),
        CustomizationItem("theme_2", "Flame", "Themes"),
        CustomizationItem("theme_3", "Void", "Themes")
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

        // Profile Picture with Frame (editable)
        FramedProfilePicture(
            profileImageUrl = profileImageUrl,
            frameId = selectedFrame,
            size = 120.dp,
            modifier = Modifier.clickable { imagePickerLauncher.launch("image/*") }
        )

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
                Column {
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

            // Frame Selection
            FrameCustomizationSection(
                title = "Frame:",
                frames = ownedFrames,
                selectedFrameId = selectedFrame,
                onFrameSelect = onFrameSelect,
                emptyMessage = "Visit the shop to buy frames!"
            )

            // Title Selection
            TitleCustomizationSection(
                title = "Title:",
                items = ownedTitles,
                selectedItemId = selectedTitle,
                onItemSelect = onTitleSelect,
                emptyMessage = "Visit the shop to buy titles!"
            )

            // Theme Selection
            ThemeCustomizationSection(
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

// Simple data class for titles and themes (until you create proper catalogs)
data class CustomizationItem(
    val id: String,
    val name: String,
    val category: String
)

// Frame-specific customization section
@Composable
fun FrameCustomizationSection(
    title: String,
    frames: List<ProfileFrame>,
    selectedFrameId: String?,
    onFrameSelect: (String?) -> Unit,
    emptyMessage: String
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4A5568),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        if (frames.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No frames owned",
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                // Add "None" option
                item {
                    SelectableFrameCard(
                        frame = null,
                        isSelected = selectedFrameId == null,
                        onSelect = { onFrameSelect(null) }
                    )
                }

                items(frames) { frame ->
                    SelectableFrameCard(
                        frame = frame,
                        isSelected = selectedFrameId == frame.id,
                        onSelect = { onFrameSelect(frame.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableFrameCard(
    frame: ProfileFrame?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val cardModifier = if (isSelected) {
            Modifier
                .size(100.dp)
                .selectable(selected = isSelected, onClick = onSelect)
                .border(3.dp, Color(0xFF667EEA), RoundedCornerShape(12.dp))
        } else {
            Modifier
                .size(100.dp)
                .selectable(selected = isSelected, onClick = onSelect)
        }

        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (frame == null) {
                    Text(
                        text = "None",
                        color = Color(0xFF4A5568),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    // Show frame preview using FramedProfilePicture
                    FramedProfilePicture(
                        profileImageUrl = null,
                        frameId = frame.id,
                        size = 80.dp,
                        iconSize = 40.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = frame?.name ?: "None",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF667EEA) else Color(0xFF4A5568)
        )
    }
}

// Keep your existing sections for titles and themes
@Composable
fun TitleCustomizationSection(
    title: String,
    items: List<Title>,
    selectedItemId: String?,
    onItemSelect: (String?) -> Unit,
    emptyMessage: String
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4A5568),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No titles owned",
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                item {
                    SelectableTitleCard(
                        item = null,
                        isSelected = selectedItemId == null,
                        onSelect = { onItemSelect(null) }
                    )
                }

                items(items) { item ->
                    SelectableTitleCard(
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
fun SelectableTitleCard(
    item: Title?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val cardModifier = if (isSelected) {
            Modifier
                .size(100.dp)
                .selectable(selected = isSelected, onClick = onSelect)
                .border(3.dp, Color(0xFF667EEA), RoundedCornerShape(12.dp))
        } else {
            Modifier
                .size(100.dp)
                .selectable(selected = isSelected, onClick = onSelect)
        }

        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF96CEB4)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item?.name ?: "None",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item?.name ?: "None",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF667EEA) else Color(0xFF4A5568)
        )
    }
}

@Composable
fun ThemeCustomizationSection(
    title: String,
    items: List<CustomizationItem>,
    selectedItemId: String?,
    onItemSelect: (String?) -> Unit,
    emptyMessage: String
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4A5568),
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        if (items.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No themes owned",
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                item {
                    SelectableThemeCard(
                        item = null,
                        isSelected = selectedItemId == null,
                        onSelect = { onItemSelect(null) }
                    )
                }

                items(items) { item ->
                    SelectableThemeCard(
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
fun SelectableThemeCard(
    item: CustomizationItem?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val cardModifier = if (isSelected) {
            Modifier
                .size(100.dp)
                .selectable(selected = isSelected, onClick = onSelect)
                .border(3.dp, Color(0xFF667EEA), RoundedCornerShape(12.dp))
        } else {
            Modifier
                .size(100.dp)
                .selectable(selected = isSelected, onClick = onSelect)
        }

        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4A90E2)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item?.name ?: "None",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item?.name ?: "None",
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF667EEA) else Color(0xFF4A5568)
        )
    }
}