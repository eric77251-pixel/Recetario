package com.example.recetario.Manager

import com.example.recetario.Funciones.Supabase
import com.example.recetario.Modelos.Pasos
import io.github.jan.supabase.postgrest.from

object PasosManager {

    suspend fun crearPaso(paso: Pasos): Boolean {
        return try {
            Supabase.client.from("pasos").insert(paso)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun obtenerPasos(recetaId: String): List<Pasos> {
        return try {
            Supabase.client.from("pasos").select {
                    filter {
                        eq("receta_id", recetaId)
                    }
                }
                .decodeList<Pasos>()

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun eliminarPasos(recetaId: String): Boolean {
        return try {
            Supabase.client.from("pasos").delete {
                    filter {
                        eq("receta_id", recetaId)
                    }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}