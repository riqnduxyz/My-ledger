package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.LedgerNoteDao
import com.example.data.model.ExpenseCategory
import com.example.data.model.LedgerNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class LedgerEntryWithCategory(
    val note: LedgerNote,
    val category: ExpenseCategory?
)

class LedgerRepository(
    private val categoryDao: CategoryDao,
    private val ledgerNoteDao: LedgerNoteDao
) {
    val allCategories: Flow<List<ExpenseCategory>> = categoryDao.getAllCategories()
    val allNotes: Flow<List<LedgerNote>> = ledgerNoteDao.getAllNotes()

    // Reactive flow combining notes and categories
    val ledgerEntries: Flow<List<LedgerEntryWithCategory>> = combine(
        ledgerNoteDao.getAllNotes(),
        categoryDao.getAllCategories()
    ) { notes, categories ->
        val categoryMap = categories.associateBy { it.id }
        notes.map { note ->
            LedgerEntryWithCategory(note, categoryMap[note.categoryId])
        }
    }

    suspend fun getCategoriesList(): List<ExpenseCategory> = categoryDao.getCategoriesList()
    suspend fun getNotesList(): List<LedgerNote> = ledgerNoteDao.getNotesList()

    suspend fun insertCategory(category: ExpenseCategory) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: ExpenseCategory) {
        categoryDao.deleteCategory(category)
    }

    suspend fun insertNote(note: LedgerNote) {
        ledgerNoteDao.insertNote(note)
    }

    suspend fun deleteNote(note: LedgerNote) {
        ledgerNoteDao.deleteNote(note)
    }

    suspend fun clearAndImport(categories: List<ExpenseCategory>, notes: List<LedgerNote>) {
        categoryDao.clearAllCategories()
        ledgerNoteDao.clearAllNotes()
        categoryDao.insertCategories(categories)
        ledgerNoteDao.insertNotes(notes)
    }

    suspend fun ensureDefaultCategories() {
        val existing = categoryDao.getCategoriesList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                ExpenseCategory(name = "Food & Dining", colorHex = "#FF7043", iconName = "Restaurant"),
                ExpenseCategory(name = "Shopping", colorHex = "#EC407A", iconName = "ShoppingBag"),
                ExpenseCategory(name = "Transport", colorHex = "#42A5F5", iconName = "DirectionsCar"),
                ExpenseCategory(name = "Utilities", colorHex = "#FFCA28", iconName = "Lightbulb"),
                ExpenseCategory(name = "Entertainment", colorHex = "#AB47BC", iconName = "Movie"),
                ExpenseCategory(name = "Salary & Income", colorHex = "#66BB6A", iconName = "Payments"),
                ExpenseCategory(name = "Other", colorHex = "#78909C", iconName = "MoreHoriz")
            )
            categoryDao.insertCategories(defaults)
        }
    }
}
