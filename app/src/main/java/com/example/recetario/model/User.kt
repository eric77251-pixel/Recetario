package com.example.recetario.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(

    var id: String = "",

    var nombre: String = "",

    var apellido: String = "",

    var correo: String = "",

    @SerialName("foto_perfil")
    var fotoPerfil: String = ""

)