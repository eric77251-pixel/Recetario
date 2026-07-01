package com.example.recetario.Modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ingredientes(

    var id: String = "",

    @SerialName("receta_id")
    var recetaId: String = "",

    var nombre: String = "",

    var cantidad: String = ""

)