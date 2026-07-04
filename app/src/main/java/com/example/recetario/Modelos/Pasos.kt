package com.example.recetario.Modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pasos(

    var id: String = "",

    var recetaId: String = "",

    var numero: Int = 0,

    var descripcion: String = ""

)