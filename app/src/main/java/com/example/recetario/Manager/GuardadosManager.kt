package com.example.recetario.Manager

import com.example.recetario.Modelos.Guardado
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object GuardadosManager {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("favoritos")

    suspend fun agregarFavorito(guardado: Guardado): Boolean {

        return try {

            coleccion
                .document(guardado.id)
                .set(guardado)
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

    suspend fun obtenerFavoritos(usuarioId: String): List<Guardado> {

        return try {

            coleccion
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()
                .toObjects(Guardado::class.java)

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }
}