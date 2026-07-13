package com.example.recetario.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.recetario.R
import com.example.recetario.data.UserManager
import com.example.recetario.model.User
import com.example.recetario.utils.AuthManager
import com.example.recetario.utils.NetworkUtils
import com.example.recetario.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private lateinit var txtNombre: EditText
    private lateinit var txtApellido: EditText
    private lateinit var txtCorreo: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtConfirmarPassword: EditText
    private lateinit var btnCrearUsuario: Button

    private data class RegisterForm(
        val nombre: String,
        val apellido: String,
        val correo: String,
        val password: String,
        val confirmarPassword: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        inicializarVistas(view)
        configurarEventos()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarBotonAtras()
    }

    private fun inicializarVistas(view: View) {
        txtNombre = view.findViewById(R.id.txtNombre)
        txtApellido = view.findViewById(R.id.txtApellido)
        txtCorreo = view.findViewById(R.id.txtCorreo)
        txtPassword = view.findViewById(R.id.txtPassword)
        txtConfirmarPassword = view.findViewById(R.id.txtConfirmarPassword)
        btnCrearUsuario = view.findViewById(R.id.btnCrearUsuario)
    }

    private fun configurarEventos() {
        btnCrearUsuario.setOnClickListener { registrarUsuario() }
    }

    private fun configurarBotonAtras() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    private fun registrarUsuario() {
        val form = leerFormulario()
        if (!validarFormulario(form)) return
        if (!validarConexion()) return

        btnCrearUsuario.isEnabled = false
        crearCuentaFirebase(form)
    }

    private fun leerFormulario(): RegisterForm {
        return RegisterForm(
            nombre = txtNombre.text.toString().trim(),
            apellido = txtApellido.text.toString().trim(),
            correo = txtCorreo.text.toString().trim(),
            password = txtPassword.text.toString(),
            confirmarPassword = txtConfirmarPassword.text.toString()
        )
    }

    private fun validarFormulario(form: RegisterForm): Boolean {
        if (ValidationUtils.campoVacio(form.nombre)) {
            marcarError(txtNombre, "Ingrese un nombre")
            return false
        }

        if (ValidationUtils.campoVacio(form.apellido)) {
            marcarError(txtApellido, "Ingrese un apellido")
            return false
        }

        if (ValidationUtils.campoVacio(form.correo)) {
            marcarError(txtCorreo, "Ingrese un correo")
            return false
        }

        if (!ValidationUtils.correoValido(form.correo)) {
            marcarError(txtCorreo, "Correo inválido")
            return false
        }

        if (ValidationUtils.campoVacio(form.password)) {
            marcarError(txtPassword, "Ingrese una contraseña")
            return false
        }

        if (!ValidationUtils.contraseñaValida(form.password)) {
            marcarError(txtPassword, ValidationUtils.mensajeReglaPassword())
            return false
        }

        if (!ValidationUtils.contraseñasCoinciden(form.password, form.confirmarPassword)) {
            marcarError(txtConfirmarPassword, "No coinciden")
            return false
        }

        return true
    }

    private fun validarConexion(): Boolean {
        if (NetworkUtils.hayConexion(requireContext())) return true
        mostrarMensaje("Sin conexión. Intente nuevamente.", Toast.LENGTH_LONG)
        return false
    }

    private fun crearCuentaFirebase(form: RegisterForm) {
        AuthManager.crearUsuario(form.correo, form.password) { exito, mensaje ->
            if (!exito) {
                btnCrearUsuario.isEnabled = true
                mostrarDialogoError(mensaje ?: "No fue posible crear el usuario.")
                return@crearUsuario
            }

            guardarPerfilUsuario(form)
        }
    }

    private fun guardarPerfilUsuario(form: RegisterForm) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            btnCrearUsuario.isEnabled = true
            mostrarMensaje("Error: usuario Firebase nulo")
            return
        }

        val usuario = User(
            id = firebaseUser.uid,
            nombre = form.nombre,
            apellido = form.apellido,
            correo = firebaseUser.email ?: "",
            fotoPerfil = ""
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val guardado = UserManager.crearUsuario(usuario)
            btnCrearUsuario.isEnabled = true

            if (guardado) {
                mostrarMensaje("Usuario creado correctamente")
                parentFragmentManager.popBackStack()
            } else {
                mostrarDialogoError("No se pudo guardar el usuario en Firestore")
            }
        }
    }

    private fun marcarError(campo: EditText, mensaje: String) {
        campo.error = mensaje
        campo.requestFocus()
    }

    private fun mostrarMensaje(mensaje: String, duracion: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), mensaje, duracion).show()
    }

    private fun mostrarDialogoError(mensaje: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}