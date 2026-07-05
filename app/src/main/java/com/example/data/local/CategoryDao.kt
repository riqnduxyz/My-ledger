package com.example.data.local

import androidx.room.*
import com.example.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<ExpenseCategory>>

    @Query("SELECT * FROM categories")
    suspend fun getCategoriesList(): List<ExpenseCategory>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): ExpenseCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ExpenseCategory>)

    @Delete
    suspend fun deleteCategory(category: ExpenseCategory)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()
}
