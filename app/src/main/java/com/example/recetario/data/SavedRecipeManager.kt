package com.example.recetario.data

import com.example.recetario.model.SavedRecipe
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object SavedRecipeManager {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("favoritos")

    suspend fun agregarFavorito(savedRecipe: SavedRecipe): Boolean {
        return try {
            if (savedRecipe.usuarioId.isBlank() || savedRecipe.recetaId.isBlank()) {
                return false
            }

            val idDirecto = "${savedRecipe.usuarioId}_${savedRecipe.recetaId}"
            val favoritoConId = savedRecipe.copy(id = idDirecto)

            coleccion
                .document(idDirecto)
                .set(favoritoConId)
                .await()

            eliminarDuplicados(savedRecipe.usuarioId, savedRecipe.recetaId, idDirecto)

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
            if (usuarioId.isBlank() || recetaId.isBlank()) {
                return false
            }

            val idDirecto = "${usuarioId}_${recetaId}"

            coleccion
                .document(idDirecto)
                .delete()
                .await()

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
            if (usuarioId.isBlank() || recetaId.isBlank()) {
                return false
            }

            val idDirecto = "${usuarioId}_${recetaId}"
            val favoritoDirecto = coleccion.document(idDirecto).get().await()

            if (favoritoDirecto.exists()) {
                return true
            }

            val resultado = coleccion
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("recetaId", recetaId)
                .limit(1)
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
            if (usuarioId.isBlank()) {
                return emptyList()
            }

            val documentos = coleccion
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()
                .documents

            val favoritos = mutableListOf<SavedRecipe>()
            val recetaIdsVistos = mutableSetOf<String>()

            for (documento in documentos) {
                val favorito = documento.toObject(SavedRecipe::class.java) ?: continue

                if (favorito.recetaId.isBlank()) {
                    documento.reference.delete().await()
                    continue
                }

                if (recetaIdsVistos.add(favorito.recetaId)) {
                    favoritos.add(
                        favorito.copy(
                            id = if (favorito.id.isBlank()) documento.id else favorito.id
                        )
                    )
                } else {
                    documento.reference.delete().await()
                }
            }

            favoritos
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun eliminarDuplicados(
        usuarioId: String,
        recetaId: String,
        idPermitido: String
    ) {
        val documentos = coleccion
            .whereEqualTo("usuarioId", usuarioId)
            .whereEqualTo("recetaId", recetaId)
            .get()
            .await()

        for (documento in documentos.documents) {
            if (documento.id != idPermitido) {
                documento.reference.delete().await()
            }
        }
    }
}
