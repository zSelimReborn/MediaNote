package com.reborn.medianote.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.reborn.medianote.model.note.Note
import com.reborn.medianote.model.note.INoteDao

@Database(entities = [Note::class], version = 1)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun getNoteDao(): INoteDao

    companion object {
        private var instance: NoteDatabase? = null

        @Synchronized
        fun getInstance(ctx: Context): NoteDatabase {
            if(instance == null)
                instance = Room.databaseBuilder(ctx.applicationContext, NoteDatabase::class.java,
                    "note_database")
                    .fallbackToDestructiveMigration()
                    .build()

            return instance!!
        }
    }
}