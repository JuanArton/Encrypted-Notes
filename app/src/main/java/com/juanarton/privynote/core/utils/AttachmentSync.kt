package com.juanarton.privynote.core.utils

import com.juanarton.privynote.core.data.domain.model.Attachment

object AttachmentSync {
    fun syncAttachment(localAttachment: List<Attachment>, remoteAttachment: List<Attachment>): SyncAttachment {
        val localMap = localAttachment.associateBy { it.id }
        val remoteMap = remoteAttachment.associateBy { it.id }

        // List of data to update to the server (local is newer than remote)
        val toUpdateToServer = localAttachment.filter { local ->
            val remote = remoteMap[local.id]
            remote != null && local.lastModified!! > remote.lastModified!!
        }.toMutableList()

        // List of data to update to the local database (remote is newer than local)
        val toUpdateToLocal = remoteAttachment.filter { remote ->
            val local = localMap[remote.id]
            local != null && remote.lastModified!! > local.lastModified!!
        }.toMutableList()

        // List of data that exists in local but not in remote
        val toAddToServer = localAttachment.filter { it.id !in remoteMap.keys }.toMutableList()

        // List of data that exists in remote but not in local
        val toAddToLocal = remoteAttachment.filter { it.id !in localMap.keys }.toMutableList()

        // List of data that is marked as deleted in local but not in remote
        val toDeleteInServer = localAttachment.filter { local ->
            local.isDelete!! && remoteMap[local.id]?.isDelete == false
        }.toMutableList()

        // List of data that is marked as deleted in remote but not in local
        val toDeleteInLocal = remoteAttachment.filter { remote ->
            remote.isDelete!! && localMap[remote.id]?.isDelete == false
        }.toMutableList()

        return SyncAttachment(
            toUpdateToServer = toUpdateToServer,
            toUpdateToLocal = toUpdateToLocal,
            toAddToServer = toAddToServer,
            toAddToLocal = toAddToLocal,
            toDeleteInServer = toDeleteInServer,
            toDeleteInLocal = toDeleteInLocal
        )
    }
}

data class SyncAttachment(
    val toUpdateToServer: MutableList<Attachment>,
    val toUpdateToLocal: MutableList<Attachment>,
    val toAddToServer: MutableList<Attachment>,
    val toAddToLocal: MutableList<Attachment>,
    val toDeleteInServer: MutableList<Attachment>,
    val toDeleteInLocal: MutableList<Attachment>
)