package com.example.recetario.data

import com.example.recetario.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.recetario.utils.SupabaseClientProvider
import io.github.jan.supabase.storage.storage

object UserManager {

    private val db = FirebaseFirestore.getInstance()
    private val usuarios = db.collection("usuarios")

    suspend fun crearUsuario(usuario: User): Boolean {
        return try {
            usuarios
                .document(usuario.id)
                .set(usuario)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun obtenerUsuario(id: String): User? {
        return try {
            val documento = usuarios
                .document(id)
                .get()
                .await()
            documento.toObject(User::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun actualizarUsuario(usuario: User): Boolean {
        return try {
            usuarios
                .document(usuario.id)
                .set(usuario)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun subirFotoPerfil(
        nombreArchivo: String,
        bytesImagen: ByteArray
    ): String? {
        return try {
            val bucket = SupabaseClientProvider.client.storage.from("image")
            // Se agrega el prefijo de la carpeta 'perfil/'
            val fullPath = "perfil/$nombreArchivo"

            bucket.upload(
                path = fullPath,
                data = bytesImagen
            ) {
                upsert = true
            }

            bucket.publicUrl(fullPath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
