package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_items")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: String,
    val title: String,
    val channel: String,
    val duration: String,
    val thumbnailUrl: String,
    val quality: String,
    val format: String,
    val localPath: String,
    val downloadPercent: Int = 0,
    val status: String = "Completed", // "Pending", "Downloading", "Completed", "Failed"
    val isPlaylist: Boolean = false,
    val playlistTitle: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sizeMb: Double = 0.0
)
