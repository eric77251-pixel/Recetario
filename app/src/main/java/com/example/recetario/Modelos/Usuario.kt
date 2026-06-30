package com.example.recetario.Modelos

data class Usuario(
    var id: String = "",
    var nombre: String = "",
    var apellido: String = "",
    var correo: String = "",
    var fotoPerfil: String = "",
    var favoritos: List<String> = emptyList(),
    var recetasCreadas:List<String> = emptyList()
    )