package com.example.recetario.Manager

import com.example.recetario.Funciones.Supabase
import com.example.recetario.Modelos.Receta
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.tasks.await

object RecetaManager {

    private val db = FirebaseFirestore.getInstance()
    private val recetas = db.collection("recetas")

    suspend fun crearReceta(receta: Receta): Receta? {
        return try {
            val docRef = recetas.document()
            receta.id = docRef.id

            docRef.set(receta).await()

            receta
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun obtenerRecetas(): List<Receta> {
        return try {
            recetas.get().await().toObjects(Receta::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun obtenerReceta(id: String): Receta? {
        return try {
            recetas.document(id).get().await()
                .toObject(Receta::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun obtenerRecetasUsuario(usuarioId: String): List<Receta> {
        return try {
            recetas
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()
                .toObjects(Receta::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun actualizarReceta(receta: Receta): Boolean {
        return try {
            recetas.document(receta.id).set(receta).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun eliminarReceta(id: String): Boolean {
        return try {
            recetas.document(id).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun buscarRecetas(texto: String): List<Receta> {
        return try {
            recetas.get().await()
                .toObjects(Receta::class.java)
                .filter {
                    it.nombre.contains(texto, ignoreCase = true)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun subirImagen(
        nombreArchivo: String,
        bytesImagen: ByteArray
    ): String? {
        return try {

            val bucket = Supabase.client.storage.from("image")

            bucket.upload(
                path = nombreArchivo,
                data = bytesImagen
            ) {
                upsert = true
            }

            bucket.publicUrl(nombreArchivo)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}