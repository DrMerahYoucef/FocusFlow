package com.example.data.repository

import com.example.data.db.dao.BlockedAppDao
import com.example.data.db.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

class BlockedAppRepository(private val blockedAppDao: BlockedAppDao) {
    fun getAllBlocked(): Flow<List<BlockedAppEntity>> = blockedAppDao.getAllBlocked()

    suspend fun getNotifBlockedPackages(): List<String> = blockedAppDao.getNotifBlockedPackages()

    suspend fun getLaunchBlockedPackages(): List<String> = blockedAppDao.getLaunchBlockedPackages()

    suspend fun upsert(app: BlockedAppEntity) {
        blockedAppDao.upsert(app)
    }

    suspend fun delete(app: BlockedAppEntity) {
        blockedAppDao.delete(app)
    }

    suspend fun deleteByPackage(pkg: String) {
        blockedAppDao.deleteByPackage(pkg)
    }
}
