package com.example.data.local

import androidx.room.*
import com.example.data.model.LedgerNote
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerNoteDao {
    @Query("SELECT * FROM ledger_notes ORDER BY date DESC")
    fun getAllNotes(): Flow<List<LedgerNote>>

    @Query("SELECT * FROM ledger_notes")
    suspend fun getNotesList(): List<LedgerNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: LedgerNote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<LedgerNote>)

    @Delete
    suspend fun deleteNote(note: LedgerNote)

    @Query("DELETE FROM ledger_notes")
    suspend fun clearAllNotes()
}
