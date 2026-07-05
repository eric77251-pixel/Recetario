package com.example.recetario.data

import com.example.recetario.model.Ingredient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object IngredientManager {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("ingredientes")

    suspend fun crearIngrediente(ingrediente: Ingredient): Boolean {

        return try {

            val documento = coleccion.document()

            ingrediente.id = documento.id

            documento
                .set(ingrediente)
                .await()

            true

        } catch (e: Exception) {

            e.printStackTrace()
            throw Exception(e.message)
        }
    }

    suspend fun obtenerIngredientes(recetaId: String): List<Ingredient> {

        return try {

            coleccion
                .whereEqualTo("recetaId", recetaId)
                .get()
                .await()
                .toObjects(Ingredient::class.java)

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun eliminarIngredientes(recetaId: String): Boolean {

        return try {

            val documentos = coleccion
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
}