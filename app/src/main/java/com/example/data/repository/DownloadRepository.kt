package com.example.data.repository

import com.example.data.database.DownloadDao
import com.example.data.models.DownloadItem
import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val downloadDao: DownloadDao) {
    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    suspend fun insertDownload(item: DownloadItem): Long {
        return downloadDao.insertDownload(item)
    }

    suspend fun updateDownload(item: DownloadItem) {
        downloadDao.updateDownload(item)
    }

    suspend fun deleteDownload(item: DownloadItem) {
        downloadDao.deleteDownload(item)
    }

    suspend fun deleteDownloadById(id: Int) {
        downloadDao.deleteDownloadById(id)
    }

    suspend fun getDownloadByVideoId(videoId: String): DownloadItem? {
        return downloadDao.getDownloadByVideoId(videoId)
    }

    suspend fun clearAll() {
        downloadDao.clearAll()
    }
}
