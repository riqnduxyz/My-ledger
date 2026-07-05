package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LedgerDatabase
import com.example.data.model.ExpenseCategory
import com.example.data.model.LedgerNote
import com.example.data.remote.SyncManager
import com.example.data.remote.SyncPayload
import com.example.data.repository.LedgerEntryWithCategory
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LedgerStats(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = LedgerDatabase.getDatabase(application)
    private val repository = LedgerRepository(database.categoryDao(), database.ledgerNoteDao())
    private val syncManager = SyncManager()

    private val sharedPrefs = application.getSharedPreferences("ledger_prefs", Context.MODE_PRIVATE)

    // Sync State
    val syncCode = MutableStateFlow<String?>(sharedPrefs.getString("sync_code", null))
    val lastSyncTime = MutableStateFlow<Long>(sharedPrefs.getLong("last_sync_time", 0L))
    val isSyncing = MutableStateFlow(false)
    val syncError = MutableStateFlow<String?>(null)

    // Filters
    val selectedCategoryFilter = MutableStateFlow<Int?>(null) // null = All
    val selectedTypeFilter = MutableStateFlow<String?>(null)      // null = All, "EXPENSE", "INCOME"

    // Raw Flows
    val categories: StateFlow<List<ExpenseCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<LedgerEntryWithCategory>> = repository.ledgerEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered entries flow
    val filteredEntries: StateFlow<List<LedgerEntryWithCategory>> = combine(
        repository.ledgerEntries,
        selectedCategoryFilter,
        selectedTypeFilter
    ) { entries, categoryId, type ->
        entries.filter { entry ->
            val matchCategory = categoryId == null || entry.note.categoryId == categoryId
            val matchType = type == null || entry.note.type == type
            matchCategory && matchType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations Flow (global stats independent of filters)
    val globalStats: StateFlow<LedgerStats> = repository.ledgerEntries.map { entries ->
        var income = 0.0
        var expenses = 0.0
        entries.forEach { entry ->
            if (entry.note.type == "INCOME") {
                income += entry.note.amount
            } else {
                expenses += entry.note.amount
            }
        }
        LedgerStats(income, expenses, income - expenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerStats())

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    // CRUD - Transaction Notes
    fun addTransaction(title: String, amount: Double, type: String, categoryId: Int, date: Long, description: String) {
        viewModelScope.launch {
            val note = LedgerNote(
                title = title,
                amount = amount,
                type = type,
                categoryId = categoryId,
                date = date,
                description = description,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertNote(note)
            // Auto-push if sync is set up
            autoPushIfNeeded()
        }
    }

    fun editTransaction(id: Int, title: String, amount: Double, type: String, categoryId: Int, date: Long, description: String) {
        viewModelScope.launch {
            val note = LedgerNote(
                id = id,
                title = title,
                amount = amount,
                type = type,
                categoryId = categoryId,
                date = date,
                description = description,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertNote(note)
            autoPushIfNeeded()
        }
    }

    fun deleteTransaction(note: LedgerNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            autoPushIfNeeded()
        }
    }

    // CRUD - Categories
    fun addCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val category = ExpenseCategory(
                name = name,
                colorHex = colorHex,
                iconName = iconName
            )
            repository.insertCategory(category)
            autoPushIfNeeded()
        }
    }

    fun deleteCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            autoPushIfNeeded()
        }
    }

    // Filters Actions
    fun setCategoryFilter(categoryId: Int?) {
        selectedCategoryFilter.value = categoryId
    }

    fun setTypeFilter(type: String?) {
        selectedTypeFilter.value = type
    }

    fun clearFilters() {
        selectedCategoryFilter.value = null
        selectedTypeFilter.value = null
    }

    // SYNC OPERATIONS

    /**
     * Set up a brand new cloud sync slot
     */
    fun setupNewSyncCode(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            try {
                val code = syncManager.generateSyncCode()
                saveSyncCode(code)
                // Push local state to newly created cloud slot
                val success = pushToCloudInternal(code)
                onComplete(success)
            } catch (e: Exception) {
                syncError.value = e.message ?: "Failed to set up sync"
                onComplete(false)
            } finally {
                isSyncing.value = false
            }
        }
    }

    /**
     * Link and pull from an existing sync code
     */
    fun linkAndPullSyncCode(code: String, onComplete: (Boolean) -> Unit = {}) {
        val trimmedCode = code.trim()
        if (trimmedCode.isEmpty()) return
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            try {
                val payload = syncManager.pullData(trimmedCode)
                if (payload != null) {
                    // Successfully fetched remote database state, apply locally
                    repository.clearAndImport(payload.categories, payload.notes)
                    saveSyncCode(trimmedCode)
                    updateLastSyncTime()
                    onComplete(true)
                } else {
                    syncError.value = "Invalid sync code or empty cloud ledger."
                    onComplete(false)
                }
            } catch (e: Exception) {
                syncError.value = e.message ?: "Sync connection failed"
                onComplete(false)
            } finally {
                isSyncing.value = false
            }
        }
    }

    /**
     * Manual push to cloud
     */
    fun pushToCloud(onComplete: (Boolean) -> Unit = {}) {
        val code = syncCode.value ?: return
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            val success = pushToCloudInternal(code)
            isSyncing.value = false
            onComplete(success)
        }
    }

    /**
     * Manual pull from cloud
     */
    fun pullFromCloud(onComplete: (Boolean) -> Unit = {}) {
        val code = syncCode.value ?: return
        viewModelScope.launch {
            isSyncing.value = true
            syncError.value = null
            try {
                val payload = syncManager.pullData(code)
                if (payload != null) {
                    repository.clearAndImport(payload.categories, payload.notes)
                    updateLastSyncTime()
                    onComplete(true)
                } else {
                    syncError.value = "Failed to retrieve ledger data."
                    onComplete(false)
                }
            } catch (e: Exception) {
                syncError.value = e.message ?: "Sync pull failed"
                onComplete(false)
            } finally {
                isSyncing.value = false
            }
        }
    }

    /**
     * Disconnect/Logout from current sync slot
     */
    fun disconnectSync() {
        syncCode.value = null
        lastSyncTime.value = 0L
        sharedPrefs.edit()
            .remove("sync_code")
            .remove("last_sync_time")
            .apply()
    }

    private suspend fun pushToCloudInternal(code: String): Boolean {
        return try {
            val localCategories = repository.getCategoriesList()
            val localNotes = repository.getNotesList()
            val payload = SyncPayload(categories = localCategories, notes = localNotes)
            val success = syncManager.pushData(code, payload)
            if (success) {
                updateLastSyncTime()
                true
            } else {
                syncError.value = "Could not push ledger to server."
                false
            }
        } catch (e: Exception) {
            syncError.value = e.message ?: "Sync push failed"
            false
        }
    }

    private fun autoPushIfNeeded() {
        val code = syncCode.value
        if (code != null) {
            viewModelScope.launch {
                pushToCloudInternal(code)
            }
        }
    }

    private fun saveSyncCode(code: String) {
        syncCode.value = code
        sharedPrefs.edit().putString("sync_code", code).apply()
    }

    private fun updateLastSyncTime() {
        val time = System.currentTimeMillis()
        lastSyncTime.value = time
        sharedPrefs.edit().putLong("last_sync_time", time).apply()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LedgerViewModel::class.java)) {
                return LedgerViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
