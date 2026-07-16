package com.example.recetario.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.recetario.R
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.SystemBarUtils
import com.example.recetario.utils.ValidationUtils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.content.Intent
import com.example.recetario.utils.AuthManager

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var txtPasswordActual: EditText
    private lateinit var txtNuevaPassword: EditText
    private lateinit var txtConfirmarPassword: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    private data class PasswordChangeForm(
        val passwordActual: String,
        val nuevaPassword: String,
        val confirmarPassword: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthManager.obtenerUsuario() == null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_change_password)
        SystemBarUtils.aplicarInsets(findViewById(R.id.rootChangePassword))

        inicializarVistas()
        configurarEventos()
    }

    private fun inicializarVistas() {
        txtPasswordActual = findViewById(R.id.txtPasswordActual)
        txtNuevaPassword = findViewById(R.id.txtNuevaPassword)
        txtConfirmarPassword = findViewById(R.id.txtConfirmarPassword)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnCancelar = findViewById(R.id.btnCancelar)
    }

    private fun configurarEventos() {
        btnGuardar.setOnClickListener { cambiarContrasena() }
        btnCancelar.setOnClickListener { finish() }
    }

    private fun cambiarContrasena() {
        val form = leerFormulario()
        if (!validarFormulario(form)) return
        if (!validarConexion()) return

        val usuario = obtenerUsuarioAutenticado() ?: return
        btnGuardar.isEnabled = false
        reautenticarYActualizar(usuario, form)
    }

    private fun leerFormulario(): PasswordChangeForm {
        return PasswordChangeForm(
            passwordActual = txtPasswordActual.text.toString(),
            nuevaPassword = txtNuevaPassword.text.toString(),
            confirmarPassword = txtConfirmarPassword.text.toString()
        )
    }

    private fun validarFormulario(form: PasswordChangeForm): Boolean {
        if (ValidationUtils.campoVacio(form.passwordActual)) {
            marcarError(txtPasswordActual, "Ingrese su contraseña actual")
            return false
        }

        if (ValidationUtils.campoVacio(form.nuevaPassword)) {
            marcarError(txtNuevaPassword, "Ingrese una nueva contraseña")
            return false
        }

        if (!ValidationUtils.contraseñaValida(form.nuevaPassword)) {
            marcarError(txtNuevaPassword, ValidationUtils.mensajeReglaPassword())
            return false
        }

        if (!ValidationUtils.contraseñasDiferentes(form.passwordActual, form.nuevaPassword)) {
            marcarError(txtNuevaPassword, "La nueva contraseña debe ser diferente a la actual")
            return false
        }

        if (!ValidationUtils.contraseñasCoinciden(form.nuevaPassword, form.confirmarPassword)) {
            marcarError(txtConfirmarPassword, "Las contraseñas no coinciden")
            return false
        }

        return true
    }

    private fun validarConexion(): Boolean {
        if (NetworkUtils.hayConexion(this)) return true
        mostrarMensaje("Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG)
        return false
    }

    private fun obtenerUsuarioAutenticado(): FirebaseUser? {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario != null && usuario.email != null) return usuario

        mostrarMensaje("No se encontró el usuario autenticado")
        return null
    }

    private fun reautenticarYActualizar(usuario: FirebaseUser, form: PasswordChangeForm) {
        val credential = EmailAuthProvider.getCredential(usuario.email!!, form.passwordActual)

        usuario.reauthenticate(credential)
            .addOnSuccessListener { actualizarPassword(usuario, form.nuevaPassword) }
            .addOnFailureListener {
                btnGuardar.isEnabled = true
                marcarError(txtPasswordActual, "La contraseña actual es incorrecta")
            }
    }

    private fun actualizarPassword(usuario: FirebaseUser, nuevaPassword: String) {
        usuario.updatePassword(nuevaPassword)
            .addOnSuccessListener {
                mostrarMensaje("Contraseña actualizada correctamente")
                finish()
            }
            .addOnFailureListener { e ->
                btnGuardar.isEnabled = true
                mostrarMensaje(e.message ?: "No se pudo cambiar la contraseña", Toast.LENGTH_LONG)
            }
    }

    private fun marcarError(campo: EditText, mensaje: String) {
        campo.error = mensaje
        campo.requestFocus()
    }

    private fun mostrarMensaje(mensaje: String, duracion: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, mensaje, duracion).show()
    }
}