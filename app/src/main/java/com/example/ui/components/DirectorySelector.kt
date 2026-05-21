package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DirectorySelector(
    selectedType: String,
    customName: String,
    onTypeChange: (String) -> Unit,
    onCustomNameChange: (String) -> Unit,
    resolvedPathPreview: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF222225), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "DOWNLOAD DIRECTORY SETTINGS",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Options list
            val options = listOf(
                FolderOption("Internal Sandbox", "Requires zero android storage permissions. Fastest & ultra-safe.", "Private"),
                FolderOption("Public Downloads", "Saves to phone's shared Download folder. Visible in other apps.", "Public"),
                FolderOption("Python Workspace", "Saves to an isolated virtual project environment folder.", "Python")
            )

            options.forEach { opt ->
                val isSelected = opt.name == selectedType
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0x15FF2C3B) else Color(0xFF1F1F24))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFFFF2C3B) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onTypeChange(opt.name) }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                            contentDescription = "Folder",
                            tint = if (isSelected) Color(0xFFFF2C3B) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = opt.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) Color(0xFFFF2C3B) else Color(0xFF2C2C32),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = opt.tag,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = opt.description,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Folder Name field
            if (selectedType != "Python Workspace") {
                Text(
                    text = "Subfolder Name",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = customName,
                    onValueChange = onCustomNameChange,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1F1F24),
                        unfocusedContainerColor = Color(0xFF1F1F24),
                        focusedIndicatorColor = Color(0xFFFF2C3B),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    singleLine = true,
                    placeholder = { Text("e.g. TubePy_Assets") }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Path Preview Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F12), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "info",
                        tint = Color(0xFFFF2C3B),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "RESOLVED SYSTEM SAVING LOCATION",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resolvedPathPreview,
                    color = Color.Green,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

data class FolderOption(
    val name: String,
    val description: String,
    val tag: String
)
