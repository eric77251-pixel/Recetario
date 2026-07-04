package com.example.recetario.Manager

import com.example.recetario.Modelos.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.recetario.Funciones.Supabase
import io.github.jan.supabase.storage.storage
object UserManager {

    private val db = FirebaseFirestore.getInstance()
    private val usuarios = db.collection("usuarios")

    suspend fun crearUsuario(usuario: Usuario): Boolean {

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

    suspend fun obtenerUsuario(id: String): Usuario? {

        return try {

            val documento = usuarios
                .document(id)
                .get()
                .await()

            documento.toObject(Usuario::class.java)

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }


    suspend fun actualizarUsuario(usuario: Usuario): Boolean {

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