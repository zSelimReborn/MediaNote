package com.reborn.medianote.model.note

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NoteType (val type: String) {
    NORMAL("normal"),
    IMAGE("image"),
    AUDIO("audio")
}

@Entity(tableName = "notes")
data class Note (
    val title: String,
    val content: String,
    val type: String,
    val mediaUrl: String,
    @PrimaryKey val id: Int? = null
)