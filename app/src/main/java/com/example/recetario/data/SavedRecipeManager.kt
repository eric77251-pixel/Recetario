package com.example.recetario.data

import com.example.recetario.model.SavedRecipe
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object SavedRecipeManager {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("favoritos")

    suspend fun agregarFavorito(savedRecipe: SavedRecipe): Boolean {

        return try {

            coleccion
                .document(savedRecipe.id)
                .set(savedRecipe)
                .await()

            true

        } catch (e: Exception) {

            e.printStackTrace()
            false
        }
    }

    suspend fun eliminarFavorito(
        usuarioId: String,
        recetaId: String
    ): Boolean {

        return try {

            val documentos = coleccion
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("recetaId", recetaId)
                .get()
                .await()

            for (documento in documentos.documents) {
                documento.reference.delete().await()
            }

            true

        } catch (e: Exception) {

            e.printStackTrace()
            false
        }
    }

    suspend fun esFavorito(
        usuarioId: String,
        recetaId: String
    ): Boolean {

        return try {

            val resultado = coleccion
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("recetaId", recetaId)
                .get()
                .await()

            !resultado.isEmpty

        } catch (e: Exception) {

            e.printStackTrace()
            false
        }
    }

    suspend fun obtenerFavoritos(usuarioId: String): List<SavedRecipe> {

        return try {

            coleccion
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()
                .toObjects(SavedRecipe::class.java)

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }
}