package com.example.recetario.Manager

import com.example.recetario.Funciones.Supabase
import com.example.recetario.Modelos.Receta
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.io.ByteArrayOutputStream

object RecetaManager {


    suspend fun crearReceta(receta: Receta): Receta? {

        return try {
            Supabase.client.from("recetas").insert(receta){
                select()
            }.decodeSingle<Receta>()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun obtenerRecetas(): List<Receta> {

        return try {
            Supabase.client.from("recetas").select().decodeList<Receta>()

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun obtenerReceta(id: String): Receta? {

        return try {

            Supabase.client.from("recetas").select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Receta>()

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    suspend fun obtenerRecetasUsuario(usuarioId: String): List<Receta> {
        return try {
            Supabase.client.from("recetas").select {
                    filter {
                        eq("usuario_id", usuarioId)
                    }
                }
                .decodeList<Receta>()

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }
    suspend fun actualizarReceta(receta: Receta): Boolean {

        return try {

            Supabase.client.from("recetas").update(receta) {
                    filter {
                        eq("id", receta.id)
                    }
                }

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun subirImagen(nombreArchivo: String, bytesImagen: ByteArray): String? {
        return try {
            val bucket = Supabase.client.storage.from("image")

            // 1. Subir los bytes del archivo
            bucket.upload(path = nombreArchivo, data = bytesImagen) {
                upsert = true // Si el archivo ya existe, lo sobrescribe
            }

            // 2. Obtener y retornar la URL pública
            val urlPublica = bucket.publicUrl(path = nombreArchivo)
            urlPublica
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun eliminarReceta(id: String): Boolean {

        return try {

            Supabase.client.from("recetas").delete {
                    filter {
                        eq("id", id)
                    }
                }

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun buscarRecetas(texto: String): List<Receta> {
        return try { Supabase.client.from("recetas").select {
                    filter {
                        ilike(
                            "nombre",
                            "%$texto%"
                        )
                    }
                }
                .decodeList<Receta>()
        } catch (e: Exception) {

            emptyList()
        }
    }
}