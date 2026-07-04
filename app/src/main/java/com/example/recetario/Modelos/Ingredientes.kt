package com.example.recetario.Modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Ingredientes(

    var id: String = "",

    var recetaId: String = "",

    var nombre: String = "",

    var cantidad: String = ""

)