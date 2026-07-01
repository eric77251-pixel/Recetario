package com.example.recetario.Manager

import com.example.recetario.Funciones.Supabase
import com.example.recetario.Modelos.Ingredientes
import io.github.jan.supabase.postgrest.from

object IngredientesManager {

    suspend fun crearIngrediente(ingrediente: Ingredientes): Boolean {
        return try {
            Supabase.client.from("ingredientes").insert(ingrediente)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun obtenerIngredientes(recetaId: String): List<Ingredientes> {
        return try {
            Supabase.client.from("ingredientes").select {
                    filter {
                        eq("receta_id", recetaId)
                    }
                }
                .decodeList<Ingredientes>()

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun eliminarIngredientes(recetaId: String): Boolean {
        return try {
            Supabase.client.from("ingredientes").delete {
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