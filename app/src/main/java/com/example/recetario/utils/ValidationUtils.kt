package com.example.recetario.utils

object ValidationUtils {

    private const val MIN_PASSWORD_LENGTH = 12

    fun campoVacio(texto: String): Boolean {
        return texto.trim().isEmpty()
    }

    fun correoValido(correo: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS
            .matcher(correo)
            .matches()
    }

    /**
     * Contraseña con mínimo 12 caracteres, letras, números
     * y al menos un carácter especial.
     */
    fun contraseñaValida(contraseña: String): Boolean {
        val regex = Regex(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&.:;,\\-_]).{$MIN_PASSWORD_LENGTH,}$"
        )
        return regex.matches(contraseña)
    }

    fun mensajeReglaPassword(): String {
        return "La contraseña debe tener al menos 12 caracteres, una letra, un número y un símbolo."
    }

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