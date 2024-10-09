package com.juanarton.encnotes.core.data.source.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appstate")
data class AppStateEntity (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int?,

    @ColumnInfo(name = "isLoggedIn")
    val boolean: Boolean?,

    @ColumnInfo(name = "isGuest")
    val isGuest: Boolean?,

    @ColumnInfo(name = "gUID")
    val gUID: String?,

    @ColumnInfo(name = "refreshKey")
    val refreshKey: String?,

    @ColumnInfo(name = "accessKey")
    val accessKey: String?
)