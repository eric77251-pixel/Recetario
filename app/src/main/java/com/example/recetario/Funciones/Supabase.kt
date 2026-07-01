package com.example.recetario.Funciones


import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object Supabase {

    val client = createSupabaseClient(
        supabaseUrl = "https://mxxncioxgrfghiyiibfg.supabase.co",
        supabaseKey = "sb_publishable_O9wd7rlV2ZaUni4oL73S5Q_87MUnVtM"
    ) { install(Postgrest) }

}