package com.reborn.medianote.frontend

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.reborn.medianote.model.note.Note
import com.reborn.medianote.model.note.NoteRepository

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = NoteRepository(app)

    fun getAllNotes(): LiveData<List<Note>> {
        return repository.getAllNotes()
    }

    fun insert(note: Note) {
        repository.insert(note)
    }

    fun update(note: Note) {
        repository.update(note)
    }

    fun delete(note: Note) {
        repository.delete(note)
    }

    fun getNoteById(id: Int): LiveData<Note> {
        return repository.getById(id)
    }

    fun deleteAllNotes(afterDelete: (Int) -> Unit) {
        return repository.deleteAllNotes(afterDelete)
    }
}