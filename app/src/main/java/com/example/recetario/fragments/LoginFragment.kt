package com.example.recetario.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.recetario.R
import com.example.recetario.activities.MainActivity
import com.example.recetario.data.UserManager
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.SessionManager
import com.example.recetario.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private lateinit var txtUsuario: EditText
    private lateinit var txtPasswordLogin: EditText
    private lateinit var btnAcceder: Button
    private lateinit var btnCrearUsuario: Button

    private data class LoginForm(
        val correo: String,
        val password: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        inicializarVistas(view)
        configurarEventos()

        return view
    }

    private fun inicializarVistas(view: View) {
        txtUsuario = view.findViewById(R.id.txtUsuario)
        txtPasswordLogin = view.findViewById(R.id.txtPasswordLogin)
        btnAcceder = view.findViewById(R.id.btnAcceder)
        btnCrearUsuario = view.findViewById(R.id.btnCrearUsuario)
    }

    private fun configurarEventos() {
        btnAcceder.setOnClickListener { iniciarSesion() }
        btnCrearUsuario.setOnClickListener { mostrarRegistro() }
    }

    private fun iniciarSesion() {
        val form = leerFormulario()
        if (!validarFormulario(form)) return
        if (!validarConexion()) return

        btnAcceder.isEnabled = false
        autenticarUsuario(form)
    }

    private fun leerFormulario(): LoginForm {
        return LoginForm(
            correo = txtUsuario.text.toString().trim(),
            password = txtPasswordLogin.text.toString()
        )
    }

    private fun validarFormulario(form: LoginForm): Boolean {
        if (ValidationUtils.campoVacio(form.correo)) {
            marcarError(txtUsuario, "Ingrese un correo")
            return false
        }

        if (!ValidationUtils.correoValido(form.correo)) {
            marcarError(txtUsuario, "Correo inválido")
            return false
        }

        if (ValidationUtils.campoVacio(form.password)) {
            marcarError(txtPasswordLogin, "Ingrese una contraseña")
            return false
        }

        return true
    }

    private fun validarConexion(): Boolean {
        if (NetworkUtils.hayConexion(requireContext())) return true
        mostrarMensaje("Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG)
        return false
    }

    private fun autenticarUsuario(form: LoginForm) {
        AuthManager.iniciarSesion(form.correo, form.password) { exito, _ ->
            if (!exito) {
                finalizarIntentoFallido("Correo o contraseña incorrectos")
                return@iniciarSesion
            }

            cargarPerfilDelUsuarioAutenticado()
        }
    }

    private fun cargarPerfilDelUsuarioAutenticado() {
        val firebaseUser = AuthManager.obtenerUsuario()
        if (firebaseUser == null) {
            finalizarIntentoFallido("No se pudo obtener el usuario autenticado.")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val usuario = UserManager.obtenerUsuario(firebaseUser.uid)
            btnAcceder.isEnabled = true

            if (usuario == null) {
                mostrarMensaje("No se encontró el usuario en la base de datos.", Toast.LENGTH_LONG)
                AuthManager.cerrarSesion()
                return@launch
            }

            SessionManager.usuario = usuario
            mostrarMensaje("Bienvenido ${usuario.nombre}")
            mostrarHome()
        }
    }

    private fun finalizarIntentoFallido(mensaje: String) {
        btnAcceder.isEnabled = true
        mostrarMensaje(mensaje, Toast.LENGTH_LONG)
    }

    private fun mostrarRegistro() {
        (requireActivity() as MainActivity).cambiarFragmento(
            RegisterFragment(),
            agregarAlBackStack = true,
            mostrarMenu = false
        )
    }

    private fun mostrarHome() {
        (requireActivity() as MainActivity).cambiarFragmento(
            HomeFragment(),
            agregarAlBackStack = false,
            mostrarMenu = true
        )
    }

    private fun marcarError(campo: EditText, mensaje: String) {
        campo.error = mensaje
        campo.requestFocus()
    }

    private fun mostrarMensaje(mensaje: String, duracion: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), mensaje, duracion).show()
    }
}