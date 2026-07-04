package com.example.recetario.Fragmentos

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
import com.example.recetario.Funciones.AuthManager
import com.example.recetario.Funciones.Validaciones
import com.example.recetario.Manager.UserManager
import com.example.recetario.Modelos.Usuario
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.recetario.R

class RegisterFragment : Fragment() {

    private lateinit var txtNombre: EditText
    private lateinit var txtApellido: EditText
    private lateinit var txtCorreo: EditText
    private lateinit var txtPassword: EditText
    private lateinit var txtConfirmarPassword: EditText
    private lateinit var btnCrearUsuario: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.creacion_usario, container, false)

        txtNombre = view.findViewById(R.id.txtNombre)
        txtApellido = view.findViewById(R.id.txtApellido)
        txtCorreo = view.findViewById(R.id.txtCorreo)
        txtPassword = view.findViewById(R.id.txtPassword)
        txtConfirmarPassword = view.findViewById(R.id.txtConfirmarPassword)
        btnCrearUsuario = view.findViewById(R.id.btnCrearUsuario)

        btnCrearUsuario.setOnClickListener {

            if (!validarFormulario()) return@setOnClickListener

            btnCrearUsuario.isEnabled = false

            AuthManager.crearUsuario(
                txtCorreo.text.toString().trim(),
                txtPassword.text.toString()
            ) { exito, mensaje ->

                if (!exito) {

                    btnCrearUsuario.isEnabled = true

                    AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage(mensaje ?: "No fue posible crear el usuario.")
                        .setPositiveButton("Aceptar", null)
                        .show()

                    return@crearUsuario
                }

                val firebaseUser = FirebaseAuth.getInstance().currentUser

                if (firebaseUser == null) {

                    btnCrearUsuario.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Error: usuario Firebase nulo",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@crearUsuario
                }

                val usuario = Usuario(
                    id = firebaseUser.uid,
                    nombre = txtNombre.text.toString().trim(),
                    apellido = txtApellido.text.toString().trim(),
                    correo = firebaseUser.email ?: "",
                    fotoPerfil = ""
                )

                viewLifecycleOwner.lifecycleScope.launch {

                    val guardado = UserManager.crearUsuario(usuario)

                    if (guardado) {

                        Toast.makeText(
                            requireContext(),
                            "Usuario creado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        // volver al login o pantalla anterior
                        parentFragmentManager.popBackStack()

                    } else {

                        AlertDialog.Builder(requireContext())
                            .setTitle("Error")
                            .setMessage("No se pudo guardar el usuario en Firestore")
                            .setPositiveButton("Aceptar", null)
                            .show()
                    }

                    btnCrearUsuario.isEnabled = true
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    parentFragmentManager.popBackStack()
                }
            }
        )
    }

    private fun validarFormulario(): Boolean {

        val nombre = txtNombre.text.toString().trim()
        val apellido = txtApellido.text.toString().trim()
        val correo = txtCorreo.text.toString().trim()
        val password = txtPassword.text.toString()
        val confirmar = txtConfirmarPassword.text.toString()

        if (Validaciones.campoVacio(nombre)) {
            txtNombre.error = "Ingrese un nombre"
            return false
        }

        if (Validaciones.campoVacio(apellido)) {
            txtApellido.error = "Ingrese un apellido"
            return false
        }

        if (Validaciones.campoVacio(correo)) {
            txtCorreo.error = "Ingrese un correo"
            return false
        }

        if (!Validaciones.correoValido(correo)) {
            txtCorreo.error = "Correo inválido"
            return false
        }

        if (Validaciones.campoVacio(password)) {
            txtPassword.error = "Ingrese una contraseña"
            return false
        }

        if (!Validaciones.contraseñaValida(password)) {
            txtPassword.error = "Contraseña débil"
            return false
        }

        if (!Validaciones.contraseñasCoinciden(password, confirmar)) {
            txtConfirmarPassword.error = "No coinciden"
            return false
        }

        return true
    }
}