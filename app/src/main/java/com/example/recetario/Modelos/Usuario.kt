package com.example.recetario.Modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(

    var id: String = "",

    var nombre: String = "",

    var apellido: String = "",

    var correo: String = "",

    @SerialName("foto_perfil")
    var fotoPerfil: String = ""

)