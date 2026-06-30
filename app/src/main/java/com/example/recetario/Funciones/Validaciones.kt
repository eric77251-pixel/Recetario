package com.example.recetario.Funciones

object Validaciones {

    /// Se asegura que la contraseña no este vacia ni con espacios
    fun campoVacio(texto: String): Boolean {
        return texto.trim().isEmpty()
    }

    ///El correo tenga formato correcto
    fun correoValido(correo: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS
            .matcher(correo)
            .matches()
    }

    fun contraseñaValida(contraseña: String): Boolean {

        val regex =
            Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&.:;,]).{8,}$")

        return regex.matches(contraseña)
    }

    ///Se asegura que en la creacion la primera contraseña sea igual a la segunda
    fun contraseñasCoinciden(
        password: String,
        confirmar: String
    ): Boolean {

        return password == confirmar
    }


    fun contraseñasDiferentes(
        contraseñaActual: String,
        nuevaContraseña: String
    ): Boolean {

        return contraseñaActual != nuevaContraseña
    }



}