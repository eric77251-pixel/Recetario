package com.example.recetario.utils

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {

    val client = createSupabaseClient(
        supabaseUrl = "https://mxxncioxgrfghiyiibfg.supabase.co",
        supabaseKey = "sb_publishable_O9wd7rlV2ZaUni4oL73S5Q_87MUnVtM"
    ) {
        install(Storage)
    }
}