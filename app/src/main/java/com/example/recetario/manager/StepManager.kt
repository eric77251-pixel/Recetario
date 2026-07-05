package com.example.recetario.manager

import com.example.recetario.model.Step
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object StepManager {

    private val db = FirebaseFirestore.getInstance()
    private val coleccion = db.collection("pasos")

    suspend fun crearPaso(paso: Step): Boolean {

        return try {

            val documento = coleccion.document()

            paso.id = documento.id

            documento
                .set(paso)
                .await()

            true

        } catch (e: Exception) {

            e.printStackTrace()
            throw Exception(e.message)
        }
    }

    suspend fun obtenerPasos(recetaId: String): List<Step> {

        return try {

            coleccion
                .whereEqualTo("recetaId", recetaId)
                .get()
                .await()
                .toObjects(Step::class.java)
                .sortedBy { it.numero } // 👈 ordenamos en Kotlin en vez de Firestore

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun eliminarPasos(recetaId: String): Boolean {

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