package com.example.recetario.data

import com.example.recetario.utils.SupabaseClientProvider
import com.example.recetario.model.Recipe
import com.google.firebase.firestore.FirebaseFirestore
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.tasks.await

object RecipeManager {

    private val db = FirebaseFirestore.getInstance()
    private val recetas = db.collection("recetas")

    suspend fun crearReceta(receta: Recipe): Recipe? {
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

    suspend fun obtenerRecetas(): List<Recipe> {
        return try {
            recetas.get().await().toObjects(Recipe::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun obtenerReceta(id: String): Recipe? {
        return try {
            if (id.isBlank()) return null

            val documento = recetas.document(id).get().await()
            val receta = documento.toObject(Recipe::class.java) ?: return null

            receta.id = documento.id
            receta
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun obtenerRecetasUsuario(usuarioId: String): List<Recipe> {
        return try {
            recetas
                .whereEqualTo("usuarioId", usuarioId)
                .get()
                .await()
                .toObjects(Recipe::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun actualizarReceta(receta: Recipe): Boolean {
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

    suspend fun buscarRecetas(texto: String): List<Recipe> {
        return try {
            recetas.get().await()
                .toObjects(Recipe::class.java)
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

            val bucket = SupabaseClientProvider.client.storage.from("image")

            bucket.upload(
                path = "receta/$nombreArchivo",
                data = bytesImagen
            ) {
                upsert = true
            }

            bucket.publicUrl("receta/$nombreArchivo")

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun eliminarImagen(urlImagen: String): Boolean {
        return try {
            if (urlImagen.isBlank()) return true

            val path = obtenerPathImagenDesdeUrl(urlImagen)

            if (path.isBlank()) return true

            val bucket = SupabaseClientProvider.client.storage.from("image")

            bucket.delete(path)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun obtenerPathImagenDesdeUrl(urlImagen: String): String {
        val marcador = "/storage/v1/object/public/image/"
        val indice = urlImagen.indexOf(marcador)

        if (indice == -1) return ""

        return urlImagen.substring(indice + marcador.length)
    }
}