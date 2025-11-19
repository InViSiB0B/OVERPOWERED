package com.example.overpowered.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overpowered.data.FriendRequest
import com.example.overpowered.profile.components.FramedProfilePicture
import com.example.overpowered.profile.components.StyledTitle

@Composable
fun FriendRequestsDialog(
    friendRequests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onIgnore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Notifications",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A5568)
            )
        },
        text = {
            if (friendRequests.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\uD83D\uDD14",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5568),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When something of note happens, it will appear here.",
                        fontSize = 14.sp,
                        color = Color(0xFF718096),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friendRequests) { request ->
                        FriendRequestItem(
                            request = request,
                            onAccept = { onAccept(request) },
                            onIgnore = { onIgnore(request.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onIgnore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile picture (with frame if you want)
        FramedProfilePicture(
            profileImageUrl = request.fromProfileImageUrl,
            frameId = request.fromSelectedFrame,
            size = 40.dp,
            iconSize = 20.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = request.fromUserName.split("#")[0],
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5568)
                )
                // Show title if available
                request.fromSelectedTitle?.let { titleId ->
                    StyledTitle(
                        titleId = titleId,
                        fontSize = 12.sp,
                        includeBackground = true
                    )
                }
            }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "wants to be your friend",
                    fontSize = 14.sp,
                    color = Color(0xFF718096)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Accept button
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFF48BB78)
                    )
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Ignore button
                IconButton(
                    onClick = onIgnore,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFE53E3E)
                    )
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Ignore",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }