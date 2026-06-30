package com.example.recetario.Funciones


import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object Supabase{

    val client = createSupabaseClient(
        supabaseUrl = "https://TU_PROYECTO.supabase.co",
        supabaseKey = "TU_API_KEY"
    ) {
        install(Auth)
        install(Postgrest)
    }

}