package com.example.recetario.Funciones

import com.google.firebase.auth.FirebaseAuth

object Authentication {

    private val auth = FirebaseAuth.getInstance()

    fun crearUsuario(correo: String, contraseña: String, resultado: (Boolean, String?) -> Unit) {

        auth.createUserWithEmailAndPassword(correo, contraseña).addOnCompleteListener {
            if (it.isSuccessful) {
                resultado(true, null)
            } else {
                resultado(
                    false,
                    it.exception?.localizedMessage
                )
            }
        }
    }
    fun iniciarSesion(
        correo: String,
        contraseña: String,
        resultado: (Boolean, String?) -> Unit
    ) {

        auth.signInWithEmailAndPassword(
            correo,
            contraseña
        ).addOnCompleteListener {

            if (it.isSuccessful) {

                resultado(
                    true,
                    null
                )

            } else {

                resultado(
                    false,
                    it.exception?.localizedMessage
                )
            }
        }
    }
    fun cerrarSesion(){
        auth.signOut()
    }

}
