package com.reborn.medianote.model.note

import android.app.Application
import androidx.lifecycle.LiveData
import com.reborn.medianote.model.NoteDatabase
import com.reborn.medianote.model.utils.subscribeOnBackground

class NoteRepository (application: Application) {
    private var noteDao: INoteDao

    private val database = NoteDatabase.getInstance(application)

    init {
        noteDao = database.getNoteDao()
    }

    fun insert(note: Note) {
        subscribeOnBackground {
            noteDao.insert(note)
        }
    }

    fun delete(note: Note) {
        subscribeOnBackground {
            noteDao.delete(note)
        }
    }

    fun update(note: Note) {
        subscribeOnBackground {
            noteDao.update(note)
        }
    }

    fun getAllNotes() : LiveData<List<Note>> {
        return noteDao.getAllNotes()
    }

    fun getById(id: Int) : LiveData<Note> {
        return noteDao.getById(id)
    }

    fun deleteAllNotes(afterDelete: (Int) -> Unit) {
        subscribeOnBackground {
            val notesDeleted = noteDao.deleteAllNotes()
            afterDelete(notesDeleted)
        }
    }
}