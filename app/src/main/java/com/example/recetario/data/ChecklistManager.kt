package com.example.recetario.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object ChecklistManager {
    private const val PREFS_NAME = "RecipeChecklistPrefs"

    private fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setIngredientChecked(context: Context, recipeId: String, ingredientId: String, isChecked: Boolean) {
        val key = "${getUserId()}_${recipeId}_$ingredientId"
        getPrefs(context).edit().putBoolean(key, isChecked).apply()
    }

    fun isIngredientChecked(context: Context, recipeId: String, ingredientId: String): Boolean {
        val key = "${getUserId()}_${recipeId}_$ingredientId"
        return getPrefs(context).getBoolean(key, false)
    }

    fun clearChecklist(context: Context, recipeId: String, ingredientIds: List<String>) {
        val editor = getPrefs(context).edit()
        val userId = getUserId()
        ingredientIds.forEach { id ->
            editor.remove("${userId}_${recipeId}_$id")
        }
        editor.apply()
    }
}