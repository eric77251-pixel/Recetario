package com.example.recetario.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recetario.utils.ValidationUtils
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.R
import com.example.recetario.utils.SystemBarUtils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    // Campos del formulario
    private lateinit var txtPasswordActual: EditText
    private lateinit var txtNuevaPassword: EditText
    private lateinit var txtConfirmarPassword: EditText

    // Botones
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_change_password)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootChangePassword))

        // Referencias a la interfaz
        txtPasswordActual = findViewById(R.id.txtPasswordActual)
        txtNuevaPassword = findViewById(R.id.txtNuevaPassword)
        txtConfirmarPassword = findViewById(R.id.txtConfirmarPassword)

        btnGuardar = findViewById(R.id.btnGuardar)
        btnCancelar = findViewById(R.id.btnCancelar)

        // Guardar cambios
        btnGuardar.setOnClickListener {
            cambiarContraseña()
        }

        // Cancelar
        btnCancelar.setOnClickListener {
            finish()
        }
    }

    /**
     * Valida el formulario y cambia la contraseña.
     */
    private fun cambiarContraseña() {

        val passwordActual =
            txtPasswordActual.text.toString()

        val nuevaPassword =
            txtNuevaPassword.text.toString()

        val confirmarPassword =
            txtConfirmarPassword.text.toString()

        // Validar contraseña actual
        if (ValidationUtils.campoVacio(passwordActual)) {

            txtPasswordActual.error = "Ingrese su contraseña actual"
            txtPasswordActual.requestFocus()
            return
        }

        // Validar nueva contraseña
        if (ValidationUtils.campoVacio(nuevaPassword)) {

            txtNuevaPassword.error = "Ingrese una nueva contraseña"
            txtNuevaPassword.requestFocus()
            return
        }

        // Validar seguridad de la nueva contraseña
        if (!ValidationUtils.contraseñaValida(nuevaPassword)) {

            txtNuevaPassword.error =
                ValidationUtils.mensajeReglaPassword()

            txtNuevaPassword.requestFocus()
            return
        }

        // Validar que sea diferente a la actual
        if (!ValidationUtils.contraseñasDiferentes(
                passwordActual,
                nuevaPassword
            )
        ) {

            txtNuevaPassword.error =
                "La nueva contraseña debe ser diferente a la actual"

            txtNuevaPassword.requestFocus()
            return
        }

        // Confirmar contraseña
        if (!ValidationUtils.contraseñasCoinciden(
                nuevaPassword,
                confirmarPassword
            )
        ) {

            txtConfirmarPassword.error =
                "Las contraseñas no coinciden"

            txtConfirmarPassword.requestFocus()
            return
        }

        if (!NetworkUtils.hayConexion(this)) {
            Toast.makeText(
                this,
                "Sin conexión. Intente nuevamente.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnGuardar.isEnabled = false

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null || usuario.email == null) {

            Toast.makeText(
                this,
                "No se encontró el usuario autenticado",
                Toast.LENGTH_SHORT
            ).show()

            btnGuardar.isEnabled = true
            return
        }

        val credential = EmailAuthProvider.getCredential(
            usuario.email!!,
            passwordActual
        )

        usuario.reauthenticate(credential)
            .addOnSuccessListener {

                usuario.updatePassword(nuevaPassword)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Contraseña actualizada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
                    .addOnFailureListener { e ->

                        btnGuardar.isEnabled = true

                        Toast.makeText(
                            this,
                            e.message ?: "No se pudo cambiar la contraseña",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {

                btnGuardar.isEnabled = true

                txtPasswordActual.error = "La contraseña actual es incorrecta"
                txtPasswordActual.requestFocus()
            }
    }
}