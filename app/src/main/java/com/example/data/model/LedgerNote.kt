package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "ledger_notes")
@JsonClass(generateAdapter = true)
data class LedgerNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String,          // "EXPENSE" or "INCOME"
    val categoryId: Int,       // Foreign key or simple association to ExpenseCategory
    val date: Long,            // Millisecond timestamp
    val description: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
