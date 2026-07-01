package com.example.recetario.Manager

import com.example.recetario.Funciones.Supabase
import com.example.recetario.Modelos.Usuario
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
object UsuarioManager {
    suspend fun crearUsuario(usuario: Usuario): Boolean {
        return try {
            Supabase.client
                .from("usuarios").insert(usuario)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun obtenerUsuario(id: String): Usuario? {

        return try {

            Supabase.client
                .from("usuarios")
                .select(
                    Columns.list(
                        "id",
                        "nombre",
                        "apellido",
                        "correo",
                        "foto_perfil"
                    )
                ) {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Usuario>()

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    suspend fun obtenerUsuarios(): List<Usuario> {

        return try {

            Supabase.client
                .from("usuarios")
                .select()
                .decodeList<Usuario>()

        } catch (e: Exception) {

            emptyList()
        }
    }

    suspend fun actualizarUsuario(usuario: Usuario): Boolean {

        return try {

            Supabase.client
                .from("usuarios")
                .update(usuario) {
                    filter {
                        eq("id", usuario.id)
                    }
                }

            true

        } catch (e: Exception) {

            e.printStackTrace()
            false
        }
    }


    suspend fun eliminarUsuario(id: String): Boolean {

        return try {

            Supabase.client
                .from("usuarios").delete {
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
}