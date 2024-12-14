package com.juanarton.encnotes.core.utils

import android.util.Log
import com.juanarton.encnotes.core.data.domain.model.Notes

object NoteSync {
    fun syncNotes(localNotes: List<Notes>, remoteNotes: List<Notes>): SyncNotes {
        val localMap = localNotes.associateBy { it.id }
        val remoteMap = remoteNotes.associateBy { it.id }

        // List of data to update to the server (local is newer than remote)
        val toUpdateToServer = localNotes.filter { local ->
            val remote = remoteMap[local.id]
            remote != null && local.lastModified > remote.lastModified
        }.toMutableList()

        // List of data to update to the local database (remote is newer than local)
        val toUpdateToLocal = remoteNotes.filter { remote ->
            val local = localMap[remote.id]
            local != null && remote.lastModified > local.lastModified
        }.toMutableList()

        // List of data that exists in local but not in remote
        val toAddToServer = localNotes.filter { it.id !in remoteMap.keys }.toMutableList()

        // List of data that exists in remote but not in local
        val toAddToLocal = remoteNotes.filter { it.id !in localMap.keys }.toMutableList()

        // List of data that is marked as deleted in local but not in remote
        val toDeleteInServer = localNotes.filter { local ->
            local.isDelete && remoteMap[local.id]?.isDelete == false
        }.toMutableList()

        // List of data that is marked as deleted in remote but not in local
        val toDeleteInLocal = remoteNotes.filter { remote ->
            remote.isDelete && localMap[remote.id]?.isDelete == false
        }.toMutableList()

        return SyncNotes(
            toUpdateToServer = toUpdateToServer,
            toUpdateToLocal = toUpdateToLocal,
            toAddToServer = toAddToServer,
            toAddToLocal = toAddToLocal,
            toDeleteInServer = toDeleteInServer,
            toDeleteInLocal = toDeleteInLocal
        )
    }
}

data class SyncNotes(
    val toUpdateToServer: MutableList<Notes>,
    val toUpdateToLocal: MutableList<Notes>,
    val toAddToServer: MutableList<Notes>,
    val toAddToLocal: MutableList<Notes>,
    val toDeleteInServer: MutableList<Notes>,
    val toDeleteInLocal: MutableList<Notes>
)
