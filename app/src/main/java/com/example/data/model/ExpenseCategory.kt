package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "categories")
@JsonClass(generateAdapter = true)
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String, // Hex string like "#FF7043"
    val iconName: String  // Standard icon descriptor
)
