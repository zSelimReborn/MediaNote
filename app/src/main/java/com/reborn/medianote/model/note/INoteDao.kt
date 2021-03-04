package com.reborn.medianote.model.note

import androidx.room.*
import androidx.lifecycle.LiveData

@Dao
interface INoteDao {
    @Insert
    fun insert(note: Note): Long

    @Delete
    fun delete(note: Note)

    @Update
    fun update(note: Note)

    @Query("SELECT * FROM notes")
    fun getAllNotes() : LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getById(id: Int) : LiveData<Note>

    @Query("DELETE FROM notes")
    fun deleteAllNotes() : Int
}