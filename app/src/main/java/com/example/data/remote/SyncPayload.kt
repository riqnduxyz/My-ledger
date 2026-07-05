package com.example.data.remote

import com.example.data.model.ExpenseCategory
import com.example.data.model.LedgerNote
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val version: Int = 1,
    val categories: List<ExpenseCategory>,
    val notes: List<LedgerNote>
)
