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

            val idFavorito = crearIdFavorito(savedRecipe.usuarioId, savedRecipe.recetaId)
            val favorito = savedRecipe.copy(id = idFavorito)

            coleccion
                .document(idFavorito)
                .set(favorito)
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
            if (usuarioId.isBlank() || recetaId.isBlank()) {
                return false
            }

            val idFavorito = crearIdFavorito(usuarioId, recetaId)

            coleccion
                .document(idFavorito)
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

            val idFavorito = crearIdFavorito(usuarioId, recetaId)
            val favoritoDirecto = coleccion.document(idFavorito).get().await()

            if (favoritoDirecto.exists()) {
                return true
            }

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
            if (usuarioId.isBlank()) {
                return emptyList()
            }

            val documentos = coleccion
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()

            val favoritosValidos = mutableListOf<SavedRecipe>()
            val recetaIdsVistos = mutableSetOf<String>()

            for (documento in documentos.documents) {
                val favorito = documento.toObject(SavedRecipe::class.java) ?: continue

                if (favorito.recetaId.isBlank()) {
                    documento.reference.delete().await()
                    continue
                }

                val idCorrecto = crearIdFavorito(favorito.usuarioId, favorito.recetaId)

                if (!recetaIdsVistos.add(favorito.recetaId)) {
                    documento.reference.delete().await()
                    continue
                }

                val favoritoNormalizado = favorito.copy(id = idCorrecto)
                favoritosValidos.add(favoritoNormalizado)

                if (documento.id != idCorrecto) {
                    coleccion.document(idCorrecto).set(favoritoNormalizado).await()
                    documento.reference.delete().await()
                }
            }

            favoritosValidos
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun crearIdFavorito(usuarioId: String, recetaId: String): String {
        return "${usuarioId}_${recetaId}"
    }
}
