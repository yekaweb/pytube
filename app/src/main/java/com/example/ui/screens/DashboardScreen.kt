package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.models.DownloadItem
import com.example.ui.components.DirectorySelector
import com.example.ui.components.VideoPlayerView
import com.example.viewmodel.DownloaderViewModel
import com.example.viewmodel.YouTubeVideoMeta

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DownloaderViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val urlInput by viewModel.urlInput.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val parsedMeta by viewModel.parsedVideoMeta.collectAsState()
    val progressMap by viewModel.downloadProgress.collectAsState()
    val statusMap by viewModel.downloadStatus.collectAsState()
    val logs by viewModel.downloadLogs.collectAsState()

    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val folderType by viewModel.defaultFolderType.collectAsState()
    val customFolderName by viewModel.customFolderName.collectAsState()

    val isGoogleAuthenticated by viewModel.isGoogleAuthenticated.collectAsState()
    val authenticatedUserEmail by viewModel.authenticatedUserEmail.collectAsState()
    val isAuthDialogShowing by viewModel.isAuthenticationDialogShowing.collectAsState()

    val currentPreviewVideo by viewModel.currentPreviewVideo.collectAsState()
    val downloadHistory by viewModel.downloadHistory.collectAsState()

    val context = LocalContext.current

    // Path preview based on local files configuration
    val resolvedPathPreview = remember(folderType, customFolderName) {
        val app = context.applicationContext
        val rootPath = when (folderType) {
            "Internal Sandbox" -> app.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)?.absolutePath ?: "/storage/emulated/0/Android/data"
            "Public Downloads" -> android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath
            "Python Workspace" -> "${app.filesDir.absolutePath}/python_env/downloads"
            else -> "/storage/emulated/0/Download"
        }
        if (folderType == "Python Workspace") rootPath else "$rootPath/$customFolderName"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Main Top Branding Header
            TubePyBrandingHeader(
                isGoogleAuthenticated = isGoogleAuthenticated,
                authenticatedUserEmail = authenticatedUserEmail,
                onRequestLogin = { viewModel.isAuthenticationDialogShowing.value = true },
                onLogout = { viewModel.logoutGoogle() }
            )

            // Segmented Navigation Bar Mode (Downloader | Library | Python Console)
            TubePyTabBar(
                activeTab = activeTab,
                onTabSelect = { viewModel.activeTab.value = it },
                downloadsCount = downloadHistory.size
            )

            // Core Screen Area depending on chosen selector tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> DownloaderTabContent(
                        urlInput = urlInput,
                        onUrlChange = { viewModel.urlInput.value = it },
                        onResolve = { viewModel.parseInputUrl() },
                        isLoading = searchLoading,
                        parsedMeta = parsedMeta,
                        progressMap = progressMap,
                        statusMap = statusMap,
                        selectedQuality = defaultQuality,
                        onQualityChange = { viewModel.defaultQuality.value = it },
                        onDownloadSingle = { meta ->
                            viewModel.triggerDownload(
                                videoId = meta.videoId,
                                title = meta.title,
                                channel = meta.channel,
                                duration = meta.duration,
                                thumbnailUrl = meta.thumbnailUrl,
                                quality = defaultQuality
                            )
                        },
                        onDownloadPlaylist = { viewModel.triggerPlaylistDownload() },
                        onTogglePlaylistItem = { viewModel.togglePlaylistItemSelection(it) },
                        onClearSearch = { viewModel.parsedVideoMeta.value = null; viewModel.urlInput.value = "" },
                        viewModel = viewModel
                    )
                    1 -> LibraryTabContent(
                        history = downloadHistory,
                        onPlayItem = { viewModel.currentPreviewVideo.value = it },
                        onDeleteItem = { viewModel.deleteDownload(it) },
                        onClearAll = { viewModel.clearAllHistory() }
                    )
                    2 -> SettingsTabContent(
                        folderType = folderType,
                        customFolderName = customFolderName,
                        onFolderTypeChange = { viewModel.defaultFolderType.value = it },
                        onCustomFolderNameChange = { viewModel.customFolderName.value = it },
                        resolvedPathPreview = resolvedPathPreview,
                        logs = logs,
                        isGoogleAuthenticated = isGoogleAuthenticated,
                        authenticatedUserEmail = authenticatedUserEmail,
                        onRequestLogin = { viewModel.isAuthenticationDialogShowing.value = true },
                        onLogout = { viewModel.logoutGoogle() }
                    )
                }
            }
        }

        // 1. Google Authentication Dialog Overlay when needed (triggered either on setting or high-res clicks)
        if (isAuthDialogShowing) {
            GoogleAuthSimulatedDialog(
                onDismiss = { viewModel.isAuthenticationDialogShowing.value = false },
                onLoginCompleted = { email ->
                    viewModel.authenticateGoogleUser(email)
                    viewModel.isAuthenticationDialogShowing.value = false
                }
            )
        }

        // 2. Playback / Preview Overlay Dialogue for Saved Streams
        currentPreviewVideo?.let { activeVideo ->
            VideoPlayerView(
                item = activeVideo,
                onDismiss = { viewModel.currentPreviewVideo.value = null }
            )
        }
    }
}

@Composable
fun TubePyBrandingHeader(
    isGoogleAuthenticated: Boolean,
    authenticatedUserEmail: String?,
    onRequestLogin: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121217)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF222228))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Elegant brand title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF2C3B), Color(0xFF6B1119))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TubePy",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2E2E36), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "PyTube 15.0",
                                color = Color(0xFFFF2C3B),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Multiplex Streaming Engine",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            // Google Auth Status Action Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        if (isGoogleAuthenticated) Color(0xFF4CAF50) else Color(0x33FFFFFF),
                        RoundedCornerShape(10.dp)
                    )
                    .background(if (isGoogleAuthenticated) Color(0x114CAF50) else Color(0xFF1E1E24))
                    .clickable { if (isGoogleAuthenticated) onLogout() else onRequestLogin() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isGoogleAuthenticated) Icons.Filled.CheckCircle else Icons.Filled.AccountCircle,
                        contentDescription = "Google Link",
                        tint = if (isGoogleAuthenticated) Color(0xFF4CAF50) else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isGoogleAuthenticated) {
                            authenticatedUserEmail?.take(10)?.plus("...") ?: "Linked"
                        } else {
                            "Google Sign-In"
                        },
                        color = if (isGoogleAuthenticated) Color.White else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TubePyTabBar(
    activeTab: Int,
    onTabSelect: (Int) -> Unit,
    downloadsCount: Int
) {
    ScrollableTabRow(
        selectedTabIndex = activeTab,
        containerColor = Color.Transparent,
        contentColor = Color(0xFFFF2C3B),
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            if (activeTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = Color(0xFFFF2C3B),
                    height = 3.dp
                )
            }
        },
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        val tabItems = listOf(
            TabModel(0, "Downloader", Icons.Filled.Download),
            TabModel(1, "Library ($downloadsCount)", Icons.Filled.FolderOpen),
            TabModel(2, "Console & Paths", Icons.Filled.Terminal)
        )

        tabItems.forEach { tab ->
            val isSelected = activeTab == tab.id
            Tab(
                selected = isSelected,
                onClick = { onTabSelect(tab.id) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) Color(0xFFFF2C3B) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tab.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else Color.Gray
                        )
                    }
                }
            )
        }
    }
}

data class TabModel(
    val id: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloaderTabContent(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onResolve: () -> Unit,
    isLoading: Boolean,
    parsedMeta: YouTubeVideoMeta?,
    progressMap: Map<String, Int>,
    statusMap: Map<String, String>,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    onDownloadSingle: (YouTubeVideoMeta) -> Unit,
    onDownloadPlaylist: () -> Unit,
    onTogglePlaylistItem: (Int) -> Unit,
    onClearSearch: () -> Unit,
    viewModel: DownloaderViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Direct URL Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121217)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF1E1E24))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "STREAM MULTIPLEX URL INPUT",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste YouTube Video or Playlist link...", color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF2C3B),
                            unfocusedBorderColor = Color(0xFF2C2C35),
                            focusedContainerColor = Color(0xFF0C0C0F),
                            unfocusedContainerColor = Color(0xFF0C0C0F)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { onUrlChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons (Resolve Stream)
                    Button(
                        onClick = onResolve,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C3B)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing Python Pipelines...", fontSize = 14.sp)
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze & Lock YouTube Stream", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Helper instant action panel for easy testing of downloader!
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Or tap to test with sample links instantly:",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SuggestionChip(
                            onClick = {
                                onUrlChange("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                                viewModel.parsedVideoMeta.value = null
                                viewModel.parseInputUrl()
                            },
                            label = { Text("Single Video (HD)", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.White, containerColor = Color(0xFF1E1E24))
                        )
                        SuggestionChip(
                            onClick = {
                                onUrlChange("https://www.youtube.com/playlist?list=PL68779D6")
                                viewModel.parsedVideoMeta.value = null
                                viewModel.parseInputUrl()
                            },
                            label = { Text("Album Playlist", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.White, containerColor = Color(0xFF1E1E24))
                        )
                        SuggestionChip(
                            onClick = {
                                onUrlChange("jNQXAC9IVRw")
                                viewModel.parsedVideoMeta.value = null
                                viewModel.parseInputUrl()
                            },
                            label = { Text("Me At The Zoo", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color.White, containerColor = Color(0xFF1E1E24))
                        )
                    }
                }
            }
        }

        // Display Resolved Video / Playlist Segment Detail Block
        parsedMeta?.let { meta ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16161D)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xAAFF2C3B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Title bar
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (meta.isPlaylist) "⚠ YOUTUBE PLAYLIST METADATA" else "✔ RESOLVED STREAM METADATA",
                                color = if (meta.isPlaylist) Color(0xFFFFB300) else Color(0xFFFF2C3B),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Video layout panel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Video Thumbnail using Coil
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 75.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = meta.thumbnailUrl,
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(text = meta.duration, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Meta texts
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meta.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = meta.channel,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Playlist checklist of items if it's a playlist!
                        if (meta.isPlaylist) {
                            Text(
                                text = "Playlist Contents (${meta.playlistItems.size} files queue):",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F0F12), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                meta.playlistItems.forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onTogglePlaylistItem(idx) }
                                        .padding(vertical = 4.dp, horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isSelected,
                                            onCheckedChange = { onTogglePlaylistItem(idx) },
                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF2C3B), uncheckedColor = Color.Gray)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                color = if (item.isSelected) Color.White else Color.DarkGray,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.channel} • ${item.duration}",
                                                color = Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                        val activeState = statusMap[item.id]
                                        if (activeState != null) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (activeState == "Completed") Color(0xFF2E7D32) else Color(0xFFFF2C3B),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = activeState, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Quality selector settings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Target Format Quality:",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val qualityOptions = listOf("1080p", "720p", "480p", "MP3 Audio")
                                qualityOptions.forEach { q ->
                                    val isSelected = selectedQuality == q
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFFF2C3B) else Color(0xFF1E1E24))
                                            .clickable { onQualityChange(q) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = q,
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Trigger DL CTA Button
                        val activeDownloadPercent = progressMap[meta.videoId]
                        val isDownloading = activeDownloadPercent != null && activeDownloadPercent < 100

                        Button(
                            onClick = {
                                if (meta.isPlaylist) {
                                    onDownloadPlaylist()
                                } else {
                                    onDownloadSingle(meta)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C3B)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isDownloading
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = "Download")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (meta.isPlaylist) {
                                    "Launch Bulk Playlist Pipeline"
                                } else {
                                    "Save Stream locally as $selectedQuality"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live Downloading Segments/Queue Items
        if (progressMap.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ACTIVE MULTICAST PIPELINES",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    progressMap.forEach { (vId, progress) ->
                        val status = statusMap[vId] ?: "Queued"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF121217)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E1E24))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (status) {
                                                "Completed" -> Icons.Filled.CheckCircle
                                                "Failed" -> Icons.Filled.Warning
                                                else -> Icons.Filled.Refresh
                                            },
                                            contentDescription = "status",
                                            tint = when (status) {
                                                "Completed" -> Color(0xFF4CAF50)
                                                "Failed" -> Color.Red
                                                else -> Color(0xFFFF2C3B)
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "ID: $vId",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "$status ($progress%)",
                                        color = if (status == "Completed") Color.Green else Color(0xFFFF2C3B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFFFF2C3B),
                                    trackColor = Color(0xFF1F1F26),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryTabContent(
    history: List<DownloadItem>,
    onPlayItem: (DownloadItem) -> Unit,
    onDeleteItem: (DownloadItem) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Folder,
                        contentDescription = "Empty",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Your TubePy Media Library is empty",
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paste a video link and fetch download formats to get started.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAVED MEDIA STORAGE (${history.size} items)",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "clear")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All Database", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(history) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121217)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF222228))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(90.dp, 60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = "thumbnail",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = item.duration, color = Color.White, fontSize = 8.sp)
                                    }
                                }

                                // Specs & title
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.channel,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF2E2E32), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(text = item.quality, color = Color.Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF2E2E32), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(text = item.format, color = Color.White, fontSize = 8.sp)
                                        }
                                        Text(
                                            text = "${item.sizeMb} MB",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Interactive actions (Preview / Play / Trash)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ready to preview offline",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onDeleteItem(item) },
                                        modifier = Modifier.background(Color(0x11FF0000), CircleShape)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }

                                    Button(
                                        onClick = { onPlayItem(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C3B)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PREVIEW PLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    folderType: String,
    customFolderName: String,
    onFolderTypeChange: (String) -> Unit,
    onCustomFolderNameChange: (String) -> Unit,
    resolvedPathPreview: String,
    logs: List<String>,
    isGoogleAuthenticated: Boolean,
    authenticatedUserEmail: String?,
    onRequestLogin: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Saving folder custom component
        item {
            DirectorySelector(
                selectedType = folderType,
                customName = customFolderName,
                onTypeChange = onFolderTypeChange,
                onCustomNameChange = onCustomFolderNameChange,
                resolvedPathPreview = resolvedPathPreview
            )
        }

        // Authenticate Block helper
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121217)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF222228))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ACCOUNT AUTHENTICATION",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When downloading secured streams or restricted songs, YouTube server blocks might trigger. Authenticators use a secure Google Account token bypass framework.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isGoogleAuthenticated) {
                        Surface(
                            color = Color(0xFF1B5E20),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Authentication active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(authenticatedUserEmail ?: "", color = Color.LightGray, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = onLogout,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Unlink", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = onRequestLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C3B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "link")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Link Google Account Securely", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Realtime logs python terminal shell simulation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF2D2D35))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Terminal, contentDescription = "Shell", tint = Color.Green, modifier = Modifier.size(16.dp))
                            Text(
                                text = "PYTHON INTERPRETER SYSTEM SHELL LOGS",
                                color = Color.Green,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(
                            reverseLayout = false,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logs) { logMsg ->
                                Text(
                                    text = logMsg,
                                    color = if (logMsg.contains("[Error]") || logMsg.contains("[WARN]")) Color.Yellow else Color.Green,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog simulating Google Account linking login securely
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAuthSimulatedDialog(
    onDismiss: () -> Unit,
    onLoginCompleted: (String) -> Unit
) {
    var emailInput by remember { mutableStateOf("safeboxtv@gmail.com") }
    var passwordInput by remember { mutableStateOf("••••••••••••") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141419)),
            border = BorderStroke(1.dp, Color(0xFF32323D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Icon(
                    imageVector = Icons.Filled.AccountBox,
                    contentDescription = "Google Link",
                    tint = Color(0xFFFF2C3B),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Google Account Link",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bypass verification blocks & authenticate downloads",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Google Account Email", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2C3B),
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password field
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Google OAuth Password", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF2C3B),
                        unfocusedBorderColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { if (emailInput.isNotEmpty()) onLoginCompleted(emailInput) },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2C3B))
                    ) {
                        Text("Link Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
