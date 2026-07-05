package com.example.recetario.model

import kotlinx.serialization.Serializable

@Serializable
data class Step(

    var id: String = "",

    var recetaId: String = "",

    var numero: Int = 0,

    var descripcion: String = ""

)