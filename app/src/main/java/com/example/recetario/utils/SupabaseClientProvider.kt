package com.example.recetario.utils

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {

    val client = createSupabaseClient(
        supabaseUrl = "TU_SUPABASE_URL",
        supabaseKey = "TU_SUPABASE_KEY"
    ) {
        install(Storage)
    }
}